/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.OPS241A.config;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.UARTConfig;
import org.sensorhub.impl.sensor.OPS241A.OPS241ASensor;

/**
 * Configuration settings for the {@link OPS241ASensor} driver exposed via the OpenSensorHub Admin panel.
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
public class OPS241AConfig extends SensorConfig {

public enum Units {
    METERS_PER_SECOND("m/s"),
    CENTIMETERS_PER_SECOND("cm/s"),
    FEET_PER_SECOND("ft/s"),
    KILOMETERS_PER_HOUR("kph"),
    MILES_PER_HOUR("mph");

    private final String label;

    Units(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber;

//    @DisplayInfo(desc="Communication settings to connect to IMU data stream")
//    public CommProviderConfig<?> commSettings;

    @DisplayInfo(desc="My RxTx Settings")
    public rxtxConfig RxTxSettings = new rxtxConfig();

    @DisplayInfo(label="Measurement Units", desc="Select the units you want to output")
    public Units unit = Units.MILES_PER_HOUR;






}
