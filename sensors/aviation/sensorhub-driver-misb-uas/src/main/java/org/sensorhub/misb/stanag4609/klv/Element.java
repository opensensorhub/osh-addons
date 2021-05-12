/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.klv;

import org.sensorhub.misb.stanag4609.tags.JsonPrinter;
import org.sensorhub.misb.stanag4609.tags.Encoding;
import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagRegistry;
import org.sensorhub.misb.stanag4609.tags.TagSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Base class describing MISB KLV data elements.
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class Element implements JsonPrinter {

    private static final Logger logger = LoggerFactory.getLogger(Element.class);

    private static final TagRegistry TAG_REGISTRY = TagRegistry.getInstance();

    private static final int DWORD = 8;
    private static final int WORD = 4;
    private static final int SHORT = 2;
    private static final int BYTE = 1;

    private final byte[] value;
    private final Tag tag;

    /**
     * Constructor.
     *
     * @param tagSet Identifies the family of tags the given tagId belongs to
     * @param tagId  a numeric id for the associated tag within the given tagSet
     * @param value  the data encoded by this element, raw binary data as <code>byte[]</code>
     */
    public Element(TagSet tagSet, byte tagId, byte[] value) {

        this.tag = TAG_REGISTRY.getByTagSetAndId(tagSet, tagId);
        this.value = value;
    }

    /**
     * Constructor
     *
     * @param tag   the associated tag
     * @param value the data encoded by this element, raw binary data as <code>byte[]</code>
     */
    public Element(Tag tag, byte[] value) {

        this.tag = tag;
        this.value = value;
    }

    /**
     * Gets the elements encoding mechanism, as given by the associated element tag
     *
     * @return the encoding value indicating how data has been encoded in the element.
     */
    public Encoding getDataEncoding() {

        return tag.getEncoding();
    }

    @Override
    public String toString() {

        return tag.toString();
    }

    @Override
    public String toJsonString() {

        return getAsJsonObject().toString();
    }

    @Override
    public JsonObject getAsJsonObject() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.add("tagData", tag.getAsJsonObject());

        JsonArray jsonArray = new JsonArray();

        for (byte dataByte : value) {

            jsonArray.add(Integer.toHexString(dataByte & 0x00FF));
        }

        jsonObject.add("value", jsonArray);

        return jsonObject;
    }

    /**
     * @return the tag associated with this element.
     */
    public Tag getTag() {

        return tag;
    }

    /**
     * Returns a copy of the raw (encoded) data values
     *
     * @return array of bytes representing encoded data
     */
    public byte[] getBytes() {

        return Arrays.copyOf(value, value.length);
    }

    /**
     * Decodes the integral stored in the value of the element given the tag and its encoded length
     *
     * @return The integral representing the decoded value.
     * @throws ElementDecodingException if the length expected by the given associated tag does not match
     *                                  the length of the element data field {@link Element#value}.
     */
    public Object unpackData() throws ElementDecodingException {

        Object result = null;

        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(value));

        Encoding encoding = tag.getEncoding();

        try {

            switch (encoding) {
                case BYTE:
                case INT8:
                case ISO_IEC_13818_1_INT8:
                    if (encoding.getLen() == value.length) {
                        result = dataInputStream.readByte();
                    }
                    break;
                case UINT: // Variable length
                    long computedValue = 0;
                    for (int idx = 0; idx < value.length; ++idx) {
                        computedValue = computedValue << 8;
                        computedValue += value[idx] & 0x0FF;
                    }
                    result = computedValue;
                    break;
                case INT: // Variable length
                    if (DWORD == value.length) {
                        result = dataInputStream.readLong();
                    } else if (WORD == value.length) {
                        result = (long) dataInputStream.readInt();
                    } else if (SHORT == value.length) {
                        result = (long) dataInputStream.readShort();
                    } else if (BYTE == value.length) {
                        result = (long) dataInputStream.readByte();
                    } else {
                        logger.error("Error reading value for tag: {} encoding: {} value length: {}", tag.getName(), encoding.toString(), value.length);
                    }
                    break;
                case UINT8:
                    if (encoding.getLen() == value.length) {
                        result = dataInputStream.readUnsignedByte();
                    }
                    break;
                case UINT16:
                    if (encoding.getLen() == value.length) {
                        result = dataInputStream.readUnsignedShort();
                    }
                    break;
                case INT16:
                case ISO_IEC_13818_1_INT16:
                    if (encoding.getLen() == value.length) {
                        result = dataInputStream.readShort();
                    }
                    break;
                case UINT32:
                case INT32:
                    if (encoding.getLen() == value.length) {
                        result = dataInputStream.readInt();
                    }
                    break;
                case UINT64:
                case INT64:
                    if (encoding.getLen() == value.length) {
                        result = dataInputStream.readLong();
                    }
                    break;
                case UTF8:
                case ISO_IEC_646_ASCII_TEXT:
                case ISO_IEC_646_YYYY_MM_DD:
                case ISO_IEC_646_YYYYMMDD:
                case RFC_2781:
                    result = new String(value, StandardCharsets.UTF_8);
                    break;
                case SET:
                case SERIES:
                case SMPTE_336M:
                case BINARY:
                    result = getBytes();
                    break;
                case IMAPA:
                case IMAPB:
                    result = getBytes();
                    break;
                case DLP:
                case VLP:
                case FLP:
                case SMPTE_330M:
                    logger.error("Unsupported encoding encountered for tag: {} encoding: {}", tag.getName(), encoding.toString());
                    break;
                default:
                    logger.error("Unknown encoding encountered for tag: {} encoding: {}", tag.getName(), encoding.toString());
                    break;
            }

        } catch (IOException e) {

            logger.error("Error reading value for tag: {} encoding: {} value length: {}", tag.getName(), encoding.toString(), value.length);
        }

        if (null == result) {

            StringBuilder message = new StringBuilder();
            message.append("Value length [");
            message.append(value.length);
            message.append("] differs from encoding length [");
            message.append(encoding.getLen());
            message.append("] for tag: ");
            message.append(tag.getLocalSetTag());
            logger.error(message.toString());

            throw new ElementDecodingException(message.toString());
        }

        return result;
    }
}
