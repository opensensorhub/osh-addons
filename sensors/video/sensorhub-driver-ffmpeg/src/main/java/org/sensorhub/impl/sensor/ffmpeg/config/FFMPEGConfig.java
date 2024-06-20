/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg.config;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration settings for the FFmpeg driver exposed via the OpenSensorHub Admin panel.
 */
public class FFMPEGConfig extends SensorConfig {
    /**
     * The unique identifier for the configured FFmpeg sensor platform.
     */
    @DisplayInfo.Required
    @DisplayInfo(label = "Video Stream ID", desc = "Serial number or unique identifier for video stream.")
    public String serialNumber = "video001";

    /**
     * Configuration options for the connection to the FFmpeg video stream.
     */
    @DisplayInfo.Required
    @DisplayInfo(label = "Connection", desc = "Configuration options for source of FFMPEG.")
    public Connection connection = new Connection();

    /**
     * Configuration options for the location and orientation of the sensor.
     */
    @DisplayInfo(label = "Position", desc = "Location and orientation of the sensor.")
    public PositionConfig positionConfig = new PositionConfig();

    @Override
    public PositionConfig.LLALocation getLocation() {
        return positionConfig.location;
    }
}
