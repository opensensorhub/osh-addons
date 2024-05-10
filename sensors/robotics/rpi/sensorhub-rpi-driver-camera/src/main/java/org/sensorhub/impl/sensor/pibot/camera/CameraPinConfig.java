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

import org.sensorhub.impl.pibot.common.config.GpioEnum;
import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration settings for the CameraSensor Driver Pins exposed via the OpenSensorHub Admin panel.
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
public class CameraPinConfig {

    @DisplayInfo.Required
    @DisplayInfo(label="Pan Servo Pin", desc="The GPIO pin to control the pan servo of the Camera Module")
    public GpioEnum panServoPin = GpioEnum.PIN_14;

    @DisplayInfo.Required
    @DisplayInfo(label="Tilt Servo Pin", desc="The GPIO pin to control the tilt servo of the Camera Module")
    public GpioEnum tiltServoPin = GpioEnum.PIN_13;
}
