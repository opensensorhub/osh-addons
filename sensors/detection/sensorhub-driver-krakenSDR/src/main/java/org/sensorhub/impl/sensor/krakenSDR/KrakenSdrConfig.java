/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.krakenSDR;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration settings for the {@link OkrakenSDRSensor} driver exposed via the OpenSensorHub Admin panel.
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
public class KrakenSdrConfig extends SensorConfig {

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber;

    @DisplayInfo.Required
    @DisplayInfo(label="Kraken IP Address", desc="Provide the shared IP Address assigned to the KrakenSD")
    public String krakenIPaddress = "192.168.50.186";

    @DisplayInfo.Required
    @DisplayInfo(label="Kraken Data-out Port", desc="Provide the USB Port for your Sensor (Usually 8081)")
    public String krakenPort = "8081";

    @DisplayInfo.Required
    @DisplayInfo(label="Sample Rate (seconds)", desc="Provide the USB Port for your Sensor (Usually 8081")
    public int sampelRate = 2;


}
