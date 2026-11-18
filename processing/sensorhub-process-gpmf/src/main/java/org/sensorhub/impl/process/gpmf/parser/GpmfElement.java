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
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One key-length-value entry of a GPMF payload.
 * <p>
 * An entry is either a container, whose payload is a nested sequence of entries reachable through
 * {@link GpmfElement#getChildren()}, or a leaf holding {@link GpmfElement#getRepeat()} samples of
 * {@link GpmfElement#getStructSize()} bytes each.
 *
 * @see <a href="https://github.com/gopro/gpmf-parser/blob/main/docs/README.md">GPMF specification</a>
 */
public class GpmfElement {
    private final String key;
    private final GpmfType type;
    private final char typeCode;
    private final int structSize;
    private final int repeat;
    private final byte[] payload;
    private final List<GpmfElement> children;

    /**
     * @param key        The four-character key of this entry.
     * @param type       The declared sample type, or null if the type code is not one this implementation knows.
     * @param typeCode   The raw type code from the entry header, retained even when the type is unknown.
     * @param structSize The size in bytes of one sample.
     * @param repeat     The number of samples in the payload.
     * @param payload    The payload bytes, excluding the 32-bit alignment padding that follows them.
     * @param children   The entries nested inside this one, empty for a leaf.
     */
    GpmfElement(String key, GpmfType type, char typeCode, int structSize, int repeat, byte[] payload, List<GpmfElement> children) {
        this.key = key;
        this.type = type;
        this.typeCode = typeCode;
        this.structSize = structSize;
        this.repeat = repeat;
        this.payload = payload;
        this.children = children;
    }

    /**
     * @return The four-character key of this entry, for example {@code ACCL} or {@code STRM}.
     */
    public String getKey() {
        return key;
    }

    /**
     * @return The declared sample type, or null if the entry declared a type code this implementation
     * does not know. An unknown type is not an error; the payload is still available.
     */
    public GpmfType getType() {
        return type;
    }

    /**
     * @return The raw type code from the entry header.
     */
    public char getTypeCode() {
        return typeCode;
    }

    /**
     * @return The size in bytes of one sample.
     */
    public int getStructSize() {
        return structSize;
    }

    /**
     * @return The number of samples in the payload.
     */
    public int getRepeat() {
        return repeat;
    }

    /**
     * @return The payload bytes, excluding 32-bit alignment padding. Not copied; do not modify.
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * @return The entries nested inside this one, or an empty list for a leaf.
     */
    public List<GpmfElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * @return True if this entry is a container of other entries.
     */
    public boolean isNested() {
        return type == GpmfType.NESTED;
    }

    /**
     * @return The number of values in one sample, or zero if that cannot be determined from the type
     * alone -- which is the case for containers and for complex types, whose layout comes from a
     * sibling {@code TYPE} entry.
     */
    public int getElementCount() {
        if (type == null || !type.isNumeric() || type.getSize() == 0) {
            return 0;
        }
        return structSize / type.getSize();
    }

    /**
     * Finds the first entry nested directly inside this one with the given key.
     *
     * @param childKey The four-character key to look for.
     * @return The matching entry, or null if there is none.
     */
    public GpmfElement findChild(String childKey) {
        for (GpmfElement child : children) {
            if (child.key.equals(childKey)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Finds every entry nested directly inside this one with the given key.
     *
     * @param childKey The four-character key to look for.
     * @return The matching entries, in payload order.
     */
    public List<GpmfElement> findChildren(String childKey) {
        List<GpmfElement> matches = new ArrayList<>();
        for (GpmfElement child : children) {
            if (child.key.equals(childKey)) {
                matches.add(child);
            }
        }
        return matches;
    }

    /**
     * Reads the payload as text, for the character and date types.
     * <p>
     * GPMF text is Latin-1 rather than plain ASCII, which is what lets unit strings carry characters like
     * the micro sign in {@code µT} and the superscript in {@code m/s²}.
     *
     * @return The payload decoded as text with trailing padding and nulls removed.
     */
    public String asString() {
        return new String(payload, StandardCharsets.ISO_8859_1).replace('\0', ' ').trim();
    }

    /**
     * Reads the payload as a list of fixed-width text fields, one per sample.
     * <p>
     * An entry that describes something per element of a sample stores one field per element, each padded
     * out to {@link GpmfElement#getStructSize()} bytes. A {@code UNIT} entry for a GPS stream, for
     * example, holds nine three-byte fields, one for each of the coordinate, speed, and time values.
     * <p>
     * A sample size of one byte means the entry is a plain run of characters rather than a set of fields,
     * which is how a single-quantity stream states its one unit, so that is returned as a single string.
     *
     * @return The fields in payload order, each trimmed of its padding.
     */
    public List<String> asStrings() {
        if (structSize <= 1 || repeat <= 1) {
            return List.of(asString());
        }

        List<String> fields = new ArrayList<>(repeat);

        for (int i = 0; i < repeat && (i + 1) * structSize <= payload.length; i++) {
            fields.add(new String(payload, i * structSize, structSize, StandardCharsets.ISO_8859_1)
                    .replace('\0', ' ')
                    .trim());
        }

        return fields;
    }

    /**
     * Reads the payload as a flat sequence of numbers, one per value per sample, without applying any
     * scale divisor.
     *
     * @return The values in payload order, or an empty array if the type is not numeric.
     */
    public double[] asDoubles() {
        int elementCount = getElementCount();
        if (elementCount == 0) {
            return new double[0];
        }

        // Trust the payload length rather than repeat, so a truncated payload yields what it actually holds
        int valueCount = Math.min(repeat * elementCount, payload.length / type.getSize());
        double[] values = new double[valueCount];

        ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < valueCount; i++) {
            values[i] = type.read(buffer);
        }

        return values;
    }

    @Override
    public String toString() {
        return isNested()
                ? key + " {" + children.size() + " entries}"
                : key + " [" + typeCode + " x" + structSize + " x" + repeat + "]";
    }
}