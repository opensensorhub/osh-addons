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

import io.mavsdk.action.Action;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.mavsdk.UnmannedSystem;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

/**
 * <p>
 * Commands a ground rover or surface vessel to drive to a geographic position.
 * This is the non-flying counterpart to {@link UnmannedControlLocation}: there is no
 * altitude/AGL handling and no takeoff gating, because driven vehicles operate on the
 * surface and are ready to move as soon as they are armed and in GUIDED mode.
 * </p>
 *
 * <p>
 * Works for both ground rovers (MAV_TYPE_GROUND_ROVER) and surface boats
 * (MAV_TYPE_SURFACE_BOAT) since both run ArduRover. The control refuses to run if the
 * connected vehicle is positively identified as airborne.
 * </p>
 *
 * @since Jul 2025
 */
public class UnmannedControlDriveToLocation extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedControlDriveToLocation";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "Drive To Location Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to drive a ground rover or surface vessel to a " +
            "geographic location. Vehicle must be armed and in GUIDED mode. Rejected for airborne platforms.";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/drive_to_location_control";

    private io.mavsdk.System system = null;

    // Altitude is irrelevant for ground/surface vehicles, but MAVSDK's gotoLocation still
    // requires an absolute altitude argument, so we track the last-known value and feed it
    // back so the autopilot receives a sane number.
    private volatile float currentAbsoluteAltM = 0.0f;

    public UnmannedControlDriveToLocation( UnmannedSystem parentSensor) {
        super("mavDriveToLocationControl", parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    public void setSystem( io.mavsdk.System systemParam ) {

        system = systemParam;

        if ( null != system ) {
            system.getTelemetry().getPosition()
                    .subscribe(
                            pos -> currentAbsoluteAltM = pos.getAbsoluteAltitudeM(),
                            err -> log.error("MAVSDK: Position error: " + err)
                    );
        }
    }

    public void init() {

        GeoPosHelper factory = new GeoPosHelper();

        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("Control"))
                .addField("locationVectorLL", factory.createVector()
                        .definition(SWEHelper.getPropertyUri("Location"))
                        .addCoordinate("Latitude", factory.createQuantity()
                                .uom("deg"))
                        .addCoordinate("Longitude", factory.createQuantity()
                                .uom("deg")))
                .build();
    }

    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {

        VehicleDomain domain = parentSensor.getVehicleDomain();
        if ( !domain.permitsSurfaceCommand() ) {
            throw new CommandException("Refusing drive-to-location: connected vehicle is airborne ("
                    + domain + "). Use the air Location control instead.");
        }

        double latitude = command.getDoubleValue(0);
        double longitude = command.getDoubleValue(1);

        log.debug("Command received - Drive to Lat: " + latitude + " Lon: " + longitude);

        if ( system != null ) {
            driveToLocation( latitude, longitude );
        } else {
            throw new CommandException("Unmanned System not initialized");
        }

        return true;
    }

    private void driveToLocation( double latitudeParam, double longitudeParam ) {

        log.debug("Driving to location...");

        // Yaw is NaN: a rover/boat naturally points in its direction of travel, and
        // ArduRover ignores the yaw argument for goto. Substitute a heading here if your
        // platform needs a commanded final heading.
        system.getAction().gotoLocation(latitudeParam, longitudeParam, currentAbsoluteAltM, Float.NaN)
                .doOnComplete(() -> {

                    log.debug("Moving to target location");

                })
                .doOnError(throwable -> {

                    log.debug("Failed to drive to target: " + ((Action.ActionException) throwable).getCode());

                })
                .subscribe(
                        () -> { },
                        throwable -> log.error("Drive-to-location error: " + throwable.getMessage())
                );
    }
}
