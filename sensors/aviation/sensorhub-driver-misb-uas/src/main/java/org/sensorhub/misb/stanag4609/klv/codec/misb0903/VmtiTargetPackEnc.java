/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.klv.codec.misb0903;

import org.sensorhub.misb.stanag4609.klv.codec.ImapB;
import org.sensorhub.misb.stanag4609.klv.codec.SetEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Encoder for a single MISB ST 0903 VTarget Pack (Tag 0x65).
 *
 * <p>A VTarget Pack is a nested Local Set inside the VMTI Local Set and
 * represents a single detected target. Each pack is prefixed by a BER‑OID
 * (Object Identifier) identifying the target number, followed by the encoded
 * Local Set body.</p>
 *
 * <p>This encoder provides typed setters for common ST 0903 VTarget fields:
 * <ul>
 *     <li>Centroid pixel location (packed)</li>
 *     <li>Latitude/longitude offsets (IMAPB‑encoded)</li>
 *     <li>HAE (height above ellipsoid) offset (IMAPB‑encoded)</li>
 * </ul>
 * Additional fields can be added by extending the class or using the underlying
 * {@link SetEncoder} directly.</p>
 */
public class VmtiTargetPackEnc {

    /**
     * Unique target identifier encoded as a BER‑OID prefix.
     */
    private final int targetId;

    /**
     * Internal encoder for the VTarget Pack Local Set (tag 0x65).
     */
    private final SetEncoder setEncoder =
            new SetEncoder(VmtiTargetPack.VTARGET_PACK_LOCAL_SET);

    /**
     * Utility class for encoding a series of VTarget Packs into a KLV Series.
     *
     * <p>ST 0903 defines the VTarget Series as a sequence of Local Sets, each
     * prefixed by a BER length. This class handles the BER length encoding and
     * concatenation of the encoded Local Sets.</p>
     */
    public static class Series {

        /**
         * Encodes a list of VTarget Pack Local Sets into a KLV Series.
         *
         * @param sets list of encoded VTarget Pack Local Sets
         * @return concatenated KLV Series
         */
        public static byte[] encodeSeries(List<byte[]> sets) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] ls : sets) {
                writeBerLength(out, ls.length);
                out.write(ls);
            }
            return out.toByteArray();
        }

        /**
         * Writes a BER length field for a Local Set.
         *
         * <p>For lengths < 128, a single byte is used. For larger values,
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
    }

    /**
     * Creates a new VTarget Pack encoder for the given target ID.
     *
     * @param targetId numeric identifier for this target (OID prefix)
     */
    public VmtiTargetPackEnc(int targetId) {
        this.targetId = targetId;
    }

    /**
     * ST0903 Tag 0x01 – Target Centroid (packed pixel coordinates).
     *
     * <p>The centroid is typically encoded as a packed 32‑bit value containing
     * X and Y pixel coordinates. Packing is performed externally.</p>
     */
    public VmtiTargetPackEnc centroid(long packed) {
        setEncoder.put((byte) 0x01, packed);
        return this;
    }

    /**
     * ST0903 Tag 0x0A – Target Latitude Offset (IMAPB‑encoded).
     *
     * <p>Offsets are relative to the frame center latitude and encoded using
     * the IMAPB function defined in {@link VmtiTargetPack#LOC_OFFSET_IMAPB_FUNC}.</p>
     */
    public VmtiTargetPackEnc targetOffsetLat(double deg) {
        setEncoder.put((byte) 0x0A, ImapB.doubleToImapB(VmtiTargetPack.LOC_OFFSET_IMAPB_FUNC, deg));
        return this;
    }

    /**
     * ST0903 Tag 0x0B – Target Longitude Offset (IMAPB‑encoded).
     */
    public VmtiTargetPackEnc targetOffsetLon(double deg) {
        setEncoder.put((byte) 0x0B, ImapB.doubleToImapB(VmtiTargetPack.LOC_OFFSET_IMAPB_FUNC, deg));
        return this;
    }

    /**
     * ST0903 Tag 0x0C – Target Height Above Ellipsoid Offset (IMAPB‑encoded).
     */
    public VmtiTargetPackEnc targetHae(double meters) {
        setEncoder.put((byte) 0x0C, ImapB.doubleToImapB(VmtiTargetPack.HAE_IMAPB_FUNC, meters));
        return this;
    }

    /**
     * Encodes the VTarget Pack into a KLV byte array.
     *
     * <p>The output format is:
     * <pre>
     *   [BER‑OID targetId] [Local Set body]
     * </pre>
     * The Local Set body is encoded without its own UL/length, as required by
     * ST 0903 for nested Local Sets.</p>
     *
     * @return encoded VTarget Pack
     */
    public byte[] encode() throws IOException {
        byte[] ls = setEncoder.encode(false);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeOid(out, targetId);
        out.write(ls);

        return out.toByteArray();
    }

    /**
     * Writes the BER‑OID prefix for this VTarget Pack.
     *
     * <p>For OIDs < 128, a single byte is used. For larger values, the encoder
     * emits 0x81 followed by the OID byte.</p>
     */
    private void writeOid(ByteArrayOutputStream out, int oid) {
        if (oid < 0x80) {
            out.write(oid);
        } else {
            out.write(0x81);
            out.write(oid);
        }
    }
}
