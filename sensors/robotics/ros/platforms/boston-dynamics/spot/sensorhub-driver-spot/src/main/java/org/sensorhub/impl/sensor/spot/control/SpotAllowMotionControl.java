/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 - 2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.spot.control;

import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.command.*;
import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.service.RosServiceClient;
import org.sensorhub.impl.ros.utils.RosUtils;
import org.sensorhub.impl.sensor.spot.SpotSensor;
import org.sensorhub.impl.sensor.spot.config.SpotAllowMotionConfig;
import org.sensorhub.impl.sensor.spot.control.svc_clients.OnOffSettingsSvcClient;
import org.vast.swe.SWEHelper;

import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Exposes controls for claiming leases and maintaining control over the platform
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class SpotAllowMotionControl extends BaseSpotControl {

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "SpotAllowMotionControl";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "SPOT Allow Motion Controls";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with SPOT ROS services, actions, and nodes to effectuate control over the platform";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/allow_motion_controls";

    /**
     * ROS Node name assigned at creation
     */
    private static final String SERVICE_CLIENT_STR = "_service_client";

    /**
     * Enumerated list of commands allowed by this control
     */
    private enum SettingsCommands {

        NONE, ALLOW_MOTION
    }

    /**
     * The ROS executor process that manages the lifecycle of ROS nodes and services
     */
    private NodeMainExecutor nodeMainExecutor;

    /**
     * A map of the commands to the associated service client used to process them
     */
    private final Map<SettingsCommands, OnOffSettingsSvcClient> serviceClients = new EnumMap<>(SettingsCommands.class);

    /**
     * Data structure holding description of allowed commands and used to parse the received command
     */
    private DataRecord commandDataStruct;

    /**
     * Constructor
     *
     * @param spotSensor The parent sensor module
     */
    public SpotAllowMotionControl(SpotSensor spotSensor) {

        super(SENSOR_CONTROL_NAME, spotSensor);
    }

    /**
     * Builds the data structure
     */
    private void defineRecordStructure() {

        SWEHelper factory = new SWEHelper();

        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("allow_motion_controls"))
                .addField("allowMotionCommand", factory.createCategory()
                        .label("Allow Motion Command")
                        .description("Commands to apply specific settings the platform")
                        .definition(SWEHelper.getPropertyUri("allow_motion_command"))
                        .value(SettingsCommands.NONE.name())
                        .addAllowedValues(
                                SettingsCommands.NONE.name(),
                                SettingsCommands.ALLOW_MOTION.name())
                        .build())
                .build();
    }

    /**
     * Initializes the control, setting up the ROS nodes, the executor that will manage
     * their lifecycle
     */
    @Override
    public void doInit() {

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        SpotAllowMotionConfig config = parentSensor.getConfiguration().spotAllowMotionConfig;

        serviceClients.put(SettingsCommands.ALLOW_MOTION, new OnOffSettingsSvcClient(this,
                NODE_NAME_STR + "/" + SettingsCommands.ALLOW_MOTION + SERVICE_CLIENT_STR,
                config.allowMotionService));
    }

    /**
     * Starts the service clients via the ROS executor
     */
    @Override
    public void doStart() {

        RosMasterConfig config = parentSensor.getConfiguration().rosMaster;

        for (RosServiceClient<?, ?> client : serviceClients.values()) {

            NodeConfiguration nodeConfiguration = RosUtils.getNodeConfiguration(
                    config.localHostIp, client.getDefaultNodeName().toString(), URI.create(config.uri));

            nodeMainExecutor.execute(client, nodeConfiguration);
        }
    }

    /**
     * Stops the service clients and the ROS executor
     */
    @Override
    public void doStop() {


        for (RosServiceClient<?, ?> client : serviceClients.values()) {

            nodeMainExecutor.shutdownNodeMain(client);
        }

        nodeMainExecutor.shutdown();
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    @Override
    public boolean execCommand(DataBlock command) throws CommandException {

        boolean commandExecuted = false;

        DataRecord commandData = commandDataStruct.copy();

        commandData.setData(command);

        Category theCommand = (Category) commandData.getField("allowMotionCommand");

        SettingsCommands theCommandValue = SettingsCommands.valueOf(theCommand.getValue());

        OnOffSettingsSvcClient client = serviceClients.get(theCommandValue);

        if ((client != null) && (client.isConnected())) {

            client.enqueueServiceRequest(client.getNewMessageBuffer());
            commandExecuted = true;

        } else {

            ICommandStatus status;
            status = CommandStatus.failed(currentCommandId, "Service client not connected to target ROS service");
            getEventHandler().publish(new CommandStatusEvent(this, 200L, status));
        }

        return commandExecuted;
    }
}