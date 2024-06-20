/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2024 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.flir.dh390;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration settings for the FLIR DH-390 driver exposed via the OpenSensorHub Admin panel.
 */
public class DH390Config extends SensorConfig {
    /**
     * The serial number or unique identifier for the configured camera.
     */
    @DisplayInfo.Required
    @DisplayInfo(label = "Serial Number", desc = "Serial number of the FLIR DH-390 camera, used to uniquely identify the sensor.")
    public String serialNumber = "001";

    /**
     * Configuration options for the connection to the camera.
     */
    @DisplayInfo.Required
    @DisplayInfo(label = "Connection", desc = "Configuration options for connecting to the camera.")
    public DH390ConfigConnection connection = new DH390ConfigConnection();

    /**
     * Configuration options for the location and orientation of the camera.
     */
    @DisplayInfo(label = "Position", desc = "Location and orientation of the camera.")
    public PositionConfig positionConfig = new PositionConfig();

    @Override
    public PositionConfig.LLALocation getLocation() {
        return positionConfig.location;
    }
}
