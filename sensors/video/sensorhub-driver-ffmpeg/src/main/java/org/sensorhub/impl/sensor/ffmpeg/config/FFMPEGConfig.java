/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg.config;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration settings for the FFMPEG driver exposed via the OpenSensorHub Admin panel.
 * <p>
 * Configuration settings take the form of
 * <code>
 * DisplayInfo(desc="Description of configuration field to show in UI")
 * public Type configOption;
 * </code>
 * <p>
 * Containing an annotation describing the setting and if applicable its range of values
 * as well as a public access variable of the given Type
 *
 * @author Drew Botts
 * @since Feb. 6, 2023
 */
public class FFMPEGConfig extends SensorConfig {

    /**
     * The unique identifier for the configured UAS sensor platform.
     */
    @DisplayInfo.Required
    @DisplayInfo(label = "Video Stream ID", desc = "Serial number or unique identifier for video stream.")
    public String serialNumber = "video001";

    @DisplayInfo.Required
    @DisplayInfo(label = "Connection", desc = "Configuration options for source of MISB STANAG 4609 MPEG-TS")
    public Connection connection = new Connection();

}
