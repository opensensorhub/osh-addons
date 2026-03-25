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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VmtiLocalSetEnc {

    private final SetEncoder setEncoder =
            new SetEncoder(VmtiLocalSet.VMTI_LOCAL_SET); // 0x4A

    public VmtiLocalSetEnc precisionTimeStamp(long micros) {
        setEncoder.put((byte) 0x02, micros);
        return this;
    }

    public VmtiLocalSetEnc totalTargets(int total) {
        setEncoder.put((byte) 0x05, (long) total);
        return this;
    }

    public VmtiLocalSetEnc reportedTargets(int reported) {
        setEncoder.put((byte) 0x06, (long) reported);
        return this;
    }

    public VmtiLocalSetEnc frameSize(int width, int height) {
        setEncoder.put((byte) 0x08, (long) width);
        setEncoder.put((byte) 0x09, (long) height);
        return this;
    }

    public VmtiLocalSetEnc fov(double hFov, double vFov) {
        setEncoder.put((byte) 0x0B, ImapB.doubleToImapB(VmtiLocalSet.FOV_IMAPB_FUNC, hFov));
        setEncoder.put((byte) 0x0C, ImapB.doubleToImapB(VmtiLocalSet.FOV_IMAPB_FUNC, vFov));
        return this;
    }

    public VmtiLocalSetEnc vTargetSeries(List<VmtiTargetPackEnc> targets) throws IOException {
        List<byte[]> encoded = new ArrayList<>();
        for (VmtiTargetPackEnc t : targets)
            encoded.add(t.encode());

        setEncoder.put((byte) 0x65, VmtiTargetPackEnc.Series.encodeSeries(encoded));
        return this;
    }

    public byte[] encode(boolean withKeyAndLength) throws IOException {
        return setEncoder.encode(withKeyAndLength);
    }
}
