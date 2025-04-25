/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.driver.f20;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

public class F20Config extends SensorConfig {

    /**
     * The unique identifier for the configured sensor (or sensor platform).
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "001";

    /**
     * MQTT broker to communicate with F20 device
     */
    @DisplayInfo.Required
    @DisplayInfo(label = "MQTT Broker")
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.REMOTE_ADDRESS)
    public String broker = "tcp://mqtt.broker:1883";

    /**
     * Topic ID of F20 data stream
     */
    @DisplayInfo.Required
    @DisplayInfo(desc = "MQTT Topic ID")
    public String topicId;

    /**
     * Username to authenticate with MQTT broker
     */
    @DisplayInfo(desc = "Username")
    public String username;

    /**
     * Password to authenticate with MQTT broker
     */
    @DisplayInfo(desc = "Password")
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.PASSWORD)
    public String password;

}
