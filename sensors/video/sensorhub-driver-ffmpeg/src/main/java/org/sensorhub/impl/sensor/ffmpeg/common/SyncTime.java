/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg.common;

/**
 * A class to contain synchronization timing information to synchronize Telemetry and Video outputs.
 *
 * @author Nick Garay
 * @since Apr. 2, 2020
 */
public class SyncTime {

    private double precisionTimeStamp;
    private double presentationTimeStamp;

    /**
     * Constructor
     *
     * @param precisionTimeStamp    A timestamp contained within the MISB STANAG 4609 Data Link Local Set Packets
     * @param presentationTimeStamp A timestamp received with the telemetry packets, this timestamp is extracted from
     *                              transport stream and used to align video with telemetry based on closest
     *                              telemetry data as given by the precisionTimeStamp
     */
    public SyncTime(double precisionTimeStamp, double presentationTimeStamp) {

        this.precisionTimeStamp = precisionTimeStamp;
        this.presentationTimeStamp = presentationTimeStamp;
    }

    /**
     * Retrieve the precision timestamp
     *
     * @return the precision timestamp
     */
    public double getPrecisionTimeStamp() {
        return precisionTimeStamp;
    }

    /**
     * Retrieve the presentation timestamp
     *
     * @return the presentation timestamp
     */
    public double getPresentationTimeStamp() {
        return presentationTimeStamp;
    }
}
