/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.klv.codec.misb0102;

import org.sensorhub.misb.stanag4609.klv.codec.SetEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SecurityLocalSetEnc {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLocalSetEnc.class);

    private final SetEncoder setEncoder =
            new SetEncoder(SecurityLocalSet.SECURITY_LOCAL_SET); // 0x30

    /**
     * Tag 0x01 – Security Classification (UINT8)
     */
    public SecurityLocalSetEnc classification(int code) {
        setEncoder.put((byte) 0x01, (short) code);
        return this;
    }

    /**
     * Tag 0x02 – Country Coding Method (UINT8)
     */
    public SecurityLocalSetEnc codingMethod(int code) {
        setEncoder.put((byte) 0x02, (short) code);
        return this;
    }

    /**
     * Tag 0x03 – Classifying Country
     */
    public SecurityLocalSetEnc classifyingCountry(String country) {
        setEncoder.put((byte) 0x03, country);
        return this;
    }

    /**
     * Tag 0x04 – SCI/SHI Information
     */
    public SecurityLocalSetEnc sciShiInfo(String info) {
        setEncoder.put((byte) 0x04, info);
        return this;
    }

    /**
     * Tag 0x05 – Caveats
     */
    public SecurityLocalSetEnc caveats(String caveats) {
        setEncoder.put((byte) 0x05, caveats);
        return this;
    }

    /**
     * Tag 0x06 – Releasing Instructions
     */
    public SecurityLocalSetEnc releasingInstructions(String instr) {
        setEncoder.put((byte) 0x06, instr);
        return this;
    }

    /**
     * Tag 0x07 – Classified By
     */
    public SecurityLocalSetEnc classifiedBy(String by) {
        setEncoder.put((byte) 0x07, by);
        return this;
    }

    /**
     * Tag 0x08 – Derived From
     */
    public SecurityLocalSetEnc derivedFrom(String src) {
        setEncoder.put((byte) 0x08, src);
        return this;
    }

    /**
     * Tag 0x09 – Classification Reason
     */
    public SecurityLocalSetEnc classificationReason(String reason) {
        setEncoder.put((byte) 0x09, reason);
        return this;
    }

    /**
     * Tag 0x0A – Declassification Date (YYYYMMDD)
     */
    public SecurityLocalSetEnc declassificationDate(String yyyymmdd) {
        setEncoder.put((byte) 0x0A, yyyymmdd);
        return this;
    }

    /**
     * Tag 0x0B – Classification System
     */
    public SecurityLocalSetEnc classificationSystem(String system) {
        setEncoder.put((byte) 0x0B, system);
        return this;
    }

    /**
     * Tag 0x0D – Object Country Codes
     */
    public SecurityLocalSetEnc objectCountryCodes(String codes) {
        setEncoder.put((byte) 0x0D, codes);
        return this;
    }

    /**
     * Tag 0x0E – Classification Comments
     */
    public SecurityLocalSetEnc classificationComments(String comments) {
        setEncoder.put((byte) 0x0E, comments);
        return this;
    }

    /**
     * Generic setter for any ST0102 tag
     */
    public SecurityLocalSetEnc put(byte tagId, Object value) {

        switch (tagId) {

            case 0x01: // Security Classification
                int classification = 0;

                switch ((String) value) {
                    case "TOP SECRET":
                        classification = 0x05;
                        break;
                    case "SECRET":
                        classification = 0x04;
                        break;
                    case "CONFIDENTIAL":
                        classification = 0x03;
                        break;
                    case "RESTRICTED":
                        classification = 0x02;
                        break;
                    case "UNCLASSIFIED":
                        classification = 0x01;
                        break;
                    default:
                        logger.error("Invalid value for tag: {} value: {}", tagId, value);
                        break;
                }
                setEncoder.put(tagId, classification);
                break;

            case 0x02: // Classifying Country and Releasing Instructions Country Coding Method
            case 0x0C: // Object Country Coding Method
            case 0x16: // Version
                setEncoder.put(tagId, Integer.valueOf((String) value));
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
            case 0x0D: // Object Country Codes
            case 0x0E: // Classification Comments
            case 0x17: // Classifying Country and Releasing Instructions Country Coding Method Version Date
            case 0x18: // Object Country Coding Method Version Date
            case 0x13: // Stream Id
            case 0x14: // Transport Stream Id
                setEncoder.put(tagId, value);
                break;

            case 0x0F: // Unique Material Identifier Video
            case 0x10: // Unique Material Identifier Audio
            case 0x11: // Unique Material Identifier Data
            case 0x12: // Unique Material Identifier System
            case 0x15: // Item Designator Id (16 byte)
                logger.info("Unsupported tag: {}", tagId);
                break;

            default:
                logger.error("Invalid tag: {}", tagId);
                break;
        }

        return this;
    }

    public byte[] encode(boolean withKeyAndLength) throws IOException {
        return setEncoder.encode(withKeyAndLength);
    }
}
