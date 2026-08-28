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

import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.impl.sensor.mavsdk.UnmannedSystem;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.helper.GeoPosHelper;

/**
 * <p>
 * This particular class provides control stream  capabilities
 * </p>
 *
 * @since Jul 2025
 */
public class UnmannedControlArm extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedControlArm";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "Arming Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to effectuate arm/disarm control over the platform. " +
                    "Send true to arm (start the motors), false to disarm (stop the motors).";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/arm_control";

    private io.mavsdk.System system = null;

    public UnmannedControlArm( UnmannedSystem parentSensor) {
        super("mavArmControl", parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    public void setSystem( io.mavsdk.System systemParam ) {
        system = systemParam;
    }

    public void init() {

        GeoPosHelper factory = new GeoPosHelper();

        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .definition("http://sensorml.com/ont/isa/property/Enabled")
                .addField("ARM", factory.createBoolean()
                        .definition("http://sensorml.com/ont/isa/property/Enabled")
                        .value(true) )
                .build();
    }

    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {

        try {
            // The ARM field is a boolean: true -> arm, false -> disarm
            boolean armCommand = command.getBooleanValue(0);

            log.debug("Command received - Arm: " + armCommand);

            if (system != null) {
                if (armCommand) {
                    arm();
                } else {
                    disarm();
                }
            } else {
                throw new CommandException("Unmanned System not initialized");
            }
        } catch (CommandException e) {
            getLogger().error("Error processing command", e);

            parentSensor.reportError( e.getMessage(), e);
        }

        return true;
    }

    private void arm() {

        log.debug("Arming");

        Disposable disposable = system.getAction().arm()
                .doOnComplete(() -> {

                    log.debug("Arming...");

                })
                .onErrorResumeNext(throwable ->
                        Completable.error(new CommandException("Error arming drone", throwable)))
                .subscribe(() -> {
                            log.debug("Armed");
                        },
                        throwable -> {
                            throw new CommandException(throwable.getMessage());
                        });

    }

    private void disarm() {

        log.debug("Disarming");

        // NOTE: MAVSDK/autopilots will reject a disarm command while the vehicle is
        //       flying (a drone that considers itself in-air refuses to disarm). To
        //       disarm after a landing, see UnmannedControlLanding which waits for the
        //       ON_GROUND state before disarming.
        Disposable disposable = system.getAction().disarm()
                .doOnComplete(() -> {

                    log.debug("Disarming...");

                })
                .onErrorResumeNext(throwable ->
                        Completable.error(new CommandException("Error disarming drone", throwable)))
                .subscribe(() -> {
                            log.debug("Disarmed");
                        },
                        throwable -> {
                            throw new CommandException(throwable.getMessage());
                        });

    }
}