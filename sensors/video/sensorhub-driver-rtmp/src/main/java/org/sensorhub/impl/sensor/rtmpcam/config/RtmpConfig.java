/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtmpcam.config;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;

public class RtmpConfig extends SensorConfig {
    @DisplayInfo.Required
    @DisplayInfo(label = "Video Stream ID", desc = "Serial number or unique identifier for video stream.")
    public String serialNumber = "video001";

    @DisplayInfo.Required
    @DisplayInfo(label = "Connection", desc = "Configuration options for source of RTMP.")
    public ConnectionConfig connectionConfig = new ConnectionConfig();

    /**
     * Configuration options for the location and orientation of the sensor.
     */
    @DisplayInfo(label = "Position", desc = "Location and orientation of the sensor.")
    public PositionConfig positionConfig = new PositionConfig();

    @Override
    public PositionConfig.LLALocation getLocation() {
        return positionConfig.location;
    }

    @Override
    public PositionConfig.EulerOrientation getOrientation() {
        return positionConfig.orientation;
    }
}
