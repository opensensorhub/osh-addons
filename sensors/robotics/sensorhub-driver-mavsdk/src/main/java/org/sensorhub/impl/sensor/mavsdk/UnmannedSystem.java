/***************************** begin license block ***************************
 the contents of this file are subject to the mozilla public license, v. 2.0.
 if a copy of the mpl was not distributed with this file, you can obtain one
 at http://mozilla.org/mpl/2.0/.

 software distributed under the license is distributed on an "as is" basis,
 without warranty of any kind, either express or implied. see the license
 for the specific language governing rights and limitations under the license.

 copyright (c) 2020-2025 botts innovative research, inc. all rights reserved.
 ******************************* end license block ***************************/
package org.sensorhub.impl.sensor.mavsdk;

import io.mavsdk.core.Core;
import io.mavsdk.mavlink_direct.MavlinkDirect;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.mavsdk.control.*;
import org.sensorhub.impl.sensor.mavsdk.outputs.*;
import org.sensorhub.impl.sensor.mavsdk.util.*;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class UnmannedSystem extends AbstractSensorModule<org.sensorhub.impl.sensor.mavsdk.UnmannedConfig> {
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

        locationOutput = new UnmannedLocationOutput(this);
        addOutput(locationOutput, false);
        locationOutput.doInit();

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

                });
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
