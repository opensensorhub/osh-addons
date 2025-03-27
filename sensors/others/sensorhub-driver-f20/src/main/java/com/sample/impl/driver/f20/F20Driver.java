/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.driver.f20;

import com.google.gson.Gson;
import com.sample.impl.driver.f20.rain.RainOutput;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author your_name
 * @since date
 */
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

        // TODO: Perform other initialization
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != output) {
            // Allocate necessary resources and start outputs
            try {
                mqttClient.subscribe(config.topicId, ((topic, message) -> output.handleMessage(topic, message)));
            } catch (MqttException e) {
                throw new SensorHubException("Error subscribing to topic " + config.topicId + " : " + e.getMessage(), e);
            }
        }

        // TODO: Perform other startup procedures
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != output) {

            output.doStop();
        }

        // TODO: Perform other shutdown procedures
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }
}
