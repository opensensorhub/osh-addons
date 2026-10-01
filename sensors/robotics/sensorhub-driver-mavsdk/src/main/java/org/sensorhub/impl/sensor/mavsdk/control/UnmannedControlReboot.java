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
import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
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
 * This particular class provides control stream  capabilities
 * </p>
 *
 * @since Jul 2025
 */
public class UnmannedControlReboot extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedControlReboot";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "Reboot Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to reboot the platform. This reboots the autopilot " +
                    "(and companion computer, camera, and gimbal where present). Should only be sent while " +
                    "the vehicle is disarmed and on the ground.";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/reboot_control";

    private io.mavsdk.System system = null;

    public UnmannedControlReboot( UnmannedSystem parentSensor) {
        super("mavRebootControl", parentSensor);
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
                .definition(SWEHelper.getPropertyUri("Control"))
                .addField("reboot", factory.createBoolean()
                        .definition(SWEHelper.getPropertyUri("Reboot"))
                        .value(true))
                .build();
    }

    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {

        boolean reboot = command.getBooleanValue(0);

        log.debug("Command received - Reboot: " + reboot);

        // Only act on an explicit "true". A false value is a no-op so an
        // accidental command can't reboot the flight controller.
        if ( !reboot ) {
            return true;
        }

        if ( system != null ) {
            reboot();
        } else {
            throw new CommandException("Unmanned System not initialized");
        }

        return true;
    }

    private void reboot() {

        log.debug("Sending reboot command...");

        Disposable disposable = system.getAction().reboot()
                .doOnComplete(() -> {

                    log.debug("Reboot command sent");

                })
                .doOnError(throwable ->
                        log.debug("Failed to send reboot: " + ((Action.ActionException) throwable).getCode()))
                .onErrorResumeNext(throwable ->
                        Completable.error(new CommandException("Error rebooting drone", throwable)))
                .subscribe(() -> {
                            log.debug("Rebooted");
                        },
                        throwable -> {
                            throw new CommandException(throwable.getMessage());
                        });

    }
}