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
import org.sensorhub.api.command.CommandStatus;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.impl.sensor.mavsdk.UnmannedSystem;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import org.vast.util.TimeExtent;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * This particular class provides control stream  capabilities
 * </p>
 *
 * @since Jul 2025
 */
public class UnmannedControlRTL extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "UnmannedControlRTL";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "RTL Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to effectuate RTL control over the platform";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/RTL_control";

    private io.mavsdk.System system = null;

    static double deltaSuccess =   0.000003; //distance from lat/lon to determine success

    public UnmannedControlRTL(UnmannedSystem parentSensor) {
        super("mavRTLControl", parentSensor);
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
                .addField("rtl", factory.createBoolean()
                        .definition(SWEHelper.getPropertyUri("Control"))
                        .value(true))
                .build();
    }


    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {

        Instant start = Instant.now();

        boolean value = command.getParams().getBooleanValue(0);

        log.debug("Command received - RTL");

        if ( system != null ) {
            RTL( value );
        } else {
            try {
                throw new CommandException("Unmanned System not initialized");
            } catch (CommandException e) {
                throw new RuntimeException(e);
            }
        }

        return CompletableFuture.supplyAsync(() -> {
            return new CommandStatus.Builder()
                    .withCommand(command.getID())
                    .withStatusCode(ICommandStatus.CommandStatusCode.COMPLETED)
                    .withExecutionTime(TimeExtent.endNow(start))
                    .build();

        });
    }

    private void RTL( boolean disarmParam ) {

        log.debug("Setting up scenario...");

        system.getAction().returnToLaunch()
                .doOnComplete( () -> {
                    log.debug("RTL...");
                })
                .doOnError( throwable -> {
                    log.debug("Failed to send RTL: " + ((Action.ActionException) throwable).getCode());

                })
                .subscribe(() -> {
                    log.debug("Command Sent");
                });
    }


    public void stop() {
        // TODO Auto-generated method stub
    }
}

