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

import geometry_msgs.Twist;
import geometry_msgs.Vector3;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.command.*;
import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.pubsub.RosPublisherNode;
import org.sensorhub.impl.ros.utils.RosUtils;
import org.sensorhub.impl.sensor.spot.SpotSensor;
import org.sensorhub.impl.sensor.spot.config.SpotMotionConfig;
import org.sensorhub.impl.sensor.spot.control.svc_clients.VelocitySvcClient;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import spot_msgs.SetVelocityRequest;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Exposes controls for motion of the platform
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class SpotMotionControl extends BaseSpotControl {

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "SpotMotionControl";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "SPOT Motion Controls";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with SPOT ROS services, actions, and nodes to effectuate control over the platform";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/motion_control";

    /**
     * Handles the sending the velocity command at the configured command rate
     */
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * Enumerated list of commands allowed by this control
     */
    private enum MotionCommands {

        STOP,
        VELOCITY,
        VELOCITY_LIMIT
    }

    /**
     * The ROS executor process that manages the lifecycle of ROS nodes and services
     */
    private NodeMainExecutor nodeMainExecutor;

    /**
     * The service client used to command setting a velocity
     */
    private VelocitySvcClient serviceClient;

    /**
     * A ROS publisher node to command a linear and angular velocity of motion for the platform
     */
    private RosPublisherNode<Twist> velocityPublisher;

    /**
     * Handle to the current command being executed
     */
    private ICommandData currentCommand;

    /**
     * Data structure holding description of allowed commands and used to parse the received command
     */
    private DataRecord commandDataStruct;

    /**
     * Constructor
     *
     * @param spotSensor The parent sensor module
     */
    public SpotMotionControl(SpotSensor spotSensor) {

        super(SENSOR_CONTROL_NAME, spotSensor);
    }

    /**
     * Builds the data structure
     */
    private void defineRecordStructure() {

        GeoPosHelper factory = new GeoPosHelper();

        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("motion_controls"))
                .addField("motionCommand", factory.createChoice()
                        .label("Motion Command")
                        .description("Commands to control the velocity of motion for the platform")
                        .definition(SWEHelper.getPropertyUri("motion_command"))
                        .addItem(MotionCommands.STOP.name(), factory.createRecord()
                                .label("Stop")
                                .description("Stops the movement of the platform")
                                .definition(SWEHelper.getPropertyUri("stop"))
                                .build())
                        .addItem(MotionCommands.VELOCITY.name(), factory.createRecord()
                                .label("Velocity")
                                .description("The velocity of the platform")
                                .definition(SWEHelper.getPropertyUri("set_velocity"))
                                .addField("velocity", factory.createVelocityVector("m/s"))
                                .addField("angularVelocity", factory.createAngularVelocityVector("rad/s"))
                                .build())
                        .addItem(MotionCommands.VELOCITY_LIMIT.name(), factory.createRecord()
                                .label("Velocity Limit")
                                .description("The limits of velocity on the platform, this command will set the new upper limits on the velocity of motion")
                                .definition(SWEHelper.getPropertyUri("velocity_limit"))
                                .addField("velocity", factory.createVelocityVector("m/s"))
                                .addField("angularVelocity", factory.createAngularVelocityVector("rad/s"))
                                .build())
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

        SpotMotionConfig config = parentSensor.getConfiguration().spotMotionConfig;

        serviceClient = new VelocitySvcClient(this, NODE_NAME_STR + "/velocity_limit_service_client", config.velocityLimitService);

        velocityPublisher = new RosPublisherNode<>(NODE_NAME_STR + "/command_velocity_publisher", config.commandVelTopic, Twist._TYPE);
    }

    /**
     * Starts the service client and publisher node via the ROS executor
     */
    @Override
    public void doStart() {

        RosMasterConfig config = parentSensor.getConfiguration().rosMaster;

        nodeMainExecutor.execute(serviceClient, RosUtils.getNodeConfiguration(
                config.localHostIp, serviceClient.getDefaultNodeName().toString(), URI.create(config.uri)));

        nodeMainExecutor.execute(velocityPublisher, RosUtils.getNodeConfiguration(
                config.localHostIp, velocityPublisher.getDefaultNodeName().toString(), URI.create(config.uri)));
    }

    /**
     * Stops the service clients, publisher node, and the ROS executor
     */
    @Override
    public void doStop() {

        nodeMainExecutor.shutdownNodeMain(velocityPublisher);

        nodeMainExecutor.shutdownNodeMain(serviceClient);

        nodeMainExecutor.shutdown();
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

     @Override
    public boolean execCommand(DataBlock command) throws CommandException {

        boolean commandExecuted = true;

        DataRecord commandData = commandDataStruct.copy();

        commandData.setData(command);

        DataChoice motionCommand = (DataChoice) commandData.getField("motionCommand");

        DataComponent selectedAction = motionCommand.getSelectedItem();

        MotionCommands motionCommandValue = MotionCommands.valueOf(selectedAction.getName());

        getLogger().debug("Received Command - {}", motionCommandValue);

        if (motionCommandValue == MotionCommands.STOP && scheduledExecutorService != null) {

            scheduledExecutorService.shutdown();

            scheduledExecutorService = null;

            ICommandStatus status;

            status = CommandStatus.completed(currentCommandId);

            getEventHandler().publish(new CommandStatusEvent(this, 200L, status));

        } else if (motionCommandValue == MotionCommands.VELOCITY) {

            if (scheduledExecutorService == null) {

                scheduledExecutorService = Executors.newScheduledThreadPool(1);

                scheduledExecutorService.schedule(() -> {

                    Twist twist = velocityPublisher.getNewMessageBuffer();

                    populateTwistMessage(twist, selectedAction);

                    velocityPublisher.publishMessage(twist);

                    ICommandStatus status;

                    status = CommandStatus.completed(currentCommandId);

                    getEventHandler().publish(new CommandStatusEvent(this, 200L, status));

                }, 1 / parentSensor.getConfiguration().spotMotionConfig.commandRate, TimeUnit.MILLISECONDS);
            }

        } else if ((motionCommandValue == MotionCommands.VELOCITY_LIMIT) && serviceClient.isConnected()) {

            SetVelocityRequest request = serviceClient.getNewMessageBuffer();

            Twist twist = request.getVelocityLimit();

            populateTwistMessage(twist, selectedAction);

            request.setVelocityLimit(twist);

            serviceClient.enqueueServiceRequest(request);

            getLogger().debug("Enqueued service request");

        } else {

            ICommandStatus status;
            status = CommandStatus.failed(currentCommandId, "Unknown Command: " + motionCommandValue);
            getEventHandler().publish(new CommandStatusEvent(this, 200L, status));

            commandExecuted = false;
        }

        return commandExecuted;
    }

    private void populateTwistMessage(Twist twist, final DataComponent selectedAction) {

        Vector3 linearVelocityVector = twist.getLinear();
        linearVelocityVector.setX(selectedAction.getData().getDoubleValue(0));
        linearVelocityVector.setY(selectedAction.getData().getDoubleValue(1));
        linearVelocityVector.setZ(selectedAction.getData().getDoubleValue(2));
        twist.setLinear(linearVelocityVector);

        Vector3 angularVelocityVector = twist.getAngular();
        angularVelocityVector.setX(selectedAction.getData().getDoubleValue(3));
        angularVelocityVector.setY(selectedAction.getData().getDoubleValue(4));
        angularVelocityVector.setZ(selectedAction.getData().getDoubleValue(5));
        twist.setAngular(angularVelocityVector);
    }
}
