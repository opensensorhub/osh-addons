/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration settings for the UAS driver exposed via the OpenSensorHub Admin panel.
 * Specifically, allows for selection of outputs to provide
 *
 * @author Nick Garay
 * @since Oct. 6, 2020
 */
public class Outputs {

    @DisplayInfo(label = "Airframe Position", desc = "Aircraft location & attitude")
    public boolean enableAirframePosition = false;

    @DisplayInfo(label = "All Combined", desc = "All MISB STANAG 4609 MPEG-TS data")
    public boolean enableFullTelemetry = false;

    @DisplayInfo(label = "Geo Referenced Image Frame Data", desc = "4-corner+ reference frame for image location")
    public boolean enableGeoRefImageFrame = false;

    @DisplayInfo(label = "Gimbal Attitude", desc = "Gimbal yaw, pitch, and roll relative to airframe")
    public boolean enableGimbalAttitude = false;

    @DisplayInfo(label = "Identification", desc = "Aircraft identification")
    public boolean enableIdentification = false;

    @DisplayInfo(label = "Security", desc = "Security classification information")
    public boolean enableSecurity = false;

    @DisplayInfo(label = "Video", desc = "Video feed")
    public boolean enableVideo = false;
}
