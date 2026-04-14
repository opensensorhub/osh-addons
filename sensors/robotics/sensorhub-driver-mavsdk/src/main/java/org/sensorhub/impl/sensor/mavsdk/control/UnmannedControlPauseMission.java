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

import io.mavsdk.mavlink_direct.MavlinkDirect;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
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
 * Mission Pausing control stream capabilities
 * </p>
 */
public class UnmannedControlPauseMission extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    private static final String SENSOR_CONTROL_NAME = "UnmannedControlPauseMission";
    private static final String SENSOR_CONTROL_LABEL = "Mission Pause Control";
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH mission pause control";

    private static final String NODE_NAME_STR = "/SensorHub/spot/mission_pause_control";

    private io.mavsdk.System system = null;

    public UnmannedControlPauseMission(UnmannedSystem parentSensor) {
        super("mavPauseMissionControl", parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    public void setSystem( io.mavsdk.System systemParam ) {
        system = systemParam;
    }

    public void init() {

        //SWEHelper helper = new SWEHelper();
        GeoPosHelper helper = new GeoPosHelper();
        commandDataStruct = helper.createRecord()
                .id("PauseMission")
                .label("PauseMission")
                .description("PauseMission")
                .addField("Resume", helper.createBoolean()
                        .definition(SWEHelper.getPropertyUri("Control"))
                )
                .build();
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {

        Instant start = Instant.now();

        CompletableFuture<ICommandStatus> future = new CompletableFuture<>();

        var resume = command.getParams().getBooleanValue(0);

        //for Ardupilot use MAV_SYSID
        //for PX4 use MAV_SYS_ID, in the future add support for PX4
        system.getParam().getParamInt("MAV_SYSID").subscribe(
                (systemID) -> {

                    var telem = system.getTelemetry().getPosition()
                            .firstElement()
                            .subscribe(pos -> {

                                if ( !resume ) { //Mission Pause
                                    MavlinkDirect.MavlinkMessage guidedCommand = new MavlinkDirect.MavlinkMessage(
                                            "COMMAND_LONG", // 1. Message Name
                                            255,            // 2. Sender System ID (GCS is usually 255)
                                            1,              // 3. Sender Component ID
                                            systemID,              // 4. Target System ID (The Drone, usually 1)
                                            1,              // 5. Target Component ID (The Autopilot, usually 1)
                                            "{ \"command\": 176, " +      // 6. JSON Payload
                                                    "\"param1\": 1.0, " +       // Custom Mode Enabled
                                                    "\"param2\": 4.0, " +       // 4 = GUIDED
                                                    "\"param3\": 0, \"param4\": 0, \"param5\": 0, \"param6\": 0, \"param7\": 0, " +
                                                    "\"confirmation\": 0, \"target_system\": 1, \"target_component\": 1 }"
                                    );
                                    sendOff(guidedCommand, command, future, start);

                                } else {  //Resume
                                    MavlinkDirect.MavlinkMessage resumeCommand = new MavlinkDirect.MavlinkMessage(
                                            "COMMAND_LONG", // 1. Message Name
                                            255,            // 2. Sender System ID
                                            1,              // 3. Sender Component ID
                                            systemID,       // 4. Target System ID
                                            1,              // 5. Target Component ID
                                            "{ \"command\": 176, " +      // MAV_CMD_DO_SET_MODE
                                                    "\"param1\": 1.0, " +       // Custom Mode Enabled
                                                    "\"param2\": 3.0, " +       // 3 = AUTO (Resumes Mission in ArduCopter)
                                                    "\"param3\": 0, \"param4\": 0, \"param5\": 0, \"param6\": 0, \"param7\": 0, " +
                                                    "\"confirmation\": 0, \"target_system\": 1, \"target_component\": 1 }"
                                    );
                                    sendOff(resumeCommand, command, future, start);
                                }
                                 }, throwable -> {

                                System.err.println("Failed to get system ID when pausing mission");

                                        future.complete(new CommandStatus.Builder()
                                                .withCommand(command.getID())
                                                .withStatusCode(ICommandStatus.CommandStatusCode.FAILED)
                                                .withExecutionTime(TimeExtent.endNow(start))
                                                .build());
                                    }
                            );
                }, throwable -> {

                    System.err.println("Failed to get System ID when pausing mission");

                    future.complete(new CommandStatus.Builder()
                            .withCommand(command.getID())
                            .withStatusCode(ICommandStatus.CommandStatusCode.FAILED)
                            .withExecutionTime(TimeExtent.endNow(start))
                            .build());
                });

        return future;
    }

    private void sendOff(MavlinkDirect.MavlinkMessage guidedCommand,
                         ICommandData command,
                         CompletableFuture<ICommandStatus> future,
                         Instant start) {

        system.getMavlinkDirect().sendMessage(guidedCommand)
                .subscribe(
                        () -> {

                            System.out.println("Mavlink Mission Paused");
                            future.complete(new CommandStatus.Builder()
                                    .withCommand(command.getID())
                                    .withStatusCode(ICommandStatus.CommandStatusCode.COMPLETED)
                                    .withExecutionTime(TimeExtent.endNow(start))
                                    .build());

                        },
                        error -> {

                            System.err.println("Failed to send command: " + error.getMessage());

                            future.complete(new CommandStatus.Builder()
                                    .withCommand(command.getID())
                                    .withStatusCode(ICommandStatus.CommandStatusCode.FAILED)
                                    .withExecutionTime(TimeExtent.endNow(start))
                                    .build());

                        }
                );
    }
}

