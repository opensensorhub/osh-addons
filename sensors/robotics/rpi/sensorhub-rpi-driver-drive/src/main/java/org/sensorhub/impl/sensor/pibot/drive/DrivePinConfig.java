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
package org.sensorhub.impl.sensor.pibot.drive;

import org.sensorhub.impl.pibot.common.config.GpioEnum;
import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration settings for the DriveSensor Driver Pins exposed via the OpenSensorHub Admin panel.
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
 * @since Feb. 15, 2021
 */
public class DrivePinConfig {

    @DisplayInfo.Required
    @DisplayInfo(label="Right Motor PWM Pin", desc="The GPIO pin for the motor PWM pin")
    public GpioEnum rightMotorPwm = GpioEnum.PIN_23;

    @DisplayInfo.Required
    @DisplayInfo(label="Right Motor Forward Pin Pin", desc="The GPIO pin for the motor forward pin")
    public GpioEnum rightMotorForward = GpioEnum.PIN_24;

    @DisplayInfo.Required
    @DisplayInfo(label="Right Motor Reverse Pin", desc="The GPIO pin for the motor reverse pin")
    public GpioEnum rightMotorReverse = GpioEnum.PIN_25;

    @DisplayInfo.Required
    @DisplayInfo(label="Left Motor PWM Pin", desc="The GPIO pin for the motor PWM pin")
    public GpioEnum leftMotorPwm = GpioEnum.PIN_27;

    @DisplayInfo.Required
    @DisplayInfo(label="Left Motor Forward Pin", desc="The GPIO pin for the motor forward pin")
    public GpioEnum leftMotorForward = GpioEnum.PIN_28;

    @DisplayInfo.Required
    @DisplayInfo(label="Left Motor Reverse Pin", desc="The GPIO pin for the motor reverse pin")
    public GpioEnum leftMotorReverse = GpioEnum.PIN_29;
}
