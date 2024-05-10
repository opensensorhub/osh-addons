/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * See the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) 2021-2024 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.sensor.pibot.camera;

import com.pi4j.io.gpio.*;
import org.sensorhub.impl.pibot.common.config.GpioEnum;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.impl.SpatialFrameImpl;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

/**
 * CameraSensor driver for the PiBot providing sensor description, control & output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Feb. 16, 2021
 */
public class CameraSensor extends AbstractSensorModule<CameraConfig> {

    private GpioPinDigitalOutput panPin;

    private GpioPinDigitalOutput tiltPin;

    private CameraOutput output;

    private final GpioController gpio = GpioFactory.getInstance();

    @Override
    public void doInit() throws SensorHubException {

        LoggerFactory.getLogger(CameraSensor.class);

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:sentinel:pibot:", config.serialNumber);
        generateXmlID("SENTINEL_PIBOT", config.serialNumber);

        // Create and initialize output
        output = new CameraOutput(this);

        addOutput(output, false);

        output.init();

        // Create and initialize controls
        CameraControl control = new CameraControl(this);

        addControlInput(control);

        control.init();
    }

    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("HD Camera");

                // Reference Frame
                SpatialFrame localRefFrame = new SpatialFrameImpl();
                localRefFrame.setId("LOCAL_FRAME");
                localRefFrame
                        .setOrigin("Center of the PiBot approximately 122.5 mm from plane extending " +
                                "perpendicular to front surface of frame, 117 mm from planes extended " +
                                "from side surfaces of the frame, and 89 mm from the plane of contact with " +
                                "the ground");
                localRefFrame.addAxis("x",
                        "The X axis is in the plane of the of the front facet points to the right");
                localRefFrame.addAxis("y",
                        "The Y axis is in the plane of the of the front facet and points up");
                localRefFrame.addAxis("z",
                        "The Z axis points towards the outside of the front facet");
                ((PhysicalSystem) sensorDescription).addLocalReferenceFrame(localRefFrame);

                SpatialFrame cameraSensor = new SpatialFrameImpl();
                cameraSensor.setId("CAMERA_SENSOR_FRAME");
                cameraSensor.setOrigin("63.5 mm on the positive Y-Axis and 38.1 mm on the negative Z-Axis " +
                        "from the origin of the #LOCAL_FRAME");
                cameraSensor.addAxis("x",
                        "The X axis is in the plane of the of the facet containing the apertures for " +
                                "the sensors and points to the right");
                cameraSensor.addAxis("y",
                        "The Y axis is in the plane of the of the facet containing the apertures " +
                                "the sensors and points up");
                cameraSensor.addAxis("z",
                        "The Z axis points towards the outside of the facet containing the apertures " +
                                "for the sensors");
                ((PhysicalSystem) sensorDescription).addLocalReferenceFrame(cameraSensor);
            }

            SMLHelper helper = new SMLHelper(sensorDescription);

            helper.addSerialNumber(config.serialNumber);
        }
    }

    @Override
    public void doStart() throws SensorHubException {

        super.doStart();

        if(null != output) {

            try {

                output.start();

            } catch (SensorException e) {

                logger.error("Failed to start {} due to {}", output.getName(), e);
            }
        }

        if (config.pinConfig.panServoPin != GpioEnum.PIN_UNSET) {

            panPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.pinConfig.panServoPin.getValue()));

        } else {

            panPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_14);
        }
        panPin.setShutdownOptions(true, PinState.LOW);

        if (config.pinConfig.tiltServoPin != GpioEnum.PIN_UNSET) {

            tiltPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.pinConfig.tiltServoPin.getValue()));

        } else {

            tiltPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_13);
        }
        tiltPin.setShutdownOptions(true, PinState.LOW);
    }

    @Override
    public void doStop() throws SensorHubException {

        super.doStop();

        if (null != output) {

            output.stop();
        }

        if (panPin != null) {

            panPin.setState(PinState.LOW);
            gpio.unprovisionPin(panPin);
            panPin = null;
        }

        if (tiltPin != null) {

            tiltPin.setState(PinState.LOW);
            gpio.unprovisionPin(tiltPin);
            tiltPin = null;
        }
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }

    /**
     * Maps angle to PWM signal for SG90 Servos
     * 20 ms (50Hz) PWM Period
     * 1 - 2 MS Duty Cycle
     * 1.0 ms pulse -  90.0° - maps to   0° (right)
     * 1.5 ms pulse -   0.0° - maps to  90° (center)
     * 2.0 ms pulse - -90.0° - maps to 180° (left)
     *
     * Using busy wait as Java Sleep Timer for threads calls exceed
     * desired sleep time due to invocation time.  In addition,
     * granularity of sleep is bound by thread scheduler's interrupt
     * period (1ms in Linux and approx 10-15 ms in Windows).
     *
     * High level of pulse is calculated between 0.5 ms and 2.5ms.
     *
     * @param servoPin The pin to operate on
     * @param angle The angle of rotation in range [0 - 180]
     */
    private void rotateTo(GpioPinDigitalOutput servoPin, double angle) {

        logger.info("pin: {} angle: {}", servoPin.getName(), angle);

        long pulseWidthMicros = Math.round(angle * 11) + 500;

        logger.info("pulseWidth: {}", pulseWidthMicros);

        for (int i = 0; i <= 15; ++i) {

            servoPin.setState(PinState.HIGH);

            long start = System.nanoTime();
            while (System.nanoTime() - start < pulseWidthMicros * 1000) ;

            servoPin.setState(PinState.LOW);

            start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < (20 - pulseWidthMicros / 1000)) ;
        }
    }

    /**
     * Rotates the pan servo to prescribed angle
     * @param angle angle to turn to
     */
    public void panTo(double angle) {

        rotateTo(panPin, angle);
    }

    /**
     * Rotates the tilt servo to prescribed angle
     * @param angle angle to turn to
     */
    public void tiltTo(double angle) {

        rotateTo(tiltPin, angle);
    }
}
