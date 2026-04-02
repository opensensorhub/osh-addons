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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Encoder for the MISB ST 0903 VMTI (Video Moving Target Indicator) Local Set.
 *
 * <p>This class provides a fluent API for constructing a complete VMTI Local Set
 * (tag 0x4A) by populating individual ST 0903 metadata fields. Internally it
 * delegates to {@link SetEncoder}, which handles KLV BER lengths, tag/value
 * encoding, and final Local Set assembly.</p>
 *
 * <p>The encoder supports:
 * <ul>
 *     <li>Precision timestamp</li>
 *     <li>Total and reported target counts</li>
 *     <li>Frame size (width/height)</li>
 *     <li>Sensor field of view (HFOV/VFOV) encoded using IMAPB</li>
 *     <li>Series of VTarget Packs (ST 0903 VTarget Local Sets)</li>
 * </ul>
 * </p>
 *
 * <h3>Usage Example</h3>
 * <pre>
 * byte[] vmti = new VmtiLocalSetEnc()
 *         .precisionTimeStamp(ptsMicros)
 *         .totalTargets(targets.size())
 *         .reportedTargets(targets.size())
 *         .frameSize(1920, 1080)
 *         .fov(20.0, 11.0)
 *         .vTargetSeries(targetPacks)
 *         .encode(true);
 * </pre>
 */
public class VmtiLocalSetEnc {

    /**
     * Internal encoder for the VMTI Local Set (tag 0x4A).
     */
    private final SetEncoder setEncoder =
            new SetEncoder(VmtiLocalSet.VMTI_LOCAL_SET);

    /**
     * ST0903 Tag 0x02 – Precision Time Stamp (microseconds).
     */
    public VmtiLocalSetEnc precisionTimeStamp(long micros) {
        setEncoder.put((byte) 0x02, micros);
        return this;
    }

    /**
     * ST0903 Tag 0x05 – Total Number of Targets Detected.
     */
    public VmtiLocalSetEnc totalTargets(int total) {
        setEncoder.put((byte) 0x05, (long) total);
        return this;
    }

    /**
     * ST0903 Tag 0x06 – Number of Reported Targets.
     */
    public VmtiLocalSetEnc reportedTargets(int reported) {
        setEncoder.put((byte) 0x06, (long) reported);
        return this;
    }

    /**
     * ST0903 Tags 0x08 and 0x09 – Frame Width and Height (pixels).
     */
    public VmtiLocalSetEnc frameSize(int width, int height) {
        setEncoder.put((byte) 0x08, (long) width);
        setEncoder.put((byte) 0x09, (long) height);
        return this;
    }

    /**
     * ST0903 Tags 0x0B and 0x0C – Sensor Horizontal and Vertical Field of View.
     *
     * <p>Values are encoded using IMAPB per ST 0903 Annex A. The mapping
     * function is provided by {@link VmtiLocalSet#FOV_IMAPB_FUNC}.</p>
     */
    public VmtiLocalSetEnc fov(double hFov, double vFov) {
        setEncoder.put((byte) 0x0B, ImapB.doubleToImapB(VmtiLocalSet.FOV_IMAPB_FUNC, hFov));
        setEncoder.put((byte) 0x0C, ImapB.doubleToImapB(VmtiLocalSet.FOV_IMAPB_FUNC, vFov));
        return this;
    }

    /**
     * ST0903 Tag 0x65 – VTarget Series.
     *
     * <p>Encodes a list of {@link VmtiTargetPackEnc} objects into a KLV Series
     * (a sequence of nested Local Sets). Each VTarget Pack is encoded
     * individually, then wrapped using {@link VmtiTargetPackEnc.Series}.</p>
     *
     * @param targets list of VTarget Pack encoders
     */
    public VmtiLocalSetEnc vTargetSeries(List<VmtiTargetPackEnc> targets) throws IOException {
        List<byte[]> encoded = new ArrayList<>();
        for (VmtiTargetPackEnc t : targets)
            encoded.add(t.encode());

        setEncoder.put((byte) 0x65, VmtiTargetPackEnc.Series.encodeSeries(encoded));
        return this;
    }

    /**
     * Encodes the VMTI Local Set into a KLV byte array.
     *
     * @param withKeyAndLength whether to include the UL and BER length fields
     * @return encoded KLV bytes
     */
    public byte[] encode(boolean withKeyAndLength) throws IOException {
        return setEncoder.encode(withKeyAndLength);
    }
}
