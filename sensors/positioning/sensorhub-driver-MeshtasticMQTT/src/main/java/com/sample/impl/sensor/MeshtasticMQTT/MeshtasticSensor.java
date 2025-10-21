/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.MeshtasticMQTT;

import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ogc.om.MovingFeature;

import java.util.concurrent.TimeUnit;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class MeshtasticSensor extends AbstractSensorModule<Config> {
    static final String UID_PREFIX = "urn:osh:Meshtastic_Node:";
    static final String XML_PREFIX = "MeshtasticMQTT";

    private static final Logger logger = LoggerFactory.getLogger(MeshtasticSensor.class);

    // DEFINE MQTT HANDLER
    MeshtasticMqttHandler mqttConnector;

    // DEFINE OUTPUTS
    MeshtasticOutputTextMessage text_output;
    MeshtasticOutputPosition pos_output;
    MeshtasticOutputNodeInfo node_output;

    Thread processingThread;

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Generate identifiers
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // CREATE AND INITIALIZE OUTPUTS
        text_output = new MeshtasticOutputTextMessage(this);
        addOutput(text_output, false);
        text_output.doInit();

        pos_output = new MeshtasticOutputPosition(this);
        addOutput(pos_output, false);
        pos_output.doInit();

        node_output = new MeshtasticOutputNodeInfo(this);
        addOutput(node_output, false);
        node_output.doInit();

    }

    @Override
    public void doStart() throws SensorHubException {
        super.doStart();

        // Register Meshtastic MQTT handler to begin handling messages from MQTT

        getParentHub().getModuleRegistry().waitForModuleType(IMqttServer.class, ModuleEvent.ModuleState.STARTED)
                .thenAccept(mqtt -> {
                    if (mqtt != null)
                    {
                        // Pass the Meshtastic Outputs to the Handler to set appropriate data
                        mqttConnector = new MeshtasticMqttHandler(
                                text_output,
                                pos_output,
                                node_output
                        );
                        mqtt.registerHandler("", mqttConnector);
                        getLogger().info("MESHTASTIC MQTT handler registered");
                        setState(ModuleEvent.ModuleState.STARTED);
                    }
                })
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    reportError("No MQTT server available", e);
                    return null;
                });

    }

    @Override
    public void doStop() throws SensorHubException {
        super.doStop();
    }

    @Override
    public boolean isConnected() {
        return processingThread != null && processingThread.isAlive();
    }

    String addFoi( String mesh_node_id)
    {
        String foiUID = UID_PREFIX + mesh_node_id;

        if (!foiMap.containsKey(foiUID))
        {
            // generate small SensorML for FOI (in this case the system is the FOI)
            MovingFeature foi = new MovingFeature();
            foi.setId(mesh_node_id);
            foi.setUniqueIdentifier(foiUID);
            foi.setName(mesh_node_id);
            foi.setDescription("Meshtastic Node " + mesh_node_id);

            // register it
            addFoi(foi);

            getLogger().debug("New Meshtastic Node added as FOI: {}", foiUID);
        }

        return foiUID;
    }


}
