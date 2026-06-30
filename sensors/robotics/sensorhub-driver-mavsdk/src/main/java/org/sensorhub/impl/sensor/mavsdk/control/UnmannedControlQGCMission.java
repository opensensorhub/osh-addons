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
 * <p>
 * Uploads a QGroundControl plan and starts it. The way the platform is brought into its
 * mission depends on its {@link VehicleDomain}:
 * </p>
 * <ul>
 *   <li><b>Flying vehicles</b> (and, fail-open, UNKNOWN): arm (if needed) and takeoff (if not
 *       already in the air) before starting the mission. This is the original behavior, kept
 *       intact.</li>
 *   <li><b>Ground / surface / submarine vehicles</b>: arm (if needed) and start the mission with
 *       <i>no</i> takeoff. Starting a MissionRaw mission switches ArduRover into AUTO.</li>
 * </ul>
 *
 * <p>
 * The decision is made via {@link VehicleDomain#permitsFlightCommand()}, which is only false when
 * the vehicle is positively identified as ground/surface/sub. Note that the plan itself should be
 * authored for the target vehicle: a quad plan that begins with a TAKEOFF item will have that item
 * rejected by ArduRover.
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
                .definition(SWEHelper.getPropertyUri("Control"))
                .addField("qGroundControlPlan",
                        helper.createText()
                                .definition(SWEHelper.getPropertyUri("Mission"))
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

        // Decide once, up front, how this platform should be brought into its mission.
        // permitsFlightCommand() is true for AIR and (fail-open) UNKNOWN, and false only when the
        // vehicle is positively known to be ground/surface/submarine. Driven vehicles must NOT be
        // sent takeoff(); they simply arm and start the mission (ArduRover switches to AUTO).
        final VehicleDomain domain = parentSensor.getVehicleDomain();
        final boolean isFlying = domain.permitsFlightCommand();

        MissionRaw missionRaw = system.getMissionRaw();
        missionRaw.importQgroundcontrolMissionFromString(planJson)
                .flatMapCompletable(importData -> {
                    return missionRaw.uploadMission(importData.getMissionItems());
                })
                .doOnComplete(() -> {
                    log.debug("Mission Uploaded Successfully");

                    Action action = system.getAction();

                    system.getTelemetry().setRateInAir(1.0).subscribe();

                    if ( isFlying ) {
                        startFlightMission(action, missionRaw);
                    } else {
                        log.debug("Surface/ground platform (" + domain
                                + "): starting mission without takeoff");
                        startSurfaceMission(action, missionRaw);
                    }
                })
                .doOnError(error -> {
                    log.error("Upload Failed: " + error.getMessage());
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


    /**
     * Flight-platform mission start (unchanged original behavior).
     * Arms if needed, takes off if not already airborne, then starts the mission.
     */
    private void startFlightMission( Action action, MissionRaw missionRaw ) {

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
                                                                            log.debug("Mission started!");
                                                                        })
                                                                        .doOnError(throwable -> {
                                                                            log.error("Failed to start mission: " +
                                                                                    throwable.getMessage());
                                                                        })
                                                        )
                                                        .doOnError(throwable -> {
                                                            log.error("Failed to take off: " +
                                                                    throwable.getMessage());
                                                        })
                                                )
                                                .doOnError(throwable -> {
                                                    log.error("Failed to arm: " +
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
                                                    log.error("Failed to start mission: " +
                                                            throwable.getMessage());
                                                })
                                                .subscribe();
                                    } else {
                                        action.takeoff()
                                                .andThen(missionRaw.startMission()
                                                        .doOnError(throwable -> {
                                                            log.error("Failed to start mission: " +
                                                                    throwable.getMessage());
                                                        })
                                                )
                                                .doOnComplete(() -> {
                                                    log.debug("Mission started!");
                                                })
                                                .doOnError( throwable -> {
                                                    log.error("Failed to take off: " +
                                                            throwable.getMessage());
                                                })
                                                .subscribe();
                                    }
                                });
                    }
                });
    }


    /**
     * Ground / surface / submarine mission start. No takeoff: arm (if needed) and start the
     * mission. Starting a MissionRaw mission puts ArduRover into AUTO and it begins driving the
     * uploaded waypoints.
     */
    private void startSurfaceMission( Action action, MissionRaw missionRaw ) {

        system.getTelemetry().getArmed()
                .firstOrError()
                .subscribe( isArmed -> {
                    if ( !isArmed ) { //Not Armed

                        action.arm()
                                .andThen(missionRaw.startMission()
                                        .doOnComplete(() -> {
                                            log.debug("Mission started!");
                                        })
                                        .doOnError(throwable -> {
                                            log.error("Failed to start mission: " +
                                                    throwable.getMessage());
                                        })
                                )
                                .doOnError(throwable -> {
                                    log.error("Failed to arm: " +
                                            throwable.getMessage());
                                })
                                .subscribe();
                    } else { //Armed

                        missionRaw.startMission()
                                .doOnComplete(() -> {
                                    log.debug("Mission started!");
                                })
                                .doOnError(throwable -> {
                                    log.error("Failed to start mission: " +
                                            throwable.getMessage());
                                })
                                .subscribe();
                    }
                }, throwable -> log.error("Failed to read armed state: " + throwable.getMessage()));
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