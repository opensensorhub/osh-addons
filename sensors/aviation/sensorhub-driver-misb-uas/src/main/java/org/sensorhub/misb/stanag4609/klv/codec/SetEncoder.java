/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.klv.codec;

import org.sensorhub.misb.stanag4609.klv.Element;
import org.sensorhub.misb.stanag4609.klv.exceptions.ElementEncodingException;
import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagRegistry;
import org.sensorhub.misb.stanag4609.tags.TagSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic encoder for MISB Local Sets.
 *
 * <p>This class provides the core functionality used by all MISB encoders
 * (ST 0601, ST 0102, ST 0903, etc.). It assembles a Local Set by mapping
 * tag identifiers to {@link Tag} definitions, encoding values using
 * {@link Element}, and writing the final KLV structure with correct BER
 * lengths and tag ordering.</p>
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *     <li>Resolve tag IDs using {@link TagRegistry}</li>
 *     <li>Encode values according to the tag’s declared type</li>
 *     <li>Preserve insertion order (LinkedHashMap) for deterministic output</li>
 *     <li>Write BER lengths for each element and for the Local Set itself</li>
 *     <li>Optionally include the Local Set UL and BER length wrapper</li>
 * </ul>
 *
 * <p>All MISB Local Set encoders (UAS LS, Security LS, VMTI LS, VTarget Pack)
 * delegate to this class for the actual KLV assembly.</p>
 */
public class SetEncoder {

    /** The TagSet (UL + tag definitions) associated with this Local Set. */
    private final TagSet tagSet;

    /**
     * Ordered map of encoded elements.
     *
     * <p>LinkedHashMap preserves insertion order, which is important for
     * deterministic output and for matching reference encodings.</p>
     */
    private final Map<Tag, byte[]> elements = new LinkedHashMap<>();

    /**
     * Creates a new encoder for the given Local Set.
     *
     * @param tagSet the TagSet defining the UL and tag definitions
     */
    public SetEncoder(TagSet tagSet) {
        this.tagSet = tagSet;
    }

    /**
     * Adds or replaces a tag/value pair in the Local Set.
     *
     * <p>The tag ID is resolved using {@link TagRegistry}. If the tag is not
     * defined for this Local Set, an {@link ElementEncodingException} is thrown.</p>
     *
     * @param tagId numeric tag identifier
     * @param value raw value (String, Number, byte[], etc.)
     */
    public void put(byte tagId, Object value) {

        Tag tag = TagRegistry.getInstance().getByTagSetAndId(tagSet, tagId);

        if (tag == null) {
            throw new ElementEncodingException("Unknown tag " + tagId);
        }

        elements.put(tag, encodeValue(tag, value));
    }

    /**
     * Encodes the Local Set into a KLV byte array.
     *
     * <p>The output format is:</p>
     * <pre>
     *   [UL] [BER length] [tag][len][value]...
     * </pre>
     *
     * <p>If {@code withKeyAndLength} is false, only the Local Set value is
     * returned (useful for nested Local Sets such as Security LS or VTarget Packs).</p>
     *
     * @param withKeyAndLength whether to include the UL and BER length wrapper
     * @return encoded KLV bytes
     */
    public byte[] encode(boolean withKeyAndLength) throws IOException {

        ByteArrayOutputStream valueStream = new ByteArrayOutputStream();

        // Encode each tag/value pair
        for (var e : elements.entrySet()) {
            Tag tag = e.getKey();
            byte[] raw = e.getValue();

            valueStream.write(tag.getLocalSetTag());
            writeBerLength(valueStream, raw.length);
            valueStream.write(raw);
        }

        byte[] valueBytes = valueStream.toByteArray();

        ByteArrayOutputStream klv = new ByteArrayOutputStream();

        if (withKeyAndLength) {
            writeTagSet(klv, tagSet);
            writeBerLength(klv, valueBytes.length);
        }

        klv.write(valueBytes);
        return klv.toByteArray();
    }

    /**
     * Writes the Local Set UL (Universal Label) to the output stream.
     *
     * <p>The UL is stored in the TagSet as a space‑separated hex string.
     * Example: "06 0E 2B 34 ..."</p>
     */
    private static void writeTagSet(ByteArrayOutputStream out, TagSet tagSet) {

        String[] tokens = tagSet.getDesignator().split(" ");
        for (String token : tokens) {
            out.write(HexFormat.fromHexDigits(token));
        }
    }

    /**
     * Writes a BER length field.
     *
     * <p>For lengths &lt; 128, a single byte is used. For larger values,
     * the encoder emits 0x81 followed by the length byte.</p>
     */
    private static void writeBerLength(ByteArrayOutputStream out, int length) {

        if (length < 0x80) {
            out.write(length);
        } else {
            out.write(0x81);
            out.write(length);
        }
    }

    /**
     * Encodes a value according to the tag’s declared type.
     *
     * <p>This delegates to {@link Element}, which performs type‑correct packing
     * (e.g., UINT16, INT32, IMAPB, strings, byte arrays).</p>
     */
    private static byte[] encodeValue(Tag tagId, Object value) {

        Element element = new Element(tagId, value);
        return element.packData();
    }
}
