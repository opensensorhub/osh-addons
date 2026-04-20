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

import io.mavsdk.action.Action;
import io.mavsdk.telemetry.Telemetry;
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
public class UnmannedControlLanding extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedControlLanding";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "Landing Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to effectuate landing control over the platform";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/landing_control";

    private io.mavsdk.System system = null;

    static double deltaSuccess =   0.000003; //distance from lat/lon to determine success

    public UnmannedControlLanding( UnmannedSystem parentSensor) {
        super("mavLandingControl", parentSensor);
    }


    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    public void setSystem( io.mavsdk.System systemParam ) {
        system = systemParam;
    }

    private UnmannedControlLocation locationRef;

    public void init(UnmannedControlLocation locationCommand /*For disabling on land command*/) {

        locationRef = locationCommand;

        GeoPosHelper factory = new GeoPosHelper();

        commandDataStruct = factory.createRecord()
                .name(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .addField("disarm", factory.createBoolean().value(true)
                        .definition(SWEHelper.getPropertyUri("Control"))
                )
                .build();
    }


    @Override
    protected boolean execCommand(DataBlock command) throws CommandException {

        if ( null != locationRef) {
            locationRef.disable();
        }

        boolean disarm = command.getBooleanValue(0);

        log.debug("Command received - Land - Disarm: " + disarm );

        if ( system != null ) {
            land( disarm );
        } else {
            throw new CommandException("Unmanned System not initialized");
        }

        return true;
    }


    private void land( boolean disarmParam ) {

        log.debug("Setting up scenario...");

        system.getAction().land()
                .doOnComplete( () -> {

                    log.debug("Landing...");

                })
                .doOnError( throwable -> {
                    log.debug("Failed to land: " + ((Action.ActionException) throwable).getCode());

                })
                .andThen(system.getTelemetry().getLandedState()
                        .filter( landed -> landed == Telemetry.LandedState.ON_GROUND )
                        .firstElement()
                        .ignoreElement()
                )
                .subscribe(() -> {

                    log.debug("Landed");

                    if ( disarmParam ) {

                        log.debug("Checking to see if disarm necessary..." );

                        system.getTelemetry().getArmed()
                            .firstElement()
                            .subscribe( armed -> {
                               if ( armed ) {

                                   log.debug("System already disarmed.");
                               } else {

                                   system.getAction().disarm()
                                           .doOnComplete( () -> {

                                               log.debug("Disarming...");

                                           })
                                           .doOnError( throwable -> {

                                               log.debug("Failed to disarm: " + ((Action.ActionException) throwable).getCode());

                                           });
                               }

                            });


                    }

                })
        ;
    }


    public void stop() {
        // TODO Auto-generated method stub
    }
}

