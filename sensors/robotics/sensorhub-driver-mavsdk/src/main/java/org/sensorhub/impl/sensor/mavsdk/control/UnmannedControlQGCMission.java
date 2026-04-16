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
import io.mavsdk.action_server.ActionServer;
import io.mavsdk.mission.Mission;
import io.mavsdk.mission_raw.MissionRaw;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.*;
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
public class UnmannedControlQGCMission extends AbstractSensorControl<UnmannedSystem>
{
    private DataRecord commandDataStruct;

    /**
     * Name of the control
     */
    private static final String SENSOR_CONTROL_NAME = "QGroundControlPlan";

    /**
     * Label for the control
     */
    private static final String SENSOR_CONTROL_LABEL = "QGC Mission Control";

    /**
     * Control description
     */
    private static final String SENSOR_CONTROL_DESCRIPTION =
            "Interfaces with MAVLINK and OSH to effectuate QGC mission control over the platform";

    /**
     * ROS Node name assigned at creation
     */
    private static final String NODE_NAME_STR = "/SensorHub/spot/qgc_mission_control";

    private io.mavsdk.System system = null;

    public UnmannedControlQGCMission(UnmannedSystem parentSensor) {
        super(SENSOR_CONTROL_NAME, parentSensor);
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
                .id(SENSOR_CONTROL_NAME)
                .label(SENSOR_CONTROL_LABEL)
                .description(SENSOR_CONTROL_DESCRIPTION)
                .addField("qGroundControlPlan",
                        helper.createText()
                                .definition(SWEHelper.getPropertyUri("Control"))
                                .value("None").build())
                .build();
    }

    @Override
    public CompletableFuture<ICommandStatus> submitCommand(ICommandData command) {

        ActionServer.AllowableFlightModes flightModes = new ActionServer.AllowableFlightModes(
                true,true,true
        );
        var res = system.getActionServer().setAllowableFlightModes(flightModes);

        String planJson = command.getParams().getStringValue(0);

        Instant start = Instant.now();

        MissionRaw missionRaw = system.getMissionRaw();
        missionRaw.importQgroundcontrolMissionFromString(planJson)
                .flatMapCompletable(importData -> {
                    return missionRaw.uploadMission(importData.getMissionItems());
                })
                .doOnComplete(() -> {
                    java.lang.System.out.println("Mission Uploaded Successfully");

                    Action action = system.getAction();

                    system.getTelemetry().setRateInAir(1.0).subscribe();

                    var aRet = system.getTelemetry().getArmed()
                        .firstOrError()
                        .subscribe( isArmed -> {
                            if ( !isArmed ) { //Not Armed
                                var gaRet = system.getTelemetry().getInAir()
                                    .firstOrError()
                                    .subscribe( isInAir -> {
                                        if ( !isInAir ) { //not in air

                                            action.arm()
                                                    .andThen(action.takeoff()
                                                            .andThen(
                                                                    missionRaw.startMission()
                                                                            .doOnComplete(() -> {
                                                                                java.lang.System.out.println("Mission started!");
                                                                            })
                                                                            .doOnError(throwable -> {
                                                                                java.lang.System.err.println("Failed to start mission: " +
                                                                                        throwable.getMessage());
                                                                            })
                                                            )
                                                            .doOnError(throwable -> {
                                                                java.lang.System.err.println("Failed to take off: " +
                                                                        throwable.getMessage());
                                                            })
                                                    )
                                                    .doOnError(throwable -> {
                                                        java.lang.System.err.println("Failed to arm: " +
                                                                throwable.getMessage());
                                                    })
                                                    .subscribe();
                                        }
                                    });
                            } else { //Armed

                                var td = system.getTelemetry().getInAir()
                                    .firstOrError()
                                    .subscribe( isInAir -> {
                                        if ( isInAir ) {
                                            missionRaw.startMission()
                                                    .doOnError(throwable -> {
                                                        java.lang.System.err.println("Failed to start mission: " +
                                                                throwable.getMessage());
                                                    })
                                                    .subscribe();
                                        } else {
                                            action.takeoff()
                                                .andThen(missionRaw.startMission()
                                                        .doOnError(throwable -> {
                                                            java.lang.System.err.println("Failed to start mission: " +
                                                                    throwable.getMessage());
                                                        })
                                                )
                                                .doOnComplete(() -> {
                                                    java.lang.System.out.println("Mission started!");
                                                })
                                                .doOnError( throwable -> {
                                                    java.lang.System.err.println("Failed to take off: " +
                                                            throwable.getMessage());
                                                })
                                                .subscribe();
                                        }
                                    });
                            }
                        });


                })
                .doOnError(error -> {
                    java.lang.System.err.println("Upload Failed: " + error.getMessage());
                })
                .subscribe();


        return CompletableFuture.supplyAsync(() -> {
                return new CommandStatus.Builder()
                        .withCommand(command.getID())
                        .withStatusCode(ICommandStatus.CommandStatusCode.COMPLETED)
                        .withExecutionTime(TimeExtent.endNow(start))
                        .build();

        });
    }

    public void stop() {
        // TODO Auto-generated method stub
    }

    /**
     * Currently missions through MAVSDK appear to not work via ArduPilot SITL. For now
     * lets control the mission directly
     * @param latitudeDeg
     * @param longitudeDeg
     * @param hoverSecondsParam
     * @return
     */
    public static Mission.MissionItem generateMissionItem( double latitudeDeg, double longitudeDeg, double hoverSecondsParam ) {

        return new Mission.MissionItem(
                latitudeDeg,
                longitudeDeg,
                20f,
                1.0f,
                true,
                0f,
                0f,
                Mission.MissionItem.CameraAction.NONE,
                (float)hoverSecondsParam,
                1.0,
                0f,
                0f,
                0f,
                Mission.MissionItem.VehicleAction.NONE);
    }
}

