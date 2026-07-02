/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is GeoRobotix Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/


package org.sensorhub.impl.sensor.mavsdk.control;

import io.mavsdk.offboard.Offboard;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.mavsdk.UnmannedSystem;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

/**
 * <p>
 * Manual "drive" control for a ground rover or surface vessel: a forward/back velocity
 * and a yaw (turn) rate. Unlike the airborne {@link UnmannedControlOffboard}, there is no
 * lateral (strafe) or vertical component, since wheeled/hulled vehicles are non-holonomic
 * and stay on the surface. Sending zeros stops the vehicle.
 * </p>
 *
 * <p>
 * Uses MAVSDK Offboard velocity setpoints (GUIDED on ArduRover). Offboard is started
 * lazily on the first command <i>after</i> the vehicle-type guard passes, so a multirotor
 * is never switched into offboard by this control. The control refuses to run if the
 * connected vehicle is positively identified as airborne.
 * </p>
 *
 * @since Jul 2025
 */
public class UnmannedControlDriveVelocity extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedControlDriveVelocity";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "Drive Velocity Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to manually drive a ground rover or surface vessel " +
            "using a forward velocity and a yaw rate. Send zeros to stop. Rejected for airborne platforms.";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/drive_velocity_control";

    private io.mavsdk.System system = null;

    public UnmannedControlDriveVelocity( UnmannedSystem parentSensor) {
        super("mavDriveVelocityControl", parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    public void setSystem( io.mavsdk.System systemParam ) {
        // Note: offboard is intentionally NOT started here. It is started lazily in
        // execCommand once the surface-vehicle guard has passed, so this control can never
        // put an air vehicle into offboard/guided mode just by connecting.
        system = systemParam;
    }

    public void init() {

        GeoPosHelper factory = new GeoPosHelper();

        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("Control"))
                .addField("forwardVelocity", factory.createQuantity()
                        .definition(SWEHelper.getPropertyUri("PlatformVelocity"))
                        .label("Forward Velocity")
                        .uom("m/s")
                        .dataType(DataType.FLOAT))
                .addField("yawRate", factory.createQuantity()
                        .definition(SWEHelper.getPropertyUri("YawRate"))
                        .label("Yaw Rate")
                        .uom("deg/s")
                        .dataType(DataType.FLOAT))
                .build();
    }

    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {

        VehicleDomain domain = parentSensor.getVehicleDomain();
        if ( !domain.permitsSurfaceCommand() ) {
            throw new CommandException("Refusing drive command: connected vehicle is airborne ("
                    + domain + "). Use the offboard/velocity flight control instead.");
        }

        float forwardVelocity = command.getFloatValue(0);
        float yawRate = command.getFloatValue(1);

        log.debug("Command received - Drive forward: " + forwardVelocity + " m/s, yaw: " + yawRate + " deg/s");

        if ( system == null ) {
            throw new CommandException("Unmanned System not initialized");
        }

        // Non-holonomic surface vehicle: no lateral (right) or vertical (down) component.
        Offboard.VelocityBodyYawspeed velocityBodyYawspeed = new Offboard.VelocityBodyYawspeed(
                forwardVelocity,
                0.0f,
                0.0f,
                yawRate
        );

        // Offboard requires a setpoint to be streaming before start() is accepted.
        system.getOffboard().setVelocityBody(velocityBodyYawspeed)
                .doOnError(e -> log.debug("Unable to set body velocity: " + e.getMessage()))
                .subscribe(
                        () -> log.debug("Driving: " + forwardVelocity + " m/s, yaw " + yawRate + " deg/s"),
                        throwable -> log.error("Drive velocity error: " + throwable.getMessage())
                );

        if ( !system.getOffboard().isActive().blockingGet() ) {
            system.getOffboard().start()
                    .doOnComplete(() -> log.debug("Started offboard (GUIDED) control for surface vehicle"))
                    .doOnError(throwable -> log.debug("Failed to start offboard: " + throwable.getMessage()))
                    .subscribe(
                            () -> { },
                            throwable -> log.error("Offboard start error: " + throwable.getMessage())
                    );
        }

        return true;
    }
}
