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
 * Simple MPEG-TS muxer for KLV metadata on PID 0x0101.
 * Produces TS packets (188 bytes each) containing a single PES with KLV payload.
 * <p>
 * Usage:
 * <pre>
 * KlvFrameWriter frameWriter = new KlvFrameWriter();
 * // ... configure UAS, Security, VMTI, targets, etc.
 * byte[] klv = frameWriter.encodeFrameMetadata();
 *
 * // Convert frame PTS (microseconds) to 90 kHz clock
 * long pts90k = (framePtsMicros * 90L) / 1000L;
 *
 * TsKlvMuxer muxer = new TsKlvMuxer();
 * List<byte[]> tsPackets = muxer.muxKlvToTs(klv, pts90k);
 *
 * // Write tsPackets to your TS stream alongside video/audio PIDs
 * </pre>
 */
public class KlvMuxer {

    private static final int TS_PACKET_SIZE = 188;
    private static final int TS_SYNC_BYTE = 0x47;
    private static final int KLV_PID = 0x0101;
    private static final int STREAM_ID_PRIVATE_1 = 0xBD;

    private int continuityCounter = 0;

    /**
     * Builds TS packets carrying a single KLV PES.
     *
     * @param klvData raw KLV UL + BER length + value
     * @param pts90k  PTS in 90 kHz units (e.g. micros * 90 / 1000)
     */
    public List<byte[]> muxKlvToTs(byte[] klvData, long pts90k) throws IOException {
        byte[] pes = buildPesPacket(klvData, pts90k);
        return packetizePesToTs(pes);
    }

    private byte[] buildPesPacket(byte[] klvData, long pts90k) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // PES header
        out.write(0x00);
        out.write(0x00);
        out.write(0x01);
        out.write(STREAM_ID_PRIVATE_1);

        // PES_packet_length: can be 0 for unbounded, but here we set it
        int pesPayloadLen = 3 + 5 + klvData.length; // flags+hdrlen + PTS + data
        out.write((pesPayloadLen >> 8) & 0xFF);
        out.write(pesPayloadLen & 0xFF);

        // '10' + scrambling + priority + alignment + copyright + original
        out.write(0x80); // '10' + no scrambling, no priority, no alignment, etc.

        // PTS only
        out.write(0x80); // PTS flag set, DTS flag clear

        // PES_header_data_length
        out.write(0x05); // 5 bytes for PTS

        // PTS (33 bits) encoded per ISO/IEC 13818-1
        writePtsDts(out, 0x02, pts90k);

        // PES payload = KLV data
        out.write(klvData);

        return out.toByteArray();
    }

    private void writePtsDts(ByteArrayOutputStream out, int prefix, long ts90k) {
        long pts = ts90k & 0x1FFFFFFFFL; // 33 bits

        int b1 = (int) (((prefix & 0x0F) << 4) | (((pts >> 30) & 0x07) << 1) | 1);
        int b2 = (int) (((pts >> 22) & 0xFF));
        int b3 = (int) ((((pts >> 15) & 0x7F) << 1) | 1);
        int b4 = (int) (((pts >> 7) & 0xFF));
        int b5 = (int) ((((pts & 0x7F) << 1) | 1));

        out.write(b1);
        out.write(b2);
        out.write(b3);
        out.write(b4);
        out.write(b5);
    }

    private List<byte[]> packetizePesToTs(byte[] pes) {
        List<byte[]> packets = new ArrayList<>();

        int offset = 0;
        boolean firstPacket = true;

        while (offset < pes.length) {
            byte[] ts = new byte[TS_PACKET_SIZE];

            // TS header
            ts[0] = (byte) TS_SYNC_BYTE;

            int payloadUnitStart = firstPacket ? 1 : 0;
            int pidHigh = (KLV_PID >> 8) & 0x1F;
            int pidLow = KLV_PID & 0xFF;

            ts[1] = (byte) ((0 << 7) | (payloadUnitStart << 6) | (0 << 5) | pidHigh);
            ts[2] = (byte) pidLow;

            ts[3] = (byte) ((0 << 6) | (1 << 4) | (continuityCounter & 0x0F)); // no adaptation, payload only
            continuityCounter = (continuityCounter + 1) & 0x0F;

            int headerLen = 4;
            int payloadCapacity = TS_PACKET_SIZE - headerLen;

            int remaining = pes.length - offset;
            int toCopy = Math.min(remaining, payloadCapacity);

            System.arraycopy(pes, offset, ts, headerLen, toCopy);
            offset += toCopy;

            // If last packet has unused bytes, they remain 0x00 (stuffing)
            packets.add(ts);
            firstPacket = false;
        }

        return packets;
    }
}
