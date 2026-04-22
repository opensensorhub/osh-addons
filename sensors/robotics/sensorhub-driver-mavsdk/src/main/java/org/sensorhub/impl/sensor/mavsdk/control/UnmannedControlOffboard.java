/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

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

import static java.lang.Math.abs;

/**
 * <p>
 * This particular class provides control stream  capabilities
 * </p>
 *
 * @since Jul 2025
 */
public class UnmannedControlOffboard extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedControlOffboard";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "Offboard Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to effectuate control over the platform";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/location_control";

    private io.mavsdk.System system = null;

    static double deltaSuccess =   0.000003; //distance from lat/lon to determine success

    public UnmannedControlOffboard(UnmannedSystem parentSensor) {
        super("offboardControl", parentSensor);
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    public void setSystem( io.mavsdk.System systemParam ) {
        system = systemParam;
        if (!system.getOffboard().isActive().blockingGet())
            system.getOffboard().start()
                    .doOnComplete(() -> log.debug("Started offboard control"))
                    .doOnError(throwable -> throwable.printStackTrace());
    }

    public void init() {
        GeoPosHelper factory = new GeoPosHelper();

        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("Control"))
                .addField("velocity", factory.newVelocityVectorNED(SWEHelper.getPropertyUri("PlatformVelocity"), "m/s"))
                .addField("yawRate", factory.createQuantity()
                        .definition(SWEHelper.getPropertyUri("YawRate"))
                        .label("Yaw Rate")
                        .uom("deg/s")
                        .dataType(DataType.FLOAT))
                .build();
    }


    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {
        float forwardVelocity = command.getFloatValue(0);
        float rightVelocity = command.getFloatValue(1);
        float downVelocity = command.getFloatValue(2);
        float yawSpeed = command.getFloatValue(3);
        Offboard.VelocityBodyYawspeed velocityBodyYawspeed = new Offboard.VelocityBodyYawspeed(
                forwardVelocity,
                rightVelocity,
                downVelocity,
                yawSpeed
        );

        if (system != null)
            system.getOffboard().setVelocityBody(velocityBodyYawspeed)
                .doOnComplete(() -> log.debug("Moving in direction " + forwardVelocity + ", " + rightVelocity + ", " + downVelocity))
                .doOnError((e) -> log.debug("Unable to set body velocity: " + e.getMessage()))
                .subscribe(() -> {
                    //???
                });
        else {
            throw new CommandException("Unmanned System not initialized");
        }

        return true;
    }


}

