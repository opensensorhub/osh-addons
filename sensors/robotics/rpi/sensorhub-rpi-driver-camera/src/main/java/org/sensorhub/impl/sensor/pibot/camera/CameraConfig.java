/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * See the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) 2021-2024 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.sensor.pibot.camera;

import org.sensorhub.impl.pibot.common.config.VideoParameters;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration settings for the CameraSensor driver exposed via the OpenSensorHub Admin panel.
 *
 * Configuration settings take the form of
 * <code>
 *     DisplayInfo(desc="Description of configuration field to show in UI")
 *     public Type configOption;
 * </code>
 *
 * Containing an annotation describing the setting and if applicable its range of values
 * as well as a public access variable of the given Type
 *
 * @author Nick Garay
 * @since Feb. 16, 2021
 */
public class CameraConfig extends SensorConfig {

    /**
     * The unique identifier for the configured UAS sensor platform.
     */
    @DisplayInfo.Required
    @DisplayInfo(desc="Serial number or unique identifier for UAS sensor platform")
    public String serialNumber = "camera";

    /**
     * The pin configuration
     */
    @DisplayInfo.Required
    @DisplayInfo(label="PinConfig", desc="Pin configuration for module")
    public CameraPinConfig pinConfig = new CameraPinConfig();

    /**
     * Video camera configuration
     */
    @DisplayInfo(label = "Video Camera Parameters", desc = "Parameters for camera configuration")
    public VideoParameters videoParameters = new VideoParameters();
}
