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

import io.mavsdk.mavlink_direct.MavlinkDirect;
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
 * Sets the vehicle's home position to a commanded Lat/Lon. Works on air, ground, and surface
 * platforms (MAV_CMD_DO_SET_HOME is supported by all ArduPilot vehicle types), so there is no
 * {@link VehicleDomain} gating.
 * </p>
 *
 * <p>
 * MAVSDK-Java has no exposed set-home API in any released version (3.16.0 included), so this uses
 * a single raw MAV_CMD_DO_SET_HOME via {@link MavlinkDirect}. It is sent as COMMAND_INT so the
 * Lat/Lon ride in the int32 x/y fields (degrees x 1e7) at full precision rather than being
 * truncated by COMMAND_LONG's float32 params.
 * </p>
 *
 * @since Jun 2026
 */
public class UnmannedControlHome extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    private static final String SENSOR_CONTROL_NAME = "UnmannedControlHome";
    private static final String SENSOR_CONTROL_LABEL = "Set Home Location Control";
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Sets the vehicle's home position to a geographic Lat/Lon location. " +
                    "Works for air, ground, and surface platforms.";

    private static final String NODE_NAME_STR = "/SensorHub/spot/set_home_control";

    private static final int MAV_CMD_DO_SET_HOME = 179;
    private static final int MAV_FRAME_GLOBAL = 0;   // x/y = lat/lon (deg*1e7), z = alt AMSL (m)

    private io.mavsdk.System system = null;

    // DO_SET_HOME needs an absolute (AMSL) altitude; the operator gives Lat/Lon only, so we feed
    // the vehicle's last-known altitude (irrelevant for ground/surface, ~current alt for air).
    private volatile float currentAbsoluteAltM = 0.0f;

    public UnmannedControlHome( UnmannedSystem parentSensor ) {
        super("mavSetHomeControl", parentSensor);
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

        double latitude = command.getDoubleValue(0);
        double longitude = command.getDoubleValue(1);

        log.debug("Command received - Set Home Lat: " + latitude + " Lon: " + longitude);

        if ( system != null ) {
            setHome( latitude, longitude );
        } else {
            throw new CommandException("Unmanned System not initialized");
        }

        return true;
    }

    private void setHome( double latitudeParam, double longitudeParam ) {

        // MAV_CMD_DO_SET_HOME via COMMAND_INT. param1=0 -> use the specified location;
        // x/y are lat/lon in degrees*1e7, z is altitude AMSL.
        String fields = String.format(
                "{\"command\":%d,\"frame\":%d,\"current\":0,\"autocontinue\":0," +
                        "\"param1\":0,\"param2\":0,\"param3\":0,\"param4\":0," +
                        "\"x\":%d,\"y\":%d,\"z\":%s," +
                        "\"target_system\":1,\"target_component\":1}",
                MAV_CMD_DO_SET_HOME, MAV_FRAME_GLOBAL,
                Math.round(latitudeParam * 1e7), Math.round(longitudeParam * 1e7),
                Float.toString(currentAbsoluteAltM));

        system.getMavlinkDirect()
                .sendMessage(new MavlinkDirect.MavlinkMessage("COMMAND_INT", 255, 1, 1, 1, fields))
                .subscribe(
                        () -> log.debug("Home set to Lat: " + latitudeParam + " Lon: " + longitudeParam),
                        err -> log.error("Set-home failed: " + err.getMessage())
                );
    }
}