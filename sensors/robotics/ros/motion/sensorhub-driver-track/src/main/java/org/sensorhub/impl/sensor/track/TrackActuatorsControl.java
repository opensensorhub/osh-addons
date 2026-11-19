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
package org.sensorhub.impl.sensor.track;

import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.nodes.pubsub.RosPublisherNode;
import org.sensorhub.impl.ros.utils.RosUtils;
import geometry_msgs.Twist;
import net.opengis.swe.v20.*;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;

import java.net.URI;

public class TrackActuatorsControl extends AbstractSensorControl<TrackControlSensor> {

    private static final String SENSOR_CONTROL_NAME = "TrackControl";

    private static final String SENSOR_CONTROL_LABEL = "TrackControl";

    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Sends remote operation commands to the robot directly controlling the track drive system";

    private static final String NODE_NAME_STR = "/SensorHub/trackControl";

    private static final String TOPIC_STR = "/cmd_vel";

    private DataRecord commandDataStruct;

    private NodeMainExecutor nodeMainExecutor;

    private RosPublisherNode<Twist> publisherNode;


    public TrackActuatorsControl(TrackControlSensor parentTrackControlSensor) {

        super(SENSOR_CONTROL_NAME, parentTrackControlSensor);
    }

    private void defineRecordStructure() {

        SWEHelper factory = new SWEHelper();

        Quantity linearVelocity = factory.createQuantity()
                .name("linearVelocity")
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("LinearVelocity"))
                .label("Linear Velocity")
                .description("The linear velocity in m/s, " +
                        "+ values drive forward, - values drive backward, " +
                        ", and valid values between [-1.0, 1.0]")
                .addAllowedInterval(-1.0, 1.0)
                .uom("m/s")
                .build();

        Quantity angularVelocity = factory.createQuantity()
                .name("angularVelocity")
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("AngularVelocity"))
                .label("Angular Velocity")
                .description("The angular velocity - values turn right, + values turn left, " +
                        "and valid values between [-2.0, 2.0]")
                .addAllowedInterval(-2.0, 2.0)
                .uom("rad/s")
                .build();

        commandDataStruct = factory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("TrackControls"))
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .addField(linearVelocity.getName(), linearVelocity)
                .addField(angularVelocity.getName(), angularVelocity)
                .build();
    }

    public void doInit() {

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        publisherNode = new RosPublisherNode<>(NODE_NAME_STR, TOPIC_STR, Twist._TYPE);
    }

    public void doStart() {

        RosMasterConfig config = parentSensor.getConfiguration().rosMaster;

        NodeConfiguration nodeConfiguration = RosUtils.getNodeConfiguration(
                config.localHostIp, NODE_NAME_STR, URI.create(config.uri));

        nodeMainExecutor.execute(publisherNode, nodeConfiguration);
    }

    public void doStop() {

        nodeMainExecutor.shutdownNodeMain(publisherNode);

        nodeMainExecutor.shutdown();
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    @Override
    public boolean execCommand(DataBlock command) throws CommandException {

        DataRecord commandData = commandDataStruct.copy();

        commandData.setData(command);

        Quantity linearVelocity = (Quantity) commandData.getField("linearVelocity");

        double linearVelocityValue = linearVelocity.getValue() / 100.0;

        if (linearVelocityValue > 1.0) {

            linearVelocityValue = 1.0;

        } else if (linearVelocityValue < -1.0) {

            linearVelocityValue = - 1.0;
        }

        Quantity angularVelocity = (Quantity) commandData.getField("angularVelocity");

        double angularVelocityValue = angularVelocity.getValue();

        if (angularVelocityValue > 2.0) {

            angularVelocityValue = 2.0;

        } else if (angularVelocityValue < -2.0) {

            angularVelocityValue = -2.0;
        }

        try {

            Twist message = publisherNode.getNewMessageBuffer();

            message.getLinear().setX(linearVelocityValue);
            message.getLinear().setY(0.0);
            message.getLinear().setZ(0.0);

            message.getAngular().setX(0.0);
            message.getAngular().setY(0.0);
            message.getAngular().setZ(angularVelocityValue);

            publisherNode.publishMessage(message);

        } catch (Exception e) {

            throw new CommandException("Failed to handle command", e);
        }
        return true;
    }
}
