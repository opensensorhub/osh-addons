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

import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.impl.sensor.mavsdk.UnmannedSystem;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * This particular class provides control stream  capabilities
 * </p>
 *
 * @since Jul 2025
 */
public class UnmannedControlTakeoff extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedControlTakeoff";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "Takeoff Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to effectuate takeoff control over the platform";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/takeoff_control";

    private io.mavsdk.System system = null;

    private float takeoffAltThreshold = 3.0f;

    private UnmannedControlLocation locationRef;

    public UnmannedControlTakeoff( UnmannedSystem parentSensor) {
        super("mavTakeoffControl", parentSensor);
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    public void setSystem( io.mavsdk.System systemParam ) {
        system = systemParam;
    }

    public void init(UnmannedControlLocation locationCommand /*For disabling on land command*/) {

        locationRef = locationCommand;

        GeoPosHelper factory = new GeoPosHelper();

        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri("Control"))
                .addField("TakeoffAltitudeAGL", factory.createQuantity()
                                .definition(GeoPosHelper.DEF_ALTITUDE_GROUND))
                .build();
    }

    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {

        try {
            double altitude = command.getDoubleValue(0);

            log.debug("Command received - Alt: " + altitude);

            if (system != null) {
                takeOff(altitude);
            } else {
                throw new CommandException("Unmanned System not initialized");
            }
        } catch (CommandException e) {
            getLogger().error("Error processing command", e);

            parentSensor.reportError( e.getMessage(), e);
        }

        return true;
    }

    private void takeOff( double altAglParam ) {

        log.debug("Setting up scenario...");

        log.debug("Setting takeoff altitude AGL: " + altAglParam);

        Disposable disposable = system.getAction().arm()
                .doOnComplete(() -> {

                    log.debug("Arming...");

                })
                .onErrorResumeNext(throwable ->
                        Completable.error(new CommandException("Error arming drone", throwable)))
                .andThen(system.getAction().setTakeoffAltitude((float)altAglParam))
                .onErrorResumeNext(throwable ->
                        Completable.error(new CommandException("Error setting takeoff altitude", throwable)))
                .andThen(system.getAction().takeoff()
                        .doOnComplete(() -> {

                            log.debug("Taking off...");

                        })
                        .onErrorResumeNext(throwable ->
                                Completable.error(new CommandException("Failed to take off", throwable))))
                .andThen(system.getTelemetry().getPosition()
                        .filter(pos -> pos.getRelativeAltitudeM() >= (altAglParam - takeoffAltThreshold))
                        .firstElement()
                        .ignoreElement()
                )
                .subscribe(() -> {
                        log.debug("Reached takeoff altitude");

                        if ( null != locationRef) {
                            locationRef.enable();
                        }
                    },
          throwable -> {
                        throw new CommandException(throwable.getMessage());
                    });

    }


}

