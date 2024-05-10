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
package org.sensorhub.impl.sensor.pibot.ultrasound;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.exception.GpioPinNotProvisionedException;
import org.sensorhub.impl.pibot.common.config.GpioEnum;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.impl.SpatialFrameImpl;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

/**
 * UltrasonicSensor driver for the PiBot providing sensor description, control & output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Jan. 24, 2021
 */
public class UltrasonicSensor extends AbstractSensorModule<UltrasonicConfig> {

    private GpioPinDigitalOutput servoPin;

    private GpioPinDigitalOutput triggerPin;

    private GpioPinDigitalInput echoPin;

    private UltrasonicOutput output;

    private final GpioController gpio = GpioFactory.getInstance();

    private double detectedRange = Double.NaN;

    @Override
    public void doInit() throws SensorHubException {

        logger = LoggerFactory.getLogger(UltrasonicSensor.class);

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:sentinel:pibot:", config.serialNumber);
        generateXmlID("SENTINEL_PIBOT", config.serialNumber);

        // Create and initialize output
        output = new UltrasonicOutput(this);

        addOutput(output, false);

        output.init();

        // Create and initialize controls
        UltrasonicControl control = new UltrasonicControl(this);

        addControlInput(control);

        control.init();
    }

    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("YahBoom Ultrasonic Sensor V2.0: " +
                        "\nTransmitter requires high signal for at least 10 μs to " +
                        "the trigger pin (15 μs recommended)." +
                        "\n Receiver: After the ranging function is triggered, " +
                        "the module will automatically send out 8 40 kHz ultrasonic " +
                        "pulses and automatically detect whether there is a signal " +
                        "return. This step is automatically completed by the module. " +
                        "The ECHO pin will output a high level once an echo signal is " +
                        "detected. The high level duration is the time from the " +
                        "transmission to the return of the ultrasonic wave");

                // Reference Frame
                SpatialFrame localRefFrame = new SpatialFrameImpl();
                localRefFrame.setId("LOCAL_FRAME");
                localRefFrame
                        .setOrigin("Center of the PiBot approximately 122.5 mm from plane extending " +
                                "perpendicular to front surface of frame, 117 mm in from planes extended " +
                                "from side surfaces of the frame, and 89 mm from the plane of contact with " +
                                "the ground");
                localRefFrame.addAxis("x",
                        "The X axis is in the plane of the of the front facet points to the right");
                localRefFrame.addAxis("y",
                        "The Y axis is in the plane of the of the front facet and points up");
                localRefFrame.addAxis("z",
                        "The Z axis points towards the outside of the front facet");
                ((PhysicalSystem) sensorDescription).addLocalReferenceFrame(localRefFrame);

                SpatialFrame ultrasonicSensor = new SpatialFrameImpl();
                ultrasonicSensor.setId("ULTRASONIC_SENSOR_FRAME");
                ultrasonicSensor.setOrigin("38.1 mm on positive Y-Axis and 76.2 mm on the positive Z-Axis " +
                        "from the origin of the #LOCAL_FRAME");
                ultrasonicSensor.addAxis("x",
                        "The X axis is in the plane of the of the facet containing the apertures for " +
                                "the emitter and sensors and points to the right");
                ultrasonicSensor.addAxis("y",
                        "The Y axis is in the plane of the of the facet containing the apertures " +
                                "the emitter and sensors and points up");
                ultrasonicSensor.addAxis("z",
                        "The Z axis points towards the outside of the facet containing the apertures " +
                                "for emitter and sensors");
                ((PhysicalSystem) sensorDescription).addLocalReferenceFrame(ultrasonicSensor);
            }

            SMLHelper helper = new SMLHelper(sensorDescription);

            helper.addSerialNumber(config.serialNumber);
        }
    }

    @Override
    public void doStart() throws SensorHubException {

        super.doStart();

        if (null != output) {

            // Allocate necessary resources and start outputs
            output.start();
        }

        if (config.pinConfig.servoPin != GpioEnum.PIN_UNSET) {

            servoPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.pinConfig.servoPin.getValue()));

        } else {

            servoPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04);
        }
        servoPin.setShutdownOptions(true, PinState.LOW);

        if (config.pinConfig.triggerPin != GpioEnum.PIN_UNSET) {

            triggerPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.pinConfig.triggerPin.getValue()));

        } else {

            triggerPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02);
        }
        triggerPin.setShutdownOptions(true, PinState.LOW);

        if (config.pinConfig.echoPin != GpioEnum.PIN_UNSET) {

            echoPin = gpio.provisionDigitalInputPin(RaspiPin.getPinByAddress(config.pinConfig.echoPin.getValue()));

        } else {

            echoPin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_05);
        }
        echoPin.setShutdownOptions(true, PinState.LOW);
    }

    @Override
    public void doStop() throws SensorHubException {

        super.doStop();

        if (null != output) {

            output.stop();
        }

        try {

            if (servoPin != null) {

                servoPin.setState(PinState.LOW);
                gpio.unprovisionPin(servoPin);
                servoPin = null;
            }

            if (triggerPin != null) {

                triggerPin.setState(PinState.LOW);
                gpio.unprovisionPin(triggerPin);
                triggerPin = null;
            }

            if (echoPin != null) {

                gpio.unprovisionPin(echoPin);
                echoPin = null;
            }

        } catch (GpioPinNotProvisionedException e) {

            logger.error("Exception shutting down: { }", e);
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
     * @param angle The angle of rotation in range [0 - 180]
     */
    public void rotateTo(double angle) {

        logger.info("angle: {}", angle);

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
     * Performs range detection
     */
    public void detectRange() {

        // Set initial state to LOW
        triggerPin.setState(PinState.LOW);

        // Wait for 2 microseconds
        long start = System.nanoTime() / 1000;
        while ((System.nanoTime() / 1000) - start < 2);

        triggerPin.setState(PinState.HIGH);

        // Trigger for 15 microseconds
        start = System.nanoTime() / 1000;
        while ((System.nanoTime() / 1000) - start < 15);

        // Reset to low
        triggerPin.setState(PinState.LOW);

        while(echoPin.getState() == PinState.LOW);

        // Get the start time in microseconds
        start = System.nanoTime() / 1000;

        while(echoPin.getState() == PinState.HIGH);

        // Get the stop time in microseconds
        long stop = System.nanoTime() / 1000;

        // Compute distance converting microseconds to seconds multiplying
        // by approx speed of sound at sea level and dividing by two for
        // round trip of sonar
        detectedRange = ((double)(stop - start) / 1000000) * (34300.0 / 2);

        logger.info("Distance: {} cm", detectedRange);
    }

    protected double getDetectedRange() {

        return detectedRange;
    }
}
