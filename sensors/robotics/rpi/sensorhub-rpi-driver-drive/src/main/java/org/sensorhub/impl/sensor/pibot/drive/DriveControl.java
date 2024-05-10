/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * See the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) 2021-2024 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.sensor.pibot.drive;

import org.sensorhub.impl.pibot.common.control.BaseSensorControl;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandException;
import org.vast.swe.SWEHelper;

/**
 * Control specification and provider for PiBot DriveSensor Module
 *
 * @author Nick Garay
 * @since Jan. 24, 2021
 */
public class DriveControl extends BaseSensorControl<DriveSensor> {

    private static final String SENSOR_CONTROL_NAME = "DriveControl";

    private static final double MIN_POWER = 0.0;

    private static final double MAX_POWER = 100.0;

    protected DriveControl(DriveSensor parentSensor) {
        super(SENSOR_CONTROL_NAME, parentSensor);
    }

    @Override
    public boolean execCommand(DataBlock command) throws CommandException {

        try {

            DataRecord commandData = commandDataStruct.copy();

            commandData.setData(command);

            DataComponent directionComponent = commandData.getField("Command");

            DriveDirection direction = DriveDirection.fromString(directionComponent.getData().getStringValue());

            DataComponent powerComponent = commandData.getField("Power");

            DataBlock data = powerComponent.getData();

            double power = data.getDoubleValue();

            power = (power <= MIN_POWER) ? MIN_POWER : Math.min(power, MAX_POWER);

            parentSensor.move(direction, power);

        } catch (Exception e) {

            throw new CommandException("Failed to command the UltrasonicSensor module: ", e);
        }

        return true;
    }

    @Override
    protected void init() {

        SWEHelper factory = new SWEHelper();
        commandDataStruct = factory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("DriveSensor"))
                .label("DriveSensor")
                .description("An drive actuator for locomotion")
                .addField("Command",
                        factory.createCategory()
                                .name("Motors")
                                .label("Motors")
                                .definition(SWEHelper.getPropertyUri("MotorControl"))
                                .description("Controls direction of motion for tracked robotics platform")
                                .addAllowedValues(
                                        DriveDirection.FORWARD.name(),
                                        DriveDirection.FORWARD_TURN_LEFT.name(),
                                        DriveDirection.FORWARD_TURN_RIGHT.name(),
                                        DriveDirection.SPIN_LEFT.name(),
                                        DriveDirection.SPIN_RIGHT.name(),
                                        DriveDirection.REVERSE_TURN_LEFT.name(),
                                        DriveDirection.REVERSE_TURN_RIGHT.name(),
                                        DriveDirection.REVERSE.name(),
                                        DriveDirection.STOP.name())
                                .value(DriveDirection.STOP.name())
                                .build())
                .addField("Power",
                        factory.createQuantity()
                                .name("MotorPower")
                                .label("Motor Power")
                                .definition(SWEHelper.getPropertyUri("Power"))
                                .description("Denotes the percentage of power to apply to the drive actuators")
                                .addAllowedInterval(MIN_POWER, MAX_POWER)
                                .uomCode("%")
                                .build())
                .build();
    }
}
