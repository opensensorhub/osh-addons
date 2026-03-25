/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.comm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal MPEG-TS muxer for embedding MISB KLV metadata into a Transport Stream.
 * <p>
 * This class produces a sequence of 188‑byte MPEG‑TS packets containing a single
 * PES (Packetized Elementary Stream) with KLV payload on PID {@code 0x0101}.
 * It is intentionally simple and suitable for real‑time metadata streaming
 * alongside encoded video.
 *
 * <h3>Typical Usage</h3>
 * <pre>
 * KlvFrameWriter frameWriter = new KlvFrameWriter();
 * byte[] klv = frameWriter.encodeFrameMetadata();
 *
 * long pts90k = (framePtsMicros * 90L) / 1000L; // convert to 90 kHz clock
 *
 * KlvMuxer muxer = new KlvMuxer();
 * List<byte[]> tsPackets = muxer.muxKlvToTs(klv, pts90k);
 *
 * // Write tsPackets to your TS output alongside video/audio PIDs
 * </pre>
 */
public class KlvMuxer {

    /** Standard MPEG‑TS packet size in bytes. */
    private static final int TS_PACKET_SIZE = 188;

    /** Sync byte required at the start of every TS packet. */
    private static final int TS_SYNC_BYTE = 0x47;

    /** PID used for KLV metadata stream. */
    private static final int KLV_PID = 0x0101;

    /** PES stream ID for private data streams (per ISO/IEC 13818‑1). */
    private static final int STREAM_ID_PRIVATE_1 = 0xBD;

    /** 4‑bit continuity counter incremented for each TS packet. */
    private int continuityCounter = 0;

    /**
     * Builds a complete PES packet containing KLV metadata and packetizes it
     * into 188‑byte MPEG‑TS packets.
     *
     * @param klvData raw KLV payload (UL + BER length + value)
     * @param pts90k  presentation timestamp in 90 kHz clock units
     * @return list of 188‑byte TS packets ready for muxing
     * @throws IOException if PES construction fails
     */
    public List<byte[]> muxKlvToTs(byte[] klvData, long pts90k) throws IOException {
        byte[] pes = buildPesPacket(klvData, pts90k);
        return packetizePesToTs(pes);
    }

    /**
     * Constructs a PES packet containing the KLV payload.
     * <p>
     * PES layout:
     * <ul>
     *   <li>Start code prefix (0x000001)</li>
     *   <li>Stream ID (private_stream_1)</li>
     *   <li>PES packet length</li>
     *   <li>PES header flags</li>
     *   <li>PTS (5 bytes)</li>
     *   <li>KLV payload</li>
     * </ul>
     *
     * @param klvData KLV metadata block
     * @param pts90k  PTS timestamp in 90 kHz units
     */
    private byte[] buildPesPacket(byte[] klvData, long pts90k) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // --- PES start code prefix ---
        out.write(0x00);
        out.write(0x00);
        out.write(0x01);

        // Stream ID for private data
        out.write(STREAM_ID_PRIVATE_1);

        // PES_packet_length = header (3 + 5 bytes) + payload
        int pesPayloadLen = 3 + 5 + klvData.length;
        out.write((pesPayloadLen >> 8) & 0xFF);
        out.write(pesPayloadLen & 0xFF);

        // Flags: '10' + no scrambling, no priority, no alignment
        out.write(0x80);

        // PTS only (no DTS)
        out.write(0x80);

        // PES_header_data_length = 5 bytes (PTS)
        out.write(0x05);

        // Write PTS in ISO/IEC 13818‑1 format
        writePtsDts(out, 0x02, pts90k);

        // Append KLV payload
        out.write(klvData);

        return out.toByteArray();
    }

    /**
     * Writes a 33‑bit PTS/DTS timestamp using the MPEG‑2 bit‑split format:
     * <pre>
     *  '0010' or '0011' prefix
     *  PTS[32..30]
     *  marker bit
     *  PTS[29..22]
     *  marker bit
     *  PTS[21..15]
     *  marker bit
     *  PTS[14..7]
     *  marker bit
     *  PTS[6..0]
     *  marker bit
     * </pre>
     *
     * @param out    output stream
     * @param prefix 4‑bit prefix (2 = PTS, 3 = DTS)
     * @param ts90k  timestamp in 90 kHz units
     */
    private void writePtsDts(ByteArrayOutputStream out, int prefix, long ts90k) {
        long pts = ts90k & 0x1FFFFFFFFL; // 33 bits

        int b1 = (int) (((prefix & 0x0F) << 4) | (((pts >> 30) & 0x07) << 1) | 1);
        int b2 = (int) ((pts >> 22) & 0xFF);
        int b3 = (int) ((((pts >> 15) & 0x7F) << 1) | 1);
        int b4 = (int) ((pts >> 7) & 0xFF);
        int b5 = (int) ((((pts & 0x7F) << 1) | 1));

        out.write(b1);
        out.write(b2);
        out.write(b3);
        out.write(b4);
        out.write(b5);
    }

    /**
     * Splits a PES packet into a sequence of 188‑byte MPEG‑TS packets.
     * <p>
     * Each TS packet contains:
     * <ul>
     *   <li>Sync byte</li>
     *   <li>PID = 0x0101</li>
     *   <li>Payload Unit Start Indicator set on first packet</li>
     *   <li>No adaptation field (payload only)</li>
     *   <li>Continuity counter incremented per packet</li>
     * </ul>
     *
     * @param pes PES packet to packetize
     * @return list of 188‑byte TS packets
     */
    private List<byte[]> packetizePesToTs(byte[] pes) {
        List<byte[]> packets = new ArrayList<>();

        int offset = 0;
        boolean firstPacket = true;

        while (offset < pes.length) {
            byte[] ts = new byte[TS_PACKET_SIZE];

            // Sync byte
            ts[0] = (byte) TS_SYNC_BYTE;

            // Payload Unit Start Indicator only on first packet
            int payloadUnitStart = firstPacket ? 1 : 0;

            // PID split across bytes 1–2
            int pidHigh = (KLV_PID >> 8) & 0x1F;
            int pidLow = KLV_PID & 0xFF;

            ts[1] = (byte) ((0 << 7) | (payloadUnitStart << 6) | (0 << 5) | pidHigh);
            ts[2] = (byte) pidLow;

            // Flags: no adaptation field, payload only
            ts[3] = (byte) ((0 << 6) | (1 << 4) | (continuityCounter & 0x0F));
            continuityCounter = (continuityCounter + 1) & 0x0F;

            int headerLen = 4;
            int payloadCapacity = TS_PACKET_SIZE - headerLen;

            int remaining = pes.length - offset;
            int toCopy = Math.min(remaining, payloadCapacity);

            // Copy PES fragment into TS payload
            System.arraycopy(pes, offset, ts, headerLen, toCopy);
            offset += toCopy;

            // Remaining bytes (if any) stay as 0x00 stuffing
            packets.add(ts);
            firstPacket = false;
        }

        return packets;
    }
}
