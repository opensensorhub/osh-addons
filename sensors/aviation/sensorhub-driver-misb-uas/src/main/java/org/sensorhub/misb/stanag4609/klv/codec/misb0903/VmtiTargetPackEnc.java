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

import org.sensorhub.misb.stanag4609.klv.codec.SetEncoder;
import org.sensorhub.misb.stanag4609.klv.codec.ImapB;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class VmtiTargetPackEnc {

    private final int targetId;
    private final SetEncoder setEncoder =
            new SetEncoder(VmtiTargetPack.VTARGET_PACK_LOCAL_SET); // 0x65

    public static class Series {

        public static byte[] encodeSeries(List<byte[]> sets) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] ls : sets) {
                writeBerLength(out, ls.length);
                out.write(ls);
            }
            return out.toByteArray();
        }

        private static void writeBerLength(ByteArrayOutputStream out, int length) {
            if (length < 0x80) out.write(length);
            else {
                out.write(0x81);
                out.write(length);
            }
        }
    }

    public VmtiTargetPackEnc(int targetId) {
        this.targetId = targetId;
    }

    public VmtiTargetPackEnc centroid(long packed) {
        setEncoder.put((byte) 0x01, packed);
        return this;
    }

    public VmtiTargetPackEnc targetOffsetLat(double deg) {
        setEncoder.put((byte) 0x0A, ImapB.doubleToImapB(VmtiTargetPack.LOC_OFFSET_IMAPB_FUNC, deg));
        return this;
    }

    public VmtiTargetPackEnc targetOffsetLon(double deg) {
        setEncoder.put((byte) 0x0B, ImapB.doubleToImapB(VmtiTargetPack.LOC_OFFSET_IMAPB_FUNC, deg));
        return this;
    }

    public VmtiTargetPackEnc targetHae(double meters) {
        setEncoder.put((byte) 0x0C, ImapB.doubleToImapB(VmtiTargetPack.HAE_IMAPB_FUNC, meters));
        return this;
    }

    public byte[] encode() throws IOException {
        byte[] ls = setEncoder.encode(false);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeOid(out, targetId);
        out.write(ls);

        return out.toByteArray();
    }

    private void writeOid(ByteArrayOutputStream out, int oid) {
        if (oid < 0x80) out.write(oid);
        else {
            out.write(0x81);
            out.write(oid);
        }
    }
}
