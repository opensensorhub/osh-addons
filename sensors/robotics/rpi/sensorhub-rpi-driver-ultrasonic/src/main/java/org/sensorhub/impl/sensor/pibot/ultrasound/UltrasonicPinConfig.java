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
package org.sensorhub.impl.sensor.pibot.ultrasound;

import org.sensorhub.impl.pibot.common.config.GpioEnum;
import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration settings for the UltrasonicSensor Driver Pins exposed via the OpenSensorHub Admin panel.
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
 * @since Jan. 24, 2021
 */
public class UltrasonicPinConfig {

    @DisplayInfo.Required
    @DisplayInfo(label="Transmit Pin", desc="The GPIO pin for the Ultrasonic transmit signal")
    public GpioEnum echoPin = GpioEnum.PIN_30;

    @DisplayInfo.Required
    @DisplayInfo(label="Receive Pin", desc="The GPIO pin for the Ultrasonic receive signal")
    public GpioEnum triggerPin = GpioEnum.PIN_31;

    @DisplayInfo.Required
    @DisplayInfo(label="Servo Pin", desc="The GPIO pin to control the pan servo of the Ultrasonic Module")
    public GpioEnum servoPin = GpioEnum.PIN_04;
}
