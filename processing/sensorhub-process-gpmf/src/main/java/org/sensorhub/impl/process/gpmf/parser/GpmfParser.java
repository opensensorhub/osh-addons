/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.process.gpmf.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Reads the key-length-value tree out of a GPMF payload.
 * <p>
 * A GPMF entry is an 8-byte header followed by its payload, padded out to a 32-bit boundary:
 * <pre>
 *   bytes 0-3  four-character key, for example ACCL
 *   byte  4    sample type code, see {@link GpmfType}
 *   byte  5    size in bytes of one sample
 *   bytes 6-7  number of samples (big-endian)
 *   bytes 8..  payload, of (sample size * sample count) bytes, then padded to a multiple of four
 * </pre>
 * Entries of the nested type hold a sequence of further entries as their payload, which is how a payload
 * describes its devices ({@code DEVC}) and their sensor streams ({@code STRM}).
 * <p>
 * This class only builds the tree. Turning it into telemetry, which means pairing each sensor stream with
 * its name, units, and scale divisors, is {@link GpmfPayload}'s job.
 *
 * @see <a href="https://github.com/gopro/gpmf-parser/blob/main/docs/README.md">GPMF specification</a>
 */
public final class GpmfParser {
    /**
     * Size in bytes of a GPMF entry header.
     */
    private static final int HEADER_SIZE = 8;

    /**
     * GPMF payloads are padded so that every entry starts on a 32-bit boundary.
     */
    private static final int ALIGNMENT = 4;

    private GpmfParser() {
    }

    /**
     * Reads the entries of a GPMF payload.
     *
     * @param data The payload bytes, as carried by one packet of a GPMF stream.
     * @return The top-level entries, normally a single {@code DEVC} container per device.
     * @throws GpmfParseException If the payload is malformed or truncated.
     */
    public static List<GpmfElement> parse(byte[] data) {
        if (data == null || data.length == 0) {
            return Collections.emptyList();
        }

        return parseEntries(data, 0, data.length);
    }

    /**
     * Reads the entries in one region of a payload, recursing into containers.
     *
     * @param data   The payload bytes.
     * @param offset Index of the first byte of the region.
     * @param end    Index just past the last byte of the region.
     * @return The entries found, in payload order.
     * @throws GpmfParseException If an entry is malformed or runs past the end of the region.
     */
    private static List<GpmfElement> parseEntries(byte[] data, int offset, int end) {
        List<GpmfElement> elements = new ArrayList<>();
        int position = offset;

        while (position + HEADER_SIZE <= end) {
            // A container is padded out with zeros once its entries are done
            if (data[position] == 0) {
                break;
            }

            String key = readKey(data, position);
            char typeCode = (char) (data[position + 4] & 0xFF);
            int structSize = data[position + 5] & 0xFF;
            int repeat = ((data[position + 6] & 0xFF) << 8) | (data[position + 7] & 0xFF);

            int payloadStart = position + HEADER_SIZE;
            int payloadLength = structSize * repeat;

            if (payloadLength < 0 || payloadStart + payloadLength > end) {
                throw new GpmfParseException("Entry " + key + " at offset " + position + " declares a "
                        + payloadLength + " byte payload that runs past the end of its container");
            }

            GpmfType type = GpmfType.fromCode(typeCode);

            List<GpmfElement> children = type == GpmfType.NESTED
                    ? parseEntries(data, payloadStart, payloadStart + payloadLength)
                    : Collections.emptyList();

            byte[] payload = type == GpmfType.NESTED
                    ? new byte[0]
                    : Arrays.copyOfRange(data, payloadStart, payloadStart + payloadLength);

            elements.add(new GpmfElement(key, type, typeCode, structSize, repeat, payload, children));

            position = payloadStart + align(payloadLength);
        }

        return elements;
    }

    /**
     * Reads the four-character key of an entry.
     *
     * @throws GpmfParseException If the key is not four printable ASCII characters, which means the
     *                            payload is not aligned where we think it is.
     */
    private static String readKey(byte[] data, int position) {
        char[] key = new char[4];

        for (int i = 0; i < 4; i++) {
            int character = data[position + i] & 0xFF;

            if (character < 0x20 || character > 0x7E) {
                throw new GpmfParseException("Entry at offset " + position
                        + " does not start with a printable four-character key");
            }

            key[i] = (char) character;
        }

        return new String(key);
    }

    /**
     * Rounds a payload length up to the next 32-bit boundary.
     */
    private static int align(int length) {
        int remainder = length % ALIGNMENT;
        return remainder == 0 ? length : length + (ALIGNMENT - remainder);
    }
}