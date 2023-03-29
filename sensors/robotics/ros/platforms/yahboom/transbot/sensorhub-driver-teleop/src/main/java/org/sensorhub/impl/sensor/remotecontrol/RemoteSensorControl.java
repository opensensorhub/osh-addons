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
package org.sensorhub.impl.sensor.remotecontrol;

import org.sensorhub.impl.sensor.remotecontrol.controls.analog.LeftAnalogControl;
import org.sensorhub.impl.sensor.remotecontrol.controls.analog.RightAnalogControl;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.action.A;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.action.B;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.action.X;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.action.Y;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.analog.LeftAnalog;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.analog.RightAnalog;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.misc.Select;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.misc.Start;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.trigger.L1;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.trigger.L2;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.trigger.R1;
import org.sensorhub.impl.sensor.remotecontrol.controls.buttons.trigger.R2;
import org.sensorhub.impl.ros.config.RosMasterConfig;
import org.sensorhub.impl.ros.controller.analog.AnalogControlManager;
import org.sensorhub.impl.ros.controller.analog.ControlPlacement;
import org.sensorhub.impl.ros.controller.analog.Direction;
import org.sensorhub.impl.ros.controller.buttons.ButtonManager;
import org.sensorhub.impl.ros.controller.buttons.ButtonState;
import org.sensorhub.impl.ros.controller.buttons.DefaultButton;
import org.sensorhub.impl.ros.nodes.pubsub.RosPublisherNode;
import org.sensorhub.impl.ros.utils.RosUtils;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.ros.message.Time;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;
import sensor_msgs.Joy;

import java.net.URI;

public class RemoteSensorControl extends AbstractSensorControl<RemoteControlSensor> {

    private static final String SENSOR_CONTROL_NAME = "RemoteControl";

    private static final String SENSOR_CONTROL_LABEL = "RemoteControl";

    private static final String SENSOR_CONTROL_DESCRIPTION = "Sends remote operation commands to the robot";

    private static final String NODE_NAME_STR = "/SensorHub/remoteControl";

    private static final String TOPIC_STR = "/joy";

    private DataRecord commandDataStruct;

    private NodeMainExecutor nodeMainExecutor;

    private RosPublisherNode<Joy> publisherNode;

    private int messageSequenceId = 0;

    private final AnalogControlManager analogControlManager = new AnalogControlManager(8);

    private final ButtonManager buttonDataManager = new ButtonManager(15);

    public RemoteSensorControl(RemoteControlSensor parentRemoteControlSensor) {

        super(SENSOR_CONTROL_NAME, parentRemoteControlSensor);
    }

    private void defineRecordStructure() {

        final LeftAnalogControl leftAnalogControl = new LeftAnalogControl(analogControlManager, 0, 1);
        final RightAnalogControl rightAnalogControl = new RightAnalogControl(analogControlManager, 2, 3);
        analogControlManager.registerAnalogControls(leftAnalogControl, rightAnalogControl);

        final DefaultButton aButton = new A(buttonDataManager);
        final DefaultButton bButton = new B(buttonDataManager);
        final DefaultButton xButton = new X(buttonDataManager);
        final DefaultButton yButton = new Y(buttonDataManager);
        final DefaultButton l1Button = new L1(buttonDataManager);
        final DefaultButton r1Button = new R1(buttonDataManager);
        final DefaultButton l2Button = new L2(buttonDataManager);
        final DefaultButton r2Button = new R2(buttonDataManager);
        final DefaultButton selectButton = new Select(buttonDataManager);
        final DefaultButton startButton = new Start(buttonDataManager);
        final DefaultButton leftAnalogButton = new LeftAnalog(buttonDataManager);
        final DefaultButton rightAnalogButton = new RightAnalog(buttonDataManager);

        buttonDataManager.registerButtons(aButton, bButton, xButton, yButton,
                l1Button, r1Button, l2Button, r2Button,
                selectButton, startButton, leftAnalogButton, rightAnalogButton);

        SWEHelper factory = new SWEHelper();

        DataChoice actionChoice = factory.createChoice()
                .name("Action")
                .addItem(aButton.getName(), aButton.getSweDataComponent())
                .addItem(bButton.getName(), bButton.getSweDataComponent())
                .addItem(xButton.getName(), xButton.getSweDataComponent())
                .addItem(yButton.getName(), yButton.getSweDataComponent())
                .build();
        DataChoice leftTriggerChoice = factory.createChoice()
                .name("LeftTriggers")
                .addItem(l1Button.getName(), l1Button.getSweDataComponent())
                .addItem(l2Button.getName(), l2Button.getSweDataComponent())
                .build();
        DataChoice rightTriggerChoice = factory.createChoice()
                .name("RightTriggers")
                .addItem(r1Button.getName(), r1Button.getSweDataComponent())
                .addItem(r2Button.getName(), r2Button.getSweDataComponent())
                .build();
        DataChoice miscButtonChoice = factory.createChoice()
                .name("Miscellaneous")
                .addItem(selectButton.getName(), selectButton.getSweDataComponent())
                .addItem(startButton.getName(), startButton.getSweDataComponent())
                .build();

        commandDataStruct = factory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("Controls"))
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .addField("leftAnalogStick", leftAnalogControl.getSweDataComponent())
                .addField("rightAnalogStick", rightAnalogControl.getSweDataComponent())
                .addField("actionButtons", actionChoice)
                .addField("leftTriggerButtons", leftTriggerChoice)
                .addField("rightTriggerButtons", rightTriggerChoice)
                .addField("miscButtons", miscButtonChoice)
                .build();
    }

    public void doInit() {

        defineRecordStructure();

        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

        publisherNode = new RosPublisherNode<>(NODE_NAME_STR, TOPIC_STR, Joy._TYPE);
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

        try {

            DataChoice actionComponent = (DataChoice) commandData.getField("actionButtons");
            buttonDataManager.setButtonState(actionComponent.getSelectedItem().getName(),
                    ButtonState.fromString(actionComponent.getSelectedItem().getData().getStringValue()));

            DataChoice leftTriggersComponent = (DataChoice) commandData.getField("leftTriggerButtons");
            buttonDataManager.setButtonState(leftTriggersComponent.getSelectedItem().getName(),
                    ButtonState.fromString(leftTriggersComponent.getSelectedItem().getData().getStringValue()));

            DataChoice rightTriggersComponent = (DataChoice) commandData.getField("rightTriggerButtons");
            buttonDataManager.setButtonState(rightTriggersComponent.getSelectedItem().getName(),
                    ButtonState.fromString(rightTriggersComponent.getSelectedItem().getData().getStringValue()));

            DataChoice miscComponent = (DataChoice) commandData.getField("miscButtons");
            buttonDataManager.setButtonState(miscComponent.getSelectedItem().getName(),
                    ButtonState.fromString(miscComponent.getSelectedItem().getData().getStringValue()));

            DataComponent leftAnalogStick = commandData.getComponent("leftAnalogStick");
            Direction leftAnalogDirection = Direction.fromString(leftAnalogStick.getData().getStringValue());
            analogControlManager.getControlByPlacement(ControlPlacement.LEFT).setDirection(leftAnalogDirection);


            DataComponent rightAnalogStick = commandData.getComponent("rightAnalogStick");
            Direction rightAnalogDirection = Direction.fromString(rightAnalogStick.getData().getStringValue());
            analogControlManager.getControlByPlacement(ControlPlacement.RIGHT).setDirection(rightAnalogDirection);

            Joy message = publisherNode.getNewMessageBuffer();

            message.getHeader().setStamp(new Time(System.currentTimeMillis() / 1000.0));
            message.getHeader().setFrameId("/sensorhub/remoteSensorControl");
            message.getHeader().setSeq(++messageSequenceId);
            message.setAxes(analogControlManager.getAxisData());
            message.setButtons(buttonDataManager.getButtonData());

            publisherNode.publishMessage(message);

        } catch (Exception e) {

            throw new CommandException("Failed to handle command", e);
        }
        return true;
    }
}
