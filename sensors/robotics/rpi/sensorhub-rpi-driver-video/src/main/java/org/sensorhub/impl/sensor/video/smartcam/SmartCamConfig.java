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
package org.sensorhub.impl.sensor.video.smartcam;

import org.sensorhub.impl.pibot.common.config.VideoParameters;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.pibot.common.config.SensorPlacement;

/**
 * Configuration module for the OpenSensorHub driver
 *
 * @author Nick Garay
 * @since 1.0.0
 */
public class SmartCamConfig extends SensorConfig {

    @Required
    @DisplayInfo(desc="Camera serial number (used as suffix to generate unique identifier URI)")
    public String serialNumber = null;

    @DisplayInfo(label = "", desc = "")
    public VideoParameters videoParameters = new VideoParameters();

    @Required
    @DisplayInfo(desc="Location information for placement of a sensor")
    public SensorPlacement sensorPlacement = new SensorPlacement();
}
