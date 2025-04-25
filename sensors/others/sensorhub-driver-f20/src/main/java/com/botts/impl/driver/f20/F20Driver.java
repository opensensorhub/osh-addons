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

import com.google.gson.Gson;
import com.botts.impl.driver.f20.rain.RainOutput;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class F20Driver extends AbstractSensorModule<F20Config> {

    private static final Logger logger = LoggerFactory.getLogger(F20Driver.class);

    RainOutput output;
    MqttClient mqttClient;
    public Gson gson = new Gson();

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:driver:f20:", config.serialNumber);
        generateXmlID("F20", config.serialNumber);

        // Create and initialize output
        output = new RainOutput(this);

        addOutput(output, false);

        output.doInit();

        try {
            // Create MQTT client and connect to MQTT broker specified by config
            mqttClient = new MqttClient(
                    config.broker,
                    getUniqueIdentifier(),
                    new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(config.username);
            options.setPassword(config.password.toCharArray());
            mqttClient.connect(options);
            reportStatus("Successfully connected to MQTT broker");
        } catch (MqttException e) {
            throw new SensorHubException("Error creating MQTT connection: " + e.getMessage(), e);
        }
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != output) {
            try {
                // Begin subscription to MQTT topic ID, and let our output class handle the MQTT data stream
                mqttClient.subscribe(config.topicId, ((topic, message) -> output.handleMessage(topic, message)));
            } catch (MqttException e) {
                throw new SensorHubException("Error subscribing to topic " + config.topicId + " : " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void doStop() throws SensorHubException {
        try {
            // Unsubscribe from MQTT topic
            mqttClient.unsubscribe(config.topicId);
        } catch (MqttException e) {
            throw new SensorHubException("", e);
        }
    }

    // Use MQTT client connection status as connection status of this driver
    @Override
    public boolean isConnected() {
        return mqttClient.isConnected();
    }
}
