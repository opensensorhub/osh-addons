/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.notecardGPS.config;

import org.sensorhub.impl.sensor.notecardGPS.notecardGPSSensor;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.FieldType.Type;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration settings for the {@link notecardGPSSensor} driver exposed via the OpenSensorHub Admin panel.
 * <p>
 * Public fields are exposed in the Admin panel for configuration by the user.
 * These fields can be annotated with the DisplayInfo annotation to provide additional information to the user
 * or to restrict the values that can be entered.
 * <p>
 * Configuration takes the form of:
 * <pre>{@code
 * @DisplayInfo(label = "Field Label", desc = "A description of the field to be shown in the UI.")
 * public Type configOption = "default value";
 * }</pre>
 */
public class Config extends SensorConfig {
    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber;

    @DisplayInfo.Required
    @DisplayInfo(label = "Notecard Settings", desc = "Configuration options for the Notecard")
    public noteCardConfig NCconfig = new noteCardConfig();

    @DisplayInfo.Required
    @DisplayInfo(label = "I²C Settings", desc = "Configuration options for the I2C Connection")
    public i2cConfig connection = new i2cConfig();




}
