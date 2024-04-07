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

import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.PoseStamped;
import geometry_msgs.Quaternion;
import net.opengis.swe.v20.*;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.command.*;
import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.pubsub.RosPublisherNode;
import org.sensorhub.impl.ros.utils.RosUtils;
import org.sensorhub.impl.sensor.spot.SpotSensor;
import org.sensorhub.impl.sensor.spot.config.SpotPoseConfig;
import org.sensorhub.impl.sensor.spot.control.svc_clients.PoseSvcClient;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import spot_msgs.PosedStandRequest;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Exposes controls for commanding a pose of the platform
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class SpotPoseControl extends BaseSpotControl {

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "SpotPoseControl";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "SPOT Pose Controls";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with SPOT ROS services, actions, and nodes to effectuate control over the platform";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/pose_control";

    /**
     * Enumerated list of commands allowed by this control
     */
    private enum PoseCommands {

        POSED_STAND,
        IN_MOTION_OR_IDLE_POSE,
        GO_TO_POSE
    }

    /**
     * The ROS executor process that manages the lifecycle of ROS nodes and services
     */
    private NodeMainExecutor nodeMainExecutor;

    /**
     * The service client used to command setting a pose
     */
    private PoseSvcClient serviceClient;

    /**
     * A ROS publisher node to command a pose that is timestamped
     */
    private RosPublisherNode<PoseStamped> goToPosePublisher;

    /**
     * A ROS publisher node to command a pose that can be achieved while in motion or idle
     */
    private RosPublisherNode<Pose> inMotionOrIdlePosePublisher;

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
    public SpotPoseControl(SpotSensor spotSensor) {

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
                .definition(SWEHelper.getPropertyUri("pose_controls"))
                .addField("poseCommand", factory.createChoice()
                        .label("Pose Command")
                        .description("Commands to control the pose of the platform")
                        .definition(SWEHelper.getPropertyUri("pose_command"))
                        .addItem(PoseCommands.POSED_STAND.name(), factory.createRecord()
                                .label("Posed Stand")
                                .description("Roll/pitch/yaw specification and body height in metres")
                                .definition(SWEHelper.getPropertyUri("posed_stand"))
                                .addField("yprVector", factory.createEulerOrientationYPR("deg"))
                                .addField("height", factory.createQuantity()
                                        .label("Body Height")
                                        .description("Body height is based on displacement from the neutral position")
                                        .definition(SWEHelper.getPropertyUri("posed_stand"))
                                        .uom("m")
                                        .dataType(DataType.FLOAT))
                                .build())
                        .addItem(PoseCommands.IN_MOTION_OR_IDLE_POSE.name(), factory.createRecord()
                                .label("In Motion or Idle Pose")
                                .description("A pose the robot should hold while it is in motion or idle")
                                .definition(SWEHelper.getPropertyUri("in_motion_or_idle_pose"))
                                .addField("position", factory.createLocationVectorXYZ("m"))
                                .addField("orientation", factory.createQuatOrientation())
                                .build())
                        .addItem(PoseCommands.GO_TO_POSE.name(), factory.createRecord()
                                .label("Go To Pose")
                                .description("Moves the robot by specifying a pose")
                                .definition(SWEHelper.getPropertyUri("go_to_pose"))
                                .addField("position", factory.createLocationVectorXYZ("m"))
                                .addField("orientation", factory.createQuatOrientation())
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

        SpotPoseConfig config = parentSensor.getConfiguration().spotPoseConfig;

        serviceClient = new PoseSvcClient(this, NODE_NAME_STR + "/static_body_pose_service_client", config.staticBodyPoseService);

        goToPosePublisher = new RosPublisherNode<>(NODE_NAME_STR + "/go_to_pose_publisher", config.goToPoseTopic, PoseStamped._TYPE);

        inMotionOrIdlePosePublisher = new RosPublisherNode<>(NODE_NAME_STR + "/in_motion_or_idle_body_pose_publisher", config.inMotionOrIdleBodyPoseTopic, Pose._TYPE);
    }

    /**
     * Starts the service client and publisher nodes via the ROS executor
     */
    @Override
    public void doStart() {

        RosMasterConfig config = parentSensor.getConfiguration().rosMaster;

        nodeMainExecutor.execute(serviceClient, RosUtils.getNodeConfiguration(
                config.localHostIp, serviceClient.getDefaultNodeName().toString(), URI.create(config.uri)));

        nodeMainExecutor.execute(goToPosePublisher, RosUtils.getNodeConfiguration(
                config.localHostIp, goToPosePublisher.getDefaultNodeName().toString(), URI.create(config.uri)));

        nodeMainExecutor.execute(inMotionOrIdlePosePublisher, RosUtils.getNodeConfiguration(
                config.localHostIp, inMotionOrIdlePosePublisher.getDefaultNodeName().toString(), URI.create(config.uri)));
    }

    /**
     * Stops the service clients, publisher nodes, and the ROS executor
     */
    @Override
    public void doStop() {

        nodeMainExecutor.shutdownNodeMain(goToPosePublisher);

        nodeMainExecutor.shutdownNodeMain(inMotionOrIdlePosePublisher);

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

        DataChoice bodyPoseCommand = (DataChoice) commandData.getField("poseCommand");

        DataComponent selectedAction = bodyPoseCommand.getSelectedItem();

        PoseCommands bodyPoseCommandValue = PoseCommands.valueOf(selectedAction.getName());

        if (bodyPoseCommandValue == PoseCommands.POSED_STAND) {

            PosedStandRequest request = serviceClient.getNewMessageBuffer();

            request.setBodyYaw(selectedAction.getData().getFloatValue(0));
            request.setBodyPitch(selectedAction.getData().getFloatValue(1));
            request.setBodyRoll(selectedAction.getData().getFloatValue(2));
            request.setBodyHeight(selectedAction.getData().getFloatValue(3));

            serviceClient.enqueueServiceRequest(request);

            getLogger().debug("Enqueued service request");

        } else if (bodyPoseCommandValue == PoseCommands.GO_TO_POSE) {

            PoseStamped poseStampedMessage = goToPosePublisher.getNewMessageBuffer();

            Pose pose = poseStampedMessage.getPose();

            Point position = pose.getPosition();
            position.setX(selectedAction.getData().getDoubleValue(0));
            position.setY(selectedAction.getData().getDoubleValue(1));
            position.setZ(selectedAction.getData().getDoubleValue(2));

            Quaternion orientation = pose.getOrientation();
            orientation.setX(selectedAction.getData().getDoubleValue(3));
            orientation.setY(selectedAction.getData().getDoubleValue(4));
            orientation.setZ(selectedAction.getData().getDoubleValue(5));
            orientation.setW(selectedAction.getData().getDoubleValue(6));

            pose.setPosition(position);
            pose.setOrientation(orientation);

            poseStampedMessage.setPose(pose);

            goToPosePublisher.publishMessage(poseStampedMessage);

        } else if (bodyPoseCommandValue == PoseCommands.IN_MOTION_OR_IDLE_POSE) {

            Pose poseMessage = inMotionOrIdlePosePublisher.getNewMessageBuffer();

            Point position = poseMessage.getPosition();
            position.setX(selectedAction.getData().getDoubleValue(0));
            position.setY(selectedAction.getData().getDoubleValue(1));
            position.setZ(selectedAction.getData().getDoubleValue(2));

            Quaternion orientation = poseMessage.getOrientation();
            orientation.setX(selectedAction.getData().getDoubleValue(3));
            orientation.setY(selectedAction.getData().getDoubleValue(4));
            orientation.setZ(selectedAction.getData().getDoubleValue(5));
            orientation.setW(selectedAction.getData().getDoubleValue(6));

            poseMessage.setPosition(position);
            poseMessage.setOrientation(orientation);

            inMotionOrIdlePosePublisher.publishMessage(poseMessage);

        } else {

            ICommandStatus status;
            status = CommandStatus.failed(currentCommandId, "Unknown Command: " + bodyPoseCommandValue);
            getEventHandler().publish(new CommandStatusEvent(this, 200L, status));

            commandExecuted = false;
        }

        return commandExecuted;
    }
}
