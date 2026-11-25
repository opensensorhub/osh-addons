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

import java.nio.ByteBuffer;

/**
 * The sample types a GPMF key-length-value entry can declare, as defined by the GPMF specification.
 * <p>
 * The single-character code is what appears in the type field of a GPMF entry header. All multi-byte
 * values in a GPMF payload are big-endian.
 *
 * @see <a href="https://github.com/gopro/gpmf-parser/blob/main/docs/README.md">GPMF specification</a>
 */
public enum GpmfType {
    /** Signed 8-bit integer. */
    INT8('b', 1, true),
    /** Unsigned 8-bit integer. */
    UINT8('B', 1, true),
    /** ASCII character; an entry of this type holds a string. */
    CHAR('c', 1, false),
    /** 64-bit IEEE float. */
    DOUBLE('d', 8, true),
    /** 32-bit IEEE float. */
    FLOAT('f', 4, true),
    /** Four-character code. */
    FOURCC('F', 4, false),
    /** 128-bit UUID. */
    UUID('G', 16, false),
    /** Signed 64-bit integer. */
    INT64('j', 8, true),
    /** Unsigned 64-bit integer. */
    UINT64('J', 8, true),
    /** Signed 32-bit integer. */
    INT32('l', 4, true),
    /** Unsigned 32-bit integer. */
    UINT32('L', 4, true),
    /** Signed Q15.16 fixed-point number. */
    Q15_16('q', 4, true),
    /** Signed Q31.32 fixed-point number. */
    Q31_32('Q', 8, true),
    /** Signed 16-bit integer. */
    INT16('s', 2, true),
    /** Unsigned 16-bit integer. */
    UINT16('S', 2, true),
    /** UTC date string of the form {@code yymmddhhmmss.sss}. */
    UTC_DATE('U', 16, false),
    /** Heterogeneous structure whose field layout is given by a sibling {@code TYPE} entry. */
    COMPLEX('?', 0, false),
    /** Container whose payload is itself a sequence of GPMF entries. */
    NESTED('\0', 0, false);

    private final char code;
    private final int size;
    private final boolean numeric;

    GpmfType(char code, int size, boolean numeric) {
        this.code = code;
        this.size = size;
        this.numeric = numeric;
    }

    /**
     * @return The single-character type code as it appears in a GPMF entry header.
     */
    public char getCode() {
        return code;
    }

    /**
     * @return The size in bytes of one value of this type, or zero for the types whose size is not fixed.
     */
    public int getSize() {
        return size;
    }

    /**
     * @return True if values of this type can be read as numbers by {@link GpmfType#read(ByteBuffer)}.
     */
    public boolean isNumeric() {
        return numeric;
    }

    /**
     * Resolves a type code from a GPMF entry header.
     *
     * @param code The single-character type code.
     * @return The matching type, or null if the code is not one this implementation knows.
     */
    public static GpmfType fromCode(char code) {
        for (GpmfType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }

    /**
     * Reads one value of this type from the given buffer, advancing its position, and widens it to a double.
     * <p>
     * Fixed-point types are converted to their real value, and unsigned types are widened without sign
     * extension.
     *
     * @param buffer The buffer to read from, positioned at the value and in big-endian order.
     * @return The value as a double.
     * @throws IllegalStateException If this type is not numeric.
     */
    public double read(ByteBuffer buffer) {
        return switch (this) {
            case INT8 -> buffer.get();
            case UINT8 -> buffer.get() & 0xFF;
            case INT16 -> buffer.getShort();
            case UINT16 -> buffer.getShort() & 0xFFFF;
            case INT32 -> buffer.getInt();
            case UINT32 -> buffer.getInt() & 0xFFFFFFFFL;
            case INT64 -> buffer.getLong();
            case UINT64 -> unsignedToDouble(buffer.getLong());
            case FLOAT -> buffer.getFloat();
            case DOUBLE -> buffer.getDouble();
            // Q15.16 and Q31.32 are signed fixed-point, scaled by their fractional bit count
            case Q15_16 -> buffer.getInt() / 65536d;
            case Q31_32 -> buffer.getLong() / 4294967296d;
            default -> throw new IllegalStateException("GPMF type '" + code + "' is not numeric");
        };
    }

    /**
     * Widens a 64-bit value that should be read as unsigned, without sign extension.
     */
    private static double unsignedToDouble(long value) {
        return value >= 0 ? value : value + 0x1p64;
    }
}