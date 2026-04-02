/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.klv;

import org.sensorhub.misb.stanag4609.klv.codec.misb0102.SecurityLocalSetEnc;
import org.sensorhub.misb.stanag4609.klv.codec.misb0601.UasDataLinkSetEnc;
import org.sensorhub.misb.stanag4609.klv.codec.misb0903.VmtiLocalSetEnc;
import org.sensorhub.misb.stanag4609.klv.codec.misb0903.VmtiTargetPackEnc;

import java.io.IOException;
import java.util.List;

/**
 * Builds a complete KLV metadata blob for a single video frame:
 * - UAS Datalink Local Set (ST 0601)
 * - Security Local Set (ST 0102)
 * - VMTI Local Set + VTarget Series (ST 0903)
 * <p>
 * Usage:
 * <pre>
 * var frameKlv = new KlvFrameWriter()
 *         .uasPrecisionTimeStamp(ptsMicros)
 *         .uasMissionId("MISSION-01")
 *         .uasPlatformTailNumber("TAIL-001")
 *         .uasPlatformHeading(123.4)
 *         .uasSensorLatLon(34.123456, -117.987654)
 *         .uasFrameCenterLatLon(34.123400, -117.987600)
 *         .security(new SecurityLocalSetWriter()
 *                 .classification(0x01) // UNCLASSIFIED
 *                 .classifyingCountry("USA"))
 *         .vmtiFromTargets(ptsMicros,
 *                          1920, 1080,
 *                          20.0, 11.0,
 *                          targetPackWriters)
 *         .encodeFrameMetadata();
 * </pre>
 */
public class KlvFrameWriter {

    private final UasDataLinkSetEnc uasDataLinkSetEnc;
    private SecurityLocalSetEnc securityLocalSetEnc;
    private VmtiLocalSetEnc vmtiLocalSetEncoder;

    public KlvFrameWriter() {
        this.uasDataLinkSetEnc = new UasDataLinkSetEnc();
    }

    // ---------- UAS (ST 0601) ----------

    public KlvFrameWriter uasPrecisionTimeStamp(long micros) {
        uasDataLinkSetEnc.precisionTimeStamp(micros);
        return this;
    }

    public KlvFrameWriter uasMissionId(String missionId) {
        uasDataLinkSetEnc.missionId(missionId);
        return this;
    }

    public KlvFrameWriter uasPlatformTailNumber(String tail) {
        uasDataLinkSetEnc.platformTailNumber(tail);
        return this;
    }

    public KlvFrameWriter uasPlatformHeading(double deg) {
        uasDataLinkSetEnc.platformHeading(deg);
        return this;
    }

    public KlvFrameWriter uasSensorLatLon(double latDeg, double lonDeg) {
        uasDataLinkSetEnc.sensorLatitude(latDeg).sensorLongitude(lonDeg);
        return this;
    }

    public KlvFrameWriter uasFrameCenterLatLon(double latDeg, double lonDeg) {
        uasDataLinkSetEnc.frameCenterLatitude(latDeg).frameCenterLongitude(lonDeg);
        return this;
    }

    // ---------- Security (ST 0102) ----------

    public KlvFrameWriter security(SecurityLocalSetEnc securityLocalSetEnc) {
        this.securityLocalSetEnc = securityLocalSetEnc;
        return this;
    }

    // ---------- VMTI (ST 0903) ----------

    public KlvFrameWriter vmti(VmtiLocalSetEnc vmtiLocalSetEncoder) {
        this.vmtiLocalSetEncoder = vmtiLocalSetEncoder;
        return this;
    }

    public KlvFrameWriter vmtiFromTargets(long precisionTsMicros,
                                          int frameWidth,
                                          int frameHeight,
                                          double hFovDeg,
                                          double vFovDeg,
                                          List<VmtiTargetPackEnc> targets) throws IOException {
        VmtiLocalSetEnc vmti = new VmtiLocalSetEnc()
                .precisionTimeStamp(precisionTsMicros)
                .totalTargets(targets.size())
                .reportedTargets(targets.size())
                .frameSize(frameWidth, frameHeight)
                .fov(hFovDeg, vFovDeg)
                .vTargetSeries(targets);
        return vmti(vmti);
    }

    // ---------- Final assembly ----------

    /**
     * Builds the final KLV blob for this frame:
     * - Encodes Security LS (if present) and embeds into UAS LS
     * - Encodes VMTI LS (if present) and embeds into UAS LS
     * - Encodes UAS LS and returns its KLV bytes
     */
    public byte[] encodeFrameMetadata() throws IOException {
        // Security LS
        if (securityLocalSetEnc != null) {
            byte[] secBytes = securityLocalSetEnc.encode(false);
            uasDataLinkSetEnc.securityLocalSet(secBytes);
        }

        // VMTI LS
        if (vmtiLocalSetEncoder != null) {
            byte[] vmtiBytes = vmtiLocalSetEncoder.encode(false);
            uasDataLinkSetEnc.vmtiLocalSet(vmtiBytes);
        }

        // UAS LS (top-level KLV)
        return uasDataLinkSetEnc.encode(true);
    }
}
