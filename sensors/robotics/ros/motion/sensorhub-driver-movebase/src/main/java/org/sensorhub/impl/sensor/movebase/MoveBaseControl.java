/*
 *  The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file, You can obtain one
 *  at http://mozilla.org/MPL/2.0/.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.sensor.movebase;

import actionlib_msgs.GoalID;
import actionlib_msgs.GoalStatus;
import actionlib_msgs.GoalStatusArray;
import com.github.ekumen.rosjava_actionlib.ActionClientListener;
import geometry_msgs.PoseStamped;
import move_base_msgs.MoveBaseActionFeedback;
import move_base_msgs.MoveBaseActionGoal;
import move_base_msgs.MoveBaseActionResult;
import move_base_msgs.MoveBaseFeedback;
import net.opengis.swe.v20.*;
import org.ros.message.Time;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.ros.nodes.action.RosActionClientNode;
import org.sensorhub.impl.ros.nodes.pubsub.RosPublisherNode;
import org.sensorhub.impl.ros.utils.RosUtils;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.data.DataChoiceImpl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.net.URI;
import java.util.List;

public class MoveBaseControl extends AbstractSensorControl<MoveBaseSensor> implements
        ActionClientListener<MoveBaseActionFeedback, MoveBaseActionResult> {

    private static final String SENSOR_CONTROL_NAME = "MoveBaseControl";

    private static final String SENSOR_CONTROL_LABEL = "MoveBaseControl";

    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Sends remote operation commands in the form of pose location goals to the robot to navigate to given location";

    private static final String NODE_NAME_STR = "/SensorHub/moveBase";

    private static final String ACTION_STR = "move_base";

    private static final String ORIGIN_TOPIC_STR = "local_xy_origin";

    private DataRecord commandDataStruct;

    private NodeMainExecutor nodeMainExecutor;

    private GoalID goalId = null;

    private RosActionClientNode<MoveBaseActionGoal, MoveBaseActionFeedback, MoveBaseActionResult> actionNode;

    private RosPublisherNode<PoseStamped> originPublisherNode;

    private boolean publishedOrigin = false;

    private int messageSequenceId = 0;

    private MoveBaseConfig config;

    public MoveBaseControl(MoveBaseSensor parentNavGoalSensor) {

        super(SENSOR_CONTROL_NAME, parentNavGoalSensor);
    }

    private void defineRecordStructure() {

        GeoPosHelper factory = new GeoPosHelper();

        Vector location = factory.createLocationVectorXYZ("m")
                .name("location")
                .updatable(true)
                .label("Location")
                .description("The target location to travel to")
                .build();

        Quantity orientation = factory.createQuantity()
                .name("orientation")
                .updatable(true)
                .label("Orientation")
                .definition(SWEHelper.getPropertyUri("Orientation"))
                .description("The orientation of the platform with respect to north")
                .dataType(DataType.DOUBLE)
                .addAllowedInterval(0.0, 360.0)
                .uom("deg")
                .build();

        DataRecord goalCommand = factory.createRecord()
                .name("Goal")
                .updatable(true)
                .label("Command Goal")
                .description("A position for the platform to move to")
                .definition(SWEHelper.getPropertyUri("CommandGoal"))
                .addField(location.getName(), location)
                .addField(orientation.getName(), orientation)
                .build();

        DataRecord cancelCommand = factory.createRecord()
                .name("Cancel")
                .updatable(true)
                .label("Cancel Command ")
                .description("Command to cancel current goal command")
                .definition(SWEHelper.getPropertyUri("CancelCommand"))
                .build();

        DataChoice command = factory.createChoice()
                .name("Command")
                .label("Command")
                .description("Commands accepted by the platform")
                .definition(SWEHelper.getPropertyUri("Command"))
                .addItem(goalCommand.getName(), goalCommand)
                .addItem(cancelCommand.getName(), cancelCommand)
                .build();

        commandDataStruct = factory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("NavGoalControls"))
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .addField(command.getName(), command)
                .build();
    }

    public void doInit() {

        config = parentSensor.getConfiguration();

        publishedOrigin = false;

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        actionNode = new RosActionClientNode<>(NODE_NAME_STR, ACTION_STR, MoveBaseActionGoal._TYPE,
                MoveBaseActionFeedback._TYPE, MoveBaseActionResult._TYPE, this);

        originPublisherNode = new RosPublisherNode<>(NODE_NAME_STR, ORIGIN_TOPIC_STR, PoseStamped._TYPE);
    }

    public void doStart() {

        NodeConfiguration nodeConfiguration = RosUtils.getNodeConfiguration(
                config.rosConfig.localHostIp, NODE_NAME_STR, URI.create(config.rosConfig.uri));

        nodeMainExecutor.execute(actionNode, nodeConfiguration);

        NodeConfiguration poseNodeConfiguration = RosUtils.getNodeConfiguration(
                config.rosConfig.localHostIp, NODE_NAME_STR + "/pose", URI.create(config.rosConfig.uri));

        nodeMainExecutor.execute(originPublisherNode, poseNodeConfiguration);
    }

    public void doStop() {

        if (goalId != null) {

            actionNode.cancelGoal(goalId);
        }

        publishedOrigin = false;

        nodeMainExecutor.shutdownNodeMain(actionNode);

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

        DataComponent component = ((DataChoiceImpl) commandData.getField("Command")).getSelectedItem();

        String commandId = component.getName();

        if (commandId.equalsIgnoreCase("Goal")) {

            try {

                if (!publishedOrigin) {

                    getLogger().debug("Publishing initial pose...");

                    PoseStamped originMessage = originPublisherNode.getNewMessageBuffer();

                    originMessage.getHeader().setFrameId(config.rosConfig.frameId);
                    originMessage.getHeader().setStamp(Time.fromMillis(System.currentTimeMillis()));
                    originMessage.getPose().getPosition().setX(config.rosConfig.xOrigin);
                    originMessage.getPose().getPosition().setY(config.rosConfig.yOrigin);

                    originPublisherNode.publishMessage(originMessage);

                    getLogger().debug("Published initial pose!");

                    publishedOrigin = true;
                }

                Vector location = (Vector) component.getComponent("location");

                double y = location.getCoordinate("y").getData().getDoubleValue();

                double x = location.getCoordinate("x").getData().getDoubleValue();

                Quantity orientation = (Quantity) component.getComponent("orientation");

                double orientationAngle = orientation.getValue();

                MoveBaseActionGoal message = actionNode.getNewMessageBuffer();

                message.getHeader().setStamp(Time.fromMillis(System.currentTimeMillis()));
                message.getHeader().setFrameId(config.rosConfig.frameId);
                message.getHeader().setSeq(messageSequenceId);

                message.getGoal().getTargetPose().getHeader().setStamp(Time.fromMillis(System.currentTimeMillis()));
                message.getGoal().getTargetPose().getHeader().setFrameId(config.rosConfig.frameId);
                message.getGoal().getTargetPose().getHeader().setSeq(messageSequenceId++);
                message.getGoal().getTargetPose().getPose().getPosition().setX(x);
                message.getGoal().getTargetPose().getPose().getPosition().setY(y);
                message.getGoal().getTargetPose().getPose().getPosition().setZ(0);
                message.getGoal().getTargetPose().getPose().getOrientation().setX(0);
                message.getGoal().getTargetPose().getPose().getOrientation().setY(0);
                message.getGoal().getTargetPose().getPose().getOrientation().setZ(Math.toRadians(orientationAngle));
                message.getGoal().getTargetPose().getPose().getOrientation().setW(1);

                goalId = actionNode.publishGoal(message);

            } catch (Exception e) {

                throw new CommandException("Failed to handle command", e);
            }

        } else if (commandId.equalsIgnoreCase("Cancel")) {

            actionNode.cancelGoal(goalId);

        } else {

            commandExecuted = false;
        }

        return commandExecuted;
    }

    @Override
    public void resultReceived(MoveBaseActionResult moveBaseActionResult) {

        getLogger().debug("Goal Id: {} | Goal status: {}",
                moveBaseActionResult.getStatus().getGoalId().getId(),
                moveBaseActionResult.getStatus().getText());
    }

    @Override
    public void feedbackReceived(MoveBaseActionFeedback moveBaseActionFeedback) {

        MoveBaseFeedback moveBaseFeedback = moveBaseActionFeedback.getFeedback();

        getLogger().debug("Position: ({}, {}, {}) | Orientation: ({}, {}, {}, {})",
                moveBaseFeedback.getBasePosition().getPose().getPosition().getX(),
                moveBaseFeedback.getBasePosition().getPose().getPosition().getY(),
                moveBaseFeedback.getBasePosition().getPose().getPosition().getZ(),
                moveBaseFeedback.getBasePosition().getPose().getOrientation().getX(),
                moveBaseFeedback.getBasePosition().getPose().getOrientation().getY(),
                moveBaseFeedback.getBasePosition().getPose().getOrientation().getZ(),
                moveBaseFeedback.getBasePosition().getPose().getOrientation().getW());
    }

    @Override
    public void statusReceived(GoalStatusArray status) {

        final List<GoalStatus> goalStatusList = status.getStatusList();

        for (GoalStatus goalStatus : goalStatusList) {

            getLogger().debug("Goal Id: {} | Goal status: {}",
                    goalStatus.getGoalId().getId(), goalStatus.getText());
        }
    }
}
