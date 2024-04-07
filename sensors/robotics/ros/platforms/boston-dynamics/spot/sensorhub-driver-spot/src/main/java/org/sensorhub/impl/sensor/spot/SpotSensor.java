/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 - 2024 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.spot;

import org.ros.RosCore;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.impl.ros.output.RosSensorOutput;
import org.sensorhub.impl.ros.output.RosVideoOutput;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.spot.config.SpotConfig;
import org.sensorhub.impl.sensor.spot.config.SpotFrameResConfig;
import org.sensorhub.impl.sensor.spot.control.*;
import org.sensorhub.impl.sensor.spot.outputs.cameras.DepthOutput;
import org.sensorhub.impl.sensor.spot.outputs.cameras.ImageOutput;
import org.sensorhub.impl.sensor.spot.outputs.cameras.SensorPosition;
import org.sensorhub.impl.sensor.spot.outputs.status.*;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sensor driver for the ... providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 *
 * @author Nick Garay
 * @since Sept. 25, 2023
 */
public class SpotSensor extends AbstractSensorModule<SpotConfig> {

    private final List<BaseSpotControl> controls = new ArrayList<>();

    private RosCore rosCore;

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        logger = LoggerFactory.getLogger(SpotSensor.class);

        if (config.rosMaster.spinRosMaster) {

            // Create a publicly available ros core in port rosHostPort.
            rosCore = RosCore.newPublic(config.rosMaster.rosCorePort);

            //This will start the created java ROS Core.
            rosCore.start();

            try {

                rosCore.awaitStart(5, TimeUnit.SECONDS);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new SensorHubException("Failed to start ROS core");
            }
        }

        // Generate identifiers
        generateUniqueID("urn:osh:system:boston_dynamics:spot:", config.serialNumber);
        generateXmlID("BOSTON_DYNAMICS_SPOT_", config.serialNumber);

        // Create and initialize outputs
        initializeStatusOutputs();

        initializeRgbCamsOutputs();

        initializeDepthCamsOutputs();

        initializeControls();
    }

    @Override
    public void doStart() throws SensorHubException {

        for (IStreamingDataInterface output : getOutputs().values()) {

            if (output instanceof RosVideoOutput) {

                ((RosVideoOutput<?>) output).doStart();

            } else {

                ((RosSensorOutput<?>) output).doStart();
            }
        }

        for (BaseSpotControl control: controls) {

            control.doStart();
        }
    }

    @Override
    public void doStop() throws SensorHubException {

        for (IStreamingDataInterface output : getOutputs().values()) {

            if (output instanceof RosVideoOutput) {

                ((RosVideoOutput<?>) output).doStop();

            } else {

                ((RosSensorOutput<?>) output).doStop();
            }
        }

        for (BaseSpotControl control: controls) {

            control.doStop();
        }

        if (rosCore != null) {

            rosCore.shutdown();
        }
    }

    @Override
    public boolean isConnected() {

        boolean outputsAlive = false;

        // Determine if sensor is connected
        for (IStreamingDataInterface output : getOutputs().values()) {

            outputsAlive = ((RosSensorOutput<?>) output).isAlive();

            if (!outputsAlive) {
                break;
            }
        }

        return outputsAlive;
    }

    private void initializeControls() {

        if (config.spotAllowMotionConfig.enabled) {

            BaseSpotControl control = new SpotAllowMotionControl(this);

            control.doInit();

            addControlInput(control);

            controls.add(control);
        }

        if (config.spotCancelMotionConfig.enabled) {

            BaseSpotControl control = new SpotCancelMotionControl(this);

            control.doInit();

            addControlInput(control);

            controls.add(control);
        }

        if (config.spotEStopConfig.enabled) {

            BaseSpotControl control = new SpotEStopControl(this);

            control.doInit();

            addControlInput(control);

            controls.add(control);
        }

        if (config.spotLeaseConfig.enabled) {

            BaseSpotControl control = new SpotLeaseControl(this);

            control.doInit();

            addControlInput(control);

            controls.add(control);
        }

        if (config.spotMotionConfig.enabled) {

            BaseSpotControl control = new SpotMotionControl(this);

            control.doInit();

            addControlInput(control);

            controls.add(control);
        }

        if (config.spotPoseConfig.enabled) {

            BaseSpotControl control = new SpotPoseControl(this);

            control.doInit();

            addControlInput(control);

            controls.add(control);
        }

        if (config.spotPowerConfig.enabled) {

            BaseSpotControl control = new SpotPowerControl(this);

            control.doInit();

            addControlInput(control);

            controls.add(control);
        }

        if (config.spotSitStandConfig.enabled) {

            BaseSpotControl control = new SpotSitStandControl(this);

            control.doInit();

            addControlInput(control);

            controls.add(control);
        }

        logger.info("Controls initialized");
    }

    private void initializeStatusOutputs() {

        if (config.spotStatusConfig.batteryStatusOutput.enabled) {

            BatteryStatusOutput output = new BatteryStatusOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        if (config.spotStatusConfig.behaviorFaultsStatusOutput.enabled) {

            BehaviorFaultsOutput output = new BehaviorFaultsOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        if (config.spotStatusConfig.eStopStatusOutput.enabled) {

            EStopStatusOutput output = new EStopStatusOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        if (config.spotStatusConfig.feedbackStatusOutput.enabled) {

            FeedbackOutput output = new FeedbackOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        if (config.spotStatusConfig.feetPositionStatusOutput.enabled) {

            FeetPositionOutput output = new FeetPositionOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        if (config.spotStatusConfig.leaseStatusOutput.enabled) {

            LeaseStatusOutput output = new LeaseStatusOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        if (config.spotStatusConfig.metricsOutput.enabled) {

            MetricsOutput output = new MetricsOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        if (config.spotStatusConfig.mobilityParamsStatusOutput.enabled) {

            MobilityParametersOutput output = new MobilityParametersOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        if (config.spotStatusConfig.odometryStatusOutput.enabled) {

            OdometryOutput output = new OdometryOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        if (config.spotStatusConfig.powerStateStatusOutput.enabled) {

            PowerStateOutput output = new PowerStateOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        if (config.spotStatusConfig.systemFaultsStatusOutput.enabled) {

            SystemFaultsOutput output = new SystemFaultsOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        if (config.spotStatusConfig.wiFiStatusOutput.enabled) {

            WiFiStatusOutput output = new WiFiStatusOutput(this);

            addOutput(output, true);

            output.doInit();
        }

        logger.info("Status outputs initialized");
    }

    private void initializeRgbCamsOutputs() {

        SpotFrameResConfig frameConfig = config.spotRgbCamsConfig.frameConfig;

        if (config.spotRgbCamsConfig.frontLeftCamera.enabled) {

            ImageOutput output = new ImageOutput(this, SensorPosition.FRONT_LEFT,
                    config.spotRgbCamsConfig.frontLeftCamera.topic,
                    frameConfig);

            addOutput(output, false);

            output.doInit();
        }
        if (config.spotRgbCamsConfig.frontRightCamera.enabled) {

            ImageOutput output = new ImageOutput(this, SensorPosition.FRONT_RIGHT,
                    config.spotRgbCamsConfig.frontRightCamera.topic,
                    frameConfig);

            addOutput(output, false);

            output.doInit();
        }
        if (config.spotRgbCamsConfig.leftCamera.enabled) {

            ImageOutput output = new ImageOutput(this, SensorPosition.LEFT,
                    config.spotRgbCamsConfig.leftCamera.topic,
                    frameConfig);

            addOutput(output, false);

            output.doInit();
        }
        if (config.spotRgbCamsConfig.rightCamera.enabled) {

            ImageOutput output = new ImageOutput(this, SensorPosition.RIGHT,
                    config.spotRgbCamsConfig.rightCamera.topic,
                    frameConfig);

            addOutput(output, false);

            output.doInit();
        }
        if (config.spotRgbCamsConfig.backCamera.enabled) {

            ImageOutput output = new ImageOutput(this, SensorPosition.BACK,
                    config.spotRgbCamsConfig.backCamera.topic,
                    frameConfig);

            addOutput(output, false);

            output.doInit();
        }

        logger.info("RGB camera outputs initialized");
    }

    private void initializeDepthCamsOutputs() {

        SpotFrameResConfig frameConfig = config.spotDepthCamsConfig.frameConfig;

        if (config.spotDepthCamsConfig.frontLeftCamera.enabled) {

            DepthOutput output = new DepthOutput(this, SensorPosition.FRONT_LEFT,
                    config.spotDepthCamsConfig.frontLeftCamera.topic,
                    frameConfig);

            addOutput(output, false);

            output.doInit();
        }
        if (config.spotDepthCamsConfig.frontRightCamera.enabled) {

            DepthOutput output = new DepthOutput(this, SensorPosition.FRONT_RIGHT,
                    config.spotDepthCamsConfig.frontRightCamera.topic,
                    frameConfig);

            addOutput(output, false);

            output.doInit();
        }
        if (config.spotDepthCamsConfig.leftCamera.enabled) {

            DepthOutput output = new DepthOutput(this, SensorPosition.LEFT,
                    config.spotDepthCamsConfig.leftCamera.topic,
                    frameConfig);

            addOutput(output, false);

            output.doInit();
        }
        if (config.spotDepthCamsConfig.rightCamera.enabled) {

            DepthOutput output = new DepthOutput(this, SensorPosition.RIGHT,
                    config.spotDepthCamsConfig.rightCamera.topic,
                    frameConfig);

            addOutput(output, false);

            output.doInit();
        }
        if (config.spotDepthCamsConfig.backCamera.enabled) {

            DepthOutput output = new DepthOutput(this, SensorPosition.BACK,
                    config.spotDepthCamsConfig.backCamera.topic,
                    frameConfig);

            addOutput(output, false);

            output.doInit();
        }

        logger.info("Depth camera outputs initialized");
    }
}
