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
package org.sensorhub.impl.sensor.pibot.searchlight;

import org.sensorhub.impl.pibot.common.config.GpioEnum;
import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration settings for the SearchlightSensor Driver Pins exposed via the OpenSensorHub Admin panel.
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
public class SearchlightPinConfig {

    @DisplayInfo.Required
    @DisplayInfo(label="Red LED Pin", desc="The GPIO pin for the red led")
    public GpioEnum redLedPin = GpioEnum.PIN_03;

    @DisplayInfo.Required
    @DisplayInfo(label="Green LED Pin", desc="The GPIO pin for the green led")
    public GpioEnum greenLedPin = GpioEnum.PIN_02;

    @DisplayInfo.Required
    @DisplayInfo(label="Blue LED Pin", desc="The GPIO pin for the blue led")
    public GpioEnum blueLedPin = GpioEnum.PIN_05;
}
