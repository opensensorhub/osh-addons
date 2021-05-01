/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.tags;

import com.google.gson.JsonObject;

/**
 * Value format enumeration.  This enumeration and its values comes from <bold>MISB ST 0601.16: UAS Datalink Local Set</bold>
 * These enumerated values are used in conjunction with {@link Tag} to identify a KLV element
 * using shorthand tag values, or the keys (K) within the data stream.  The <code>{@link Encoding}</code>
 * specifies data encoding and length information.
 * <p>
 * Each value is composed of:
 * <code>name</code>: the name/type of the format
 * </code>len</code>: the number of bytes, or encoding principle used for the values
 *
 * @author Nick Garay
 * @since Jan. 30, 2020
 */
public enum Encoding implements JsonPrinter {

    NONE("None"),
    BYTE("byte", 1),
    UINT("uint"),
    UINT8("uint8", 1),
    UINT16("uint16", 2),
    UINT32("uint32", 4),
    UINT64("uint64", 8),
    INT("int"),
    INT8("int8", 1),
    INT16("int16", 2),
    INT32("int32", 4),
    INT64("int64", 8),
    UTF8("utf8"),
    ISO_IEC_646_ASCII_TEXT("ISO/IEC 646"),
    ISO_IEC_646_YYYYMMDD("ISO/IEC 646 Date", 8),
    ISO_IEC_646_YYYY_MM_DD("ISO/IEC 646 Date", 10),
    RFC_2781("RFC 2781"),
    SET("set"),
    DLP("defined length pack"),
    VLP("variable length pack"),
    IMAPB("IMAPB"),
    FLP("FLP"),
    SMPTE_330M("SMPTE 330M", 32),
    ISO_IEC_13818_1_INT8("ISO/IEC 13818-1", 1),
    ISO_IEC_13818_1_INT16("ISO/IEC 13818-1", 2),
    SMPTE_336M("SMPTE_336M", 16);

    private int len;
    private String name;

    /**
     * Constructor
     *
     * @param name the name of the format specification
     */
    Encoding(String name) {

        this(name, 0);
    }

    /**
     * Constructor
     *
     * @param name the name of the format specification
     * @param len  the number of bytes for the data representation given by the format
     */
    Encoding(String name, int len) {

        this.name = name;
        this.len = len;
    }

    /**
     * Returns the length in bytes this format specifier uses for encoding/decoding data.
     * If a value of <code>0</code> is returned then the encoding format is a variable length
     * format and KLV short or long length decoding should be performed as per <bold>MISB: TRM 1006</bold>
     */
    public int getLen() {

        return len;
    }

    /**
     * Creates a string representation of the enumeration.
     *
     * @return a string representation of the the enumerated value.
     */
    public String toString() {

        return this.name;
    }

    @Override
    public String toJsonString() {

        return getAsJsonObject().toString();
    }

    @Override
    public JsonObject getAsJsonObject() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("name", name);

        if (len > 0) {

            jsonObject.addProperty("len", len);

        } else {

            jsonObject.addProperty("len", "variable");
        }

        return jsonObject;
    }
}
