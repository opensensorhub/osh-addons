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

/**
 * Encoder for the MISB ST 0102 Security Local Set.
 *
 * <p>This class provides a fluent API for constructing a complete
 * Security Local Set (tag 0x30) by populating individual ST 0102 fields.
 * Internally it delegates to {@link SetEncoder}, which handles KLV BER
 * lengths, tag/value encoding, and final Local Set assembly.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>
 * byte[] klv = new SecurityLocalSetEnc()
 *         .classification(0x04)               // SECRET
 *         .classifyingCountry("USA")
 *         .caveats("NOFORN")
 *         .declassificationDate("20450101")
 *         .encode(true);
 * </pre>
 *
 * <p>The encoder supports both explicit setters for each ST 0102 tag and a
 * generic {@link #put(byte, Object)} method for dynamic tag assignment.</p>
 */
public class SecurityLocalSetEnc {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLocalSetEnc.class);

    /** Internal encoder for the Security Local Set (tag 0x30). */
    private final SetEncoder setEncoder =
            new SetEncoder(SecurityLocalSet.SECURITY_LOCAL_SET);

    // -------------------------------------------------------------------------
    // Explicit Tag Setters (Strongly Typed)
    // -------------------------------------------------------------------------

    /** Tag 0x01 – Security Classification (UINT8). */
    public SecurityLocalSetEnc classification(int code) {
        setEncoder.put((byte) 0x01, (short) code);
        return this;
    }

    /** Tag 0x02 – Country Coding Method (UINT8). */
    public SecurityLocalSetEnc codingMethod(int code) {
        setEncoder.put((byte) 0x02, (short) code);
        return this;
    }

    /** Tag 0x03 – Classifying Country (string). */
    public SecurityLocalSetEnc classifyingCountry(String country) {
        setEncoder.put((byte) 0x03, country);
        return this;
    }

    /** Tag 0x04 – SCI/SHI Information (string). */
    public SecurityLocalSetEnc sciShiInfo(String info) {
        setEncoder.put((byte) 0x04, info);
        return this;
    }

    /** Tag 0x05 – Caveats (string). */
    public SecurityLocalSetEnc caveats(String caveats) {
        setEncoder.put((byte) 0x05, caveats);
        return this;
    }

    /** Tag 0x06 – Releasing Instructions (string). */
    public SecurityLocalSetEnc releasingInstructions(String instr) {
        setEncoder.put((byte) 0x06, instr);
        return this;
    }

    /** Tag 0x07 – Classified By (string). */
    public SecurityLocalSetEnc classifiedBy(String by) {
        setEncoder.put((byte) 0x07, by);
        return this;
    }

    /** Tag 0x08 – Derived From (string). */
    public SecurityLocalSetEnc derivedFrom(String src) {
        setEncoder.put((byte) 0x08, src);
        return this;
    }

    /** Tag 0x09 – Classification Reason (string). */
    public SecurityLocalSetEnc classificationReason(String reason) {
        setEncoder.put((byte) 0x09, reason);
        return this;
    }

    /** Tag 0x0A – Declassification Date (YYYYMMDD). */
    public SecurityLocalSetEnc declassificationDate(String yyyymmdd) {
        setEncoder.put((byte) 0x0A, yyyymmdd);
        return this;
    }

    /** Tag 0x0B – Classification System (string). */
    public SecurityLocalSetEnc classificationSystem(String system) {
        setEncoder.put((byte) 0x0B, system);
        return this;
    }

    /** Tag 0x0D – Object Country Codes (string). */
    public SecurityLocalSetEnc objectCountryCodes(String codes) {
        setEncoder.put((byte) 0x0D, codes);
        return this;
    }

    /** Tag 0x0E – Classification Comments (string). */
    public SecurityLocalSetEnc classificationComments(String comments) {
        setEncoder.put((byte) 0x0E, comments);
        return this;
    }

    // -------------------------------------------------------------------------
    // Generic Tag Setter (Dynamic)
    // -------------------------------------------------------------------------

    /**
     * Generic setter for any ST 0102 tag.
     *
     * <p>This method allows dynamic assignment of tag values when the caller
     * does not want to use the explicit typed setters. It performs type
     * conversion, validation, and logging for unsupported or invalid tags.</p>
     *
     * @param tagId the ST 0102 tag identifier
     * @param value the value to encode (String or numeric depending on tag)
     */
    public SecurityLocalSetEnc put(byte tagId, Object value) {

        switch (tagId) {

            // -----------------------------------------------------------------
            // Tag 0x01 – Security Classification (string → UINT8)
            // -----------------------------------------------------------------
            case 0x01:
                int classification = 0;

                switch ((String) value) {
                    case "TOP SECRET":     classification = 0x05; break;
                    case "SECRET":         classification = 0x04; break;
                    case "CONFIDENTIAL":   classification = 0x03; break;
                    case "RESTRICTED":     classification = 0x02; break;
                    case "UNCLASSIFIED":   classification = 0x01; break;
                    default:
                        logger.error("Invalid value for tag: {} value: {}", tagId, value);
                        break;
                }
                setEncoder.put(tagId, classification);
                break;

            // -----------------------------------------------------------------
            // Numeric tags (string → integer)
            // -----------------------------------------------------------------
            case 0x02: // Country Coding Method
            case 0x0C: // Object Country Coding Method
            case 0x16: // Version
                setEncoder.put(tagId, Integer.valueOf((String) value));
                break;

            // -----------------------------------------------------------------
            // String tags (direct pass-through)
            // -----------------------------------------------------------------
            case 0x03: // Classifying Country
            case 0x04: // SCI/SHI Information
            case 0x05: // Caveats
            case 0x06: // Releasing Instructions
            case 0x07: // Classified By
            case 0x08: // Derived From
            case 0x09: // Classification Reason
            case 0x0A: // Declassification Date
            case 0x0B: // Classification System
            case 0x0D: // Object Country Codes
            case 0x0E: // Classification Comments
            case 0x17: // Coding Method Version Date
            case 0x18: // Object Coding Method Version Date
            case 0x13: // Stream ID
            case 0x14: // Transport Stream ID
                setEncoder.put(tagId, value);
                break;

            // -----------------------------------------------------------------
            // Unsupported binary tags (UMIDs, Item Designator ID)
            // -----------------------------------------------------------------
            case 0x0F:
            case 0x10:
            case 0x11:
            case 0x12:
            case 0x15:
                logger.info("Unsupported tag: {}", tagId);
                break;

            // -----------------------------------------------------------------
            // Unknown tag
            // -----------------------------------------------------------------
            default:
                logger.error("Invalid tag: {}", tagId);
                break;
        }

        return this;
    }

    /**
     * Encodes the Security Local Set into a KLV byte array.
     *
     * @param withKeyAndLength whether to include the UL and BER length fields
     * @return encoded KLV bytes
     * @throws IOException if encoding fails
     */
    public byte[] encode(boolean withKeyAndLength) throws IOException {
        return setEncoder.encode(withKeyAndLength);
    }
}
