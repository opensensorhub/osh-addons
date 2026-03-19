/***************************** begin license block ***************************
 the contents of this file are subject to the mozilla public license, v. 2.0.
 if a copy of the mpl was not distributed with this file, you can obtain one
 at http://mozilla.org/mpl/2.0/.

 software distributed under the license is distributed on an "as is" basis,
 without warranty of any kind, either express or implied. see the license
 for the specific language governing rights and limitations under the license.

 copyright (c) 2020-2025 botts innovative research, inc. all rights reserved.
 ******************************* end license block ***************************/
package org.sensorhub.impl.comm.mavsdk;

import io.mavsdk.action.Action;
import io.mavsdk.core.Core;
import io.mavsdk.mavlink_direct.MavlinkDirect;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.mavsdk.control.*;
import org.sensorhub.impl.comm.mavsdk.outputs.*;
import org.sensorhub.impl.comm.mavsdk.util.MavSdkServerHandler;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class UnmannedSystem extends AbstractSensorModule<UnmannedConfig> {
    static final String UID_PREFIX = "urn:osh:driver:mavsdk:";
    static final String XML_PREFIX = "MAVSDK_DRIVER_";

    private static final Logger logger = LoggerFactory.getLogger(UnmannedSystem.class);

    private UnmannedLocationOutput locationOutput;
    private UnmannedAttitudeOutput attitudeOutput;
    private UnmannedVelocityOutput velocityOutput;
    private UnmannedAccelerationOutput accelerationOutput;
    private UnmannedAngularVelocityOutput angularVelocityOutput;
    private UnmannedMagneticFieldOutput magneticFieldOutput;
    private UnmannedTemperatureOutput temperatureOutput;
    private UnmannedHealthOutput healthOutput;
    private UnmannedHealthGpsOutput healthGpsOutput;
    private UnmannedStatusEventOutput statusTextOutput;
    private UnmannedHomeOutput homeOutput;

    Thread processingThread;
    volatile boolean doProcessing = true;

    public static double destLatitude  = -35.355867;
    public static double destLongitude = 149.169245;
    public static float destAltitude = 30.0F;

    public static double homeLatitude  = -35.362219;
    public static double homeLongitude = 149.165082;
    public static float homeAltitude = 30.0F;

    public static double deltaSuccess =   0.000003;

    UnmannedControlTakeoff unmannedControlTakeoff;
    UnmannedControlLocation unmannedControlLocation;
    UnmannedControlLanding unmannedControlLanding;
    UnmannedControlQGCMission qgcMission; //qgc plans only
    UnmannedControlMission unmannedControlMission; //fully exposed schema
    UnmannedControlOffboard unmannedControlOffboard;
    UnmannedControlShell unmannedControlShell;
    UnmannedControlFlightMode unmannedControlFlightMode;
    UnmannedControlEnableLocation unmannedControlEnableLocation;
    UnmannedControlPauseMission unmannedControlPauseMission;
    UnmannedControlRTL unmannedControlRTL;

    MavSdkServerHandler mavsdkServer = new MavSdkServerHandler();

    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Generate identifiers
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // Create and initialize locationoutput
        locationOutput = new UnmannedLocationOutput(this);
        addOutput(locationOutput, false);
        locationOutput.doInit();

        // Create and initialize homeoutput
        homeOutput = new UnmannedHomeOutput(this);
        addOutput(homeOutput, false);
        homeOutput.doInit();

        attitudeOutput = new UnmannedAttitudeOutput(this);
        addOutput(attitudeOutput, false);
        attitudeOutput.doInit();

        velocityOutput = new UnmannedVelocityOutput(this);
        addOutput(velocityOutput, false);
        velocityOutput.doInit();

        accelerationOutput= new UnmannedAccelerationOutput(this);
        addOutput(accelerationOutput, false);
        accelerationOutput.doInit();

        angularVelocityOutput= new UnmannedAngularVelocityOutput(this);
        addOutput(angularVelocityOutput, false);
        angularVelocityOutput.doInit();

        magneticFieldOutput = new UnmannedMagneticFieldOutput(this);
        addOutput(magneticFieldOutput, false);
        magneticFieldOutput.doInit();

        temperatureOutput = new UnmannedTemperatureOutput(this);
        addOutput(temperatureOutput, false);
        temperatureOutput.doInit();

        healthOutput = new UnmannedHealthOutput(this);
        addOutput(healthOutput, false);
        healthOutput.doInit();

        healthGpsOutput = new UnmannedHealthGpsOutput(this);
        addOutput(healthGpsOutput, false);
        healthGpsOutput.doInit();

        statusTextOutput = new UnmannedStatusEventOutput(this);
        addOutput(statusTextOutput, false);
        statusTextOutput.doInit();

        // add Lat/Lon/Alt control stream

        this.unmannedControlLocation = new UnmannedControlLocation(this);
        addControlInput(this.unmannedControlLocation);
        unmannedControlLocation.init();

        this.unmannedControlTakeoff = new UnmannedControlTakeoff(this);
        addControlInput(this.unmannedControlTakeoff);
        unmannedControlTakeoff.init(this.unmannedControlLocation);

        this.unmannedControlLanding = new UnmannedControlLanding(this);
        addControlInput(this.unmannedControlLanding);
        unmannedControlLanding.init(this.unmannedControlLocation);

        this.qgcMission = new UnmannedControlQGCMission(this);
        addControlInput(this.qgcMission);
        qgcMission.init();

        this.unmannedControlMission = new UnmannedControlMission(this);
        addControlInput(this.unmannedControlMission);
        unmannedControlMission.init();

        this.unmannedControlRTL = new UnmannedControlRTL(this);
        addControlInput(this.unmannedControlRTL);
        unmannedControlRTL.init();

        this.unmannedControlOffboard = new UnmannedControlOffboard(this);
        addControlInput(this.unmannedControlOffboard);
        unmannedControlOffboard.init();

        this.unmannedControlShell = new UnmannedControlShell(this);
        addControlInput(this.unmannedControlShell);
        unmannedControlShell.init();

        this.unmannedControlFlightMode = new UnmannedControlFlightMode(this);
        addControlInput(this.unmannedControlFlightMode);
        unmannedControlFlightMode.init();

        this.unmannedControlEnableLocation= new UnmannedControlEnableLocation(this);
        addControlInput(this.unmannedControlEnableLocation);
        unmannedControlEnableLocation.init(this.unmannedControlLocation);

        this.unmannedControlPauseMission = new UnmannedControlPauseMission(this);
        addControlInput(this.unmannedControlPauseMission);
        unmannedControlPauseMission.init();
    }

    @Override
    public void doStart() throws SensorHubException {
        super.doStart();

        mavsdkServer.start(config);

        receiveDrone();

        //startProcessing();
    }

    @Override
    public void doStop() throws SensorHubException {
        super.doStop();

        mavsdkServer.stop();

        stopProcessing();
    }

    @Override
    public boolean isConnected() {
        return processingThread != null && processingThread.isAlive();
    }

    /**
     * Starts the data processing thread.
     * <p>
     * This method simulates sensor data collection and processing by generating data samples at regular intervals.
     */
    public void startProcessing() {
        doProcessing = true;

        processingThread = new Thread(() -> {
            while (doProcessing) {
                // Simulate data collection and processing
                //output.setData(System.currentTimeMillis(), "Sample Data");

                // Simulate a delay between data samples
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        processingThread.start();
    }

    /**
     * Signals the processing thread to stop.
     */
    public void stopProcessing() {
        doProcessing = false;
    }


    private void receiveDrone( ) {

        System.out.println("Listening for drone connection...");

        io.mavsdk.System drone = new io.mavsdk.System(config.SDKAddress, config.SDKPort);
        drone.getCore().getConnectionState()
                .filter(Core.ConnectionState::getIsConnected)
                .firstElement()
                .subscribe(state -> {
                    System.out.println("Drone connection detected.");

                    drone.getMavlinkDirect().initialize();

                    drone.getMavlinkDirect().sendMessage(new MavlinkDirect.MavlinkMessage(
                            "HEARTBEAT",
                            0,
                            0,
                            0,
                            0,
                           ""
                    )).doOnError( t -> {
                        System.out.println("Error sending heartbeat: " + t.getMessage());
                    });

                    unmannedControlLocation.setSystem(drone);
                    unmannedControlTakeoff.setSystem(drone);
                    unmannedControlLanding.setSystem(drone);
                    qgcMission.setSystem(drone);
                    unmannedControlMission.setSystem(drone);
                    unmannedControlRTL.setSystem(drone);
                    unmannedControlShell.setSystem(drone);
                    unmannedControlFlightMode.setSystem(drone);
                    unmannedControlPauseMission.setSystem(drone);
                    locationOutput.subscribe(drone);
                    homeOutput.subscribe(drone);
                    attitudeOutput.subscribe(drone);
                    accelerationOutput.subscribe(drone);
                    angularVelocityOutput.subscribe(drone);
                    healthOutput.subscribe(drone);
                    healthGpsOutput.subscribe(drone);
                    magneticFieldOutput.subscribe(drone);
                    temperatureOutput.subscribe(drone);
                    velocityOutput.subscribe(drone);
                    statusTextOutput.subscribe(drone);

                    //setUpScenario(drone);
                    //sendMission(drone);
                });
    }


    private static void setUpScenario( io.mavsdk.System drone ) {

        System.out.println("Setting up scenario...");

        CountDownLatch latch = new CountDownLatch(1);
        //downloadLog(drone);

        //subscribeTelemetry(drone);
        //printVideoStreamInfo(drone);

        //printParams(drone);
        //printHealth(drone);

        //drone.getOffboard().

        //printTransponderInfo(drone);

        drone.getAction().arm()
                .doOnComplete(() -> {

                    System.out.println("Arming...");

                    printDroneInfo(drone);

                })
                .doOnError(throwable -> {

                    System.out.println("Failed to arm: " + ((Action.ActionException) throwable).getCode());

                })
                .andThen(drone.getAction().setTakeoffAltitude(homeAltitude))
                .andThen(drone.getAction().takeoff()
                        .doOnComplete(() -> {

                            System.out.println("Taking off...");

                        })
                        .doOnError(throwable -> {

                            System.out.println("Failed to take off: " + ((Action.ActionException) throwable).getCode());

                        }))
                .delay(5, TimeUnit.SECONDS)
                .andThen(drone.getTelemetry().getPosition()
                        .filter(pos -> pos.getRelativeAltitudeM() >= homeAltitude)
                        .firstElement()
                        .ignoreElement()
                )
                .delay(5, TimeUnit.SECONDS)
                .andThen(drone.getAction().gotoLocation(destLatitude, destLongitude,
                                destAltitude + drone.getTelemetry().getPosition().blockingFirst().getAbsoluteAltitudeM(),
                                45.0F)
                        .doOnComplete( () -> {

                            System.out.println("Moving to target location");

                        }))
                .doOnError( throwable -> {

                    System.out.println("Failed to go to target: " + ((Action.ActionException) throwable).getCode());

                })
                .andThen(drone.getTelemetry().getPosition()
                        .filter(pos -> (abs(pos.getLatitudeDeg() - destLatitude) <= deltaSuccess && abs(pos.getLongitudeDeg() - destLongitude) <= deltaSuccess))
                        .firstElement()
                        .ignoreElement()
                )
                .delay( 8, TimeUnit.SECONDS )
                .andThen(drone.getAction().gotoLocation(homeLatitude, homeLongitude,
                        homeAltitude + drone.getTelemetry().getPosition().blockingFirst().getAbsoluteAltitudeM()
                        , 0.0F))
                .doOnComplete( () -> {

                    System.out.println("Moving to landing location");

                })
                .doOnError( throwable -> {

                    System.out.println("Failed to go to landing location: " + ((Action.ActionException) throwable).getCode());

                })
                .andThen(drone.getTelemetry().getPosition()
                        .filter(pos -> (abs(pos.getLatitudeDeg() - homeLatitude) <= deltaSuccess && abs(pos.getLongitudeDeg() - homeLongitude) <= deltaSuccess))
                        .firstElement()
                        .ignoreElement()
                )
                .andThen(drone.getAction().land().doOnComplete(() -> {

                            System.out.println("Landing...");

                        })
                        .doOnError(throwable -> {

                            System.out.println("Failed to land: " + ((Action.ActionException) throwable).getCode());

                        }))
                .subscribe(latch::countDown, throwable -> {

                    latch.countDown();

                });

        try {
            latch.await();
        } catch (InterruptedException ignored) {
            // This is expected
        }
    }


    public static void printDroneInfo( io.mavsdk.System drone ) {

        drone.getInfo().getVersion()
                .flatMap(version -> {
                    System.out.println("Flight software version: " +
                            version.getFlightSwMajor() + "." +
                            version.getFlightSwMinor() + "." +
                            version.getFlightSwPatch());

                    // Optionally infer autopilot from version or Git hash
                    return drone.getInfo().getIdentification();
                })
                .subscribe(identification -> {
                    System.out.println("Hardware UID: " + identification.getHardwareUid());
                }, throwable -> {
                    System.err.println("Failed to get info: " + throwable.getMessage());
                });

    }

}
