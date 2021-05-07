/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.klv;

import org.sensorhub.misb.stanag4609.klv.AbstractDataSet;
import org.sensorhub.misb.stanag4609.klv.Element;
import org.sensorhub.misb.stanag4609.tags.Encoding;
import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagRegistry;
import org.sensorhub.misb.stanag4609.tags.TagSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Encompasses a UAS Data Link Security Local Set allowing for extraction of elements from the set
 * for further decoding.
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class SecurityLocalSet extends AbstractDataSet {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLocalSet.class);

    public static final TagSet SECURITY_LOCAL_SET = new TagSet("06 0E 2B 34 02 03 01 01 0E 01 03 03 02 00 00 00", "UAS Local Set Security", "Local Label");
    public static final TagSet SECURITY_LOCAL_SET_UNIVERSAL = new TagSet("06 0E 2B 34 02 01 01 01 02 08 02 00 00 00 00 00", "UAS Local Set Security", "Universal Label");

    static {

        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 02 08 02 01 00 00 00 00", 0x01, Encoding.UINT8, "Security Classification", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 20 01 02 07 00 00", 0x02, Encoding.UINT8, "Classifying Country and Releasing Instructions Country Coding Method", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 20 01 02 08 00 00", 0x03, Encoding.ISO_IEC_646_ASCII_TEXT, "Classifying Country", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 03 02 00 00 00", 0x04, Encoding.ISO_IEC_646_ASCII_TEXT, "Security-SCI/SHI Information", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 02 08 02 02 00 00 00 00", 0x05, Encoding.ISO_IEC_646_ASCII_TEXT, "Caveats", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 20 01 02 09 00 00", 0x06, Encoding.ISO_IEC_646_ASCII_TEXT, "Releasing Instructions", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 02 08 02 03 00 00 00 00", 0x07, Encoding.ISO_IEC_646_ASCII_TEXT, "Classified By", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 02 08 02 06 00 00 00 00", 0x08, Encoding.ISO_IEC_646_ASCII_TEXT, "Derived From", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 02 08 02 04 00 00 00 00", 0x09, Encoding.ISO_IEC_646_ASCII_TEXT, "Classification Reason", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 02 08 02 05 00 00 00 00", 0x0A, Encoding.ISO_IEC_646_YYYYMMDD, "Declassification Date", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 02 08 02 08 00 00 00 00", 0x0B, Encoding.ISO_IEC_646_ASCII_TEXT, "Classification and Marking System", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 20 01 02 06 00 00", 0x0C, Encoding.UINT8, "Object Country Coding Method", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 07 01 20 01 02 01 01 00", 0x0D, Encoding.RFC_2781, "Object Country Codes", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 02 08 02 07 00 00 00 00", 0x0E, Encoding.ISO_IEC_646_ASCII_TEXT, "Classification Comments", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0A 2B 34 01 01 01 01 01 01 01 ** 00 00 00 00", 0x0F, Encoding.SMPTE_330M, "Unique Material Identifier Video", "MISB ST102.5"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0A 2B 34 01 01 01 01 01 01 02 ** 00 00 00 00", 0x10, Encoding.SMPTE_330M, "Unique Material Identifier Audio", "MISB ST102.5"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0A 2B 34 01 01 01 01 01 01 03 ** 00 00 00 00", 0x11, Encoding.SMPTE_330M, "Unique Material Identifier Data", "MISB ST102.5"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0A 2B 34 01 01 01 01 01 01 04 ** 00 00 00 00", 0x12, Encoding.SMPTE_330M, "Unique Material Identifier System", "MISB ST102.5"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 01 03 04 02 00 00 00 00", 0x13, Encoding.ISO_IEC_13818_1_INT8, "Stream Id", "MISB ST102.5"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 03 01 03 04 03 00 00 00 00 ", 0x14, Encoding.ISO_IEC_13818_1_INT16, "Transport Stream Id", "MISB ST102.5"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 05 04 00 00 00 ", 0x15, Encoding.SMPTE_336M, "Item Designator Id (16 byte)", "MISB ST102.5"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 02 05 04 00 00 00", 0x16, Encoding.UINT16, "Version", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 04 03 03 00 00 00", 0x17, Encoding.ISO_IEC_646_YYYY_MM_DD, "Classifying Country and Releasing Instructions Country Coding Method Version Date", "MISB ST102.12"));
        TagRegistry.getInstance().registerTag(new Tag(SECURITY_LOCAL_SET, "06 0E 2B 34 01 01 01 01 0E 01 04 03 04 00 00 00", 0x18, Encoding.ISO_IEC_646_YYYY_MM_DD, "Object Country Coding Method Version Date", "MISB ST102.12"));
    }

    /**
     * Constructor
     *
     * @param length  the length in bytes of the set
     * @param payload the content of the set as raw bytes
     */
    public SecurityLocalSet(int length, byte[] payload) {

        this.tagSet = SECURITY_LOCAL_SET;

        this.length = length;
        this.payload = payload;

        this.designator = SECURITY_LOCAL_SET.getDesignator();
        this.embeddedDataLength = 0;
    }

    @Override
    public HashMap<Tag, Object> decode() {

        HashMap<Tag, Object> valuesMap = new HashMap<>();

        while (hasMoreElements()) {

            // Read data from next element in local data set which can be treated as a set
            Element securityElement = getNextElement();

            Tag tag = securityElement.getTag();

            if (TagSet.UNKNOWN != tag.getMemberOf()) {

                // Decode element data values as raw data, these values get converted
                // to correct type based on tag specific formulas within the setData method below
                Object value = securityElement.unpackData();

                switch (tag.getLocalSetTag()) {

                    case 0x01: // Security Classification
                        String classification = null;

                        switch ((int) value) {
                            case 0x05: // "TOP SECRET//"
                                classification = "TOP SECRET";

                                break;
                            case 0x04: // "SECRET//"
                                classification = "SECRET";

                                break;
                            case 0x03: // "CONFIDENTIAL//"
                                classification = "CONFIDENTIAL";

                                break;
                            case 0x02: // "RESTRICTED//"
                                classification = "RESTRICTED";

                                break;
                            case 0x01: // "UNCLASSIFIED//"
                                classification = "UNCLASSIFIED";

                                break;
                            default:
                                logger.error("Invalid value for tag: {} value: {}",
                                        securityElement.getTag().getLocalSetTag(), value);
                                break;
                        }
                        valuesMap.put(tag, classification);
                        break;

                    case 0x02: // Classifying Country and Releasing Instructions Country Coding Method
                    case 0x16: // Version
                        valuesMap.put(tag, String.valueOf((int) value));
                        break;

                    case 0x03: // Classifying Country
                    case 0x04: // Security-SCI/SHI Information
                    case 0x05: // Caveats
                    case 0x06: // Releasing Instructions
                    case 0x07: // Classified By
                    case 0x08: // Derived From
                    case 0x09: // Classification Reason
                    case 0x0A: // Declassification Date
                    case 0x0B: // Classification and Marking System
                    case 0x0C: // Object Country Coding Method
                    case 0x0D: // Object Country Codes
                    case 0x0E: // Classification Comments
                    case 0x17: // Classifying Country and Releasing Instructions Country Coding Method Version Date
                    case 0x18: // Object Country Coding Method Version Date
                    case 0x13: // Stream Id
                    case 0x14: // Transport Stream Id
                        valuesMap.put(tag, value);
                        break;

                    case 0x0F: // Unique Material Identifier Video
                    case 0x10: // Unique Material Identifier Audio
                    case 0x11: // Unique Material Identifier Data
                    case 0x12: // Unique Material Identifier System
                    case 0x15: // Item Designator Id (16 byte)
                        logger.info("Unsupported tag: {}", securityElement.getTag().getLocalSetTag());
                        break;

                    default:
                        logger.error("Invalid tag: {}", securityElement.getTag().getLocalSetTag());
                        break;
                }

            } else {

                logger.info("Unknown UAS Data Link Security Local Set tag: \n \t{}",
                        securityElement.toJsonString());
            }
        }

        return valuesMap;
    }
}
