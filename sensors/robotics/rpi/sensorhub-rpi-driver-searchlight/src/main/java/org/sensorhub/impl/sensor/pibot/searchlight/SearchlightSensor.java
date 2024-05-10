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
package org.sensorhub.impl.sensor.pibot.searchlight;

import com.pi4j.io.gpio.*;
//import com.pi4j.io.gpio.exception.GpioPinNotProvisionedException;
//import org.sensorhub.impl.pibot.common.config.GpioEnum;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.impl.SpatialFrameImpl;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

/**
 * SearchlightSensor driver for the PiBot providing sensor description, control & output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Jan. 24, 2021
 */
public class SearchlightSensor extends AbstractSensorModule<SearchlightConfig> {

    private GpioPinDigitalOutput redLedPin;

    private GpioPinDigitalOutput greenLedPin;

    private GpioPinDigitalOutput blueLedPin;

    private SearchlightOutput output;

    private SearchlightState currentSearchlightState = SearchlightState.OFF;

//    private final GpioController gpio = GpioFactory.getInstance();

    @Override
    public void doInit() throws SensorHubException {

        LoggerFactory.getLogger(SearchlightSensor.class);

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:sentinel:pibot:", config.serialNumber);
        generateXmlID("SENTINEL_PIBOT", config.serialNumber);

        // Create and initialize output
        output = new SearchlightOutput(this);

        addOutput(output, false);

        output.init();

        // Create and initialize controls
        SearchlightControl control = new SearchlightControl(this);

        addControlInput(control);

        control.init();
    }

    @Override
    protected void updateSensorDescription() {

        synchronized (sensorDescLock) {

            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {

                sensorDescription.setDescription("YahBoom BST-03 V2.0: " +
                        "\nRGB LED module consisting of two 5mm  lamp beads. 3 LEDs " +
                        "(red, green, blue) are packaged per lamp bead in the RGB " +
                        "lamp module. We can mix different colors(256*256*256) by " +
                        "controlling the brightness of the three LEDs");

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

                SpatialFrame searchlightSensor = new SpatialFrameImpl();
                searchlightSensor.setId("SEARCHLIGHT_SENSOR_FRAME");
                searchlightSensor.setOrigin("44.45 mm on positive Y-Axis and 76.2 mm on the positive Z-Axis " +
                        "from the origin of the #LOCAL_FRAME");
                searchlightSensor.addAxis("x",
                        "The X axis is in the plane of the of the facet containing the apertures for " +
                                "the emitter and sensors and points to the right");
                searchlightSensor.addAxis("y",
                        "The Y axis is in the plane of the of the facet containing the apertures " +
                                "the emitter and sensors and points up");
                searchlightSensor.addAxis("z",
                        "The Z axis points towards the outside of the facet containing the apertures " +
                                "for emitter and sensors");
                ((PhysicalSystem) sensorDescription).addLocalReferenceFrame(searchlightSensor);
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

//        if (config.pinConfig.redLedPin != GpioEnum.PIN_UNSET) {
//
//            redLedPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.pinConfig.redLedPin.getValue()));
//
//        } else {
//
//            redLedPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03);
//        }
//        redLedPin.setShutdownOptions(true, PinState.LOW);
//
//        if (config.pinConfig.greenLedPin != GpioEnum.PIN_UNSET) {
//
//            greenLedPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.pinConfig.greenLedPin.getValue()));
//
//        } else {
//
//            greenLedPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02);
//        }
//        greenLedPin.setShutdownOptions(true, PinState.LOW);
//
//        if (config.pinConfig.blueLedPin != GpioEnum.PIN_UNSET) {
//
//            blueLedPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.pinConfig.blueLedPin.getValue()));
//
//        } else {
//
//            blueLedPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05);
//        }
//        blueLedPin.setShutdownOptions(true, PinState.LOW);
    }

    @Override
    public void doStop() throws SensorHubException {

        super.doStop();

        if (null != output) {

            output.stop();
        }

//        try {
//
//            if (redLedPin != null) {
//
//                redLedPin.setState(PinState.LOW);
//                gpio.unprovisionPin(redLedPin);
//                redLedPin = null;
//            }
//
//            if (greenLedPin != null) {
//
//                greenLedPin.setState(PinState.LOW);
//                gpio.unprovisionPin(greenLedPin);
//                greenLedPin = null;
//            }
//
//            if (blueLedPin != null) {
//
//                blueLedPin.setState(PinState.LOW);
//                gpio.unprovisionPin(blueLedPin);
//                blueLedPin = null;
//            }
//
//        } catch (GpioPinNotProvisionedException e) {
//
//            logger.error("Exception shutting down: { }", e);
//        }
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }

    protected SearchlightState getSearchlightState() {

        return currentSearchlightState;
    }

    protected void setSearchlightState(SearchlightState state){

        switch (state) {

            case WHITE:
                redLedPin.setState(PinState.HIGH);
                greenLedPin.setState(PinState.HIGH);
                blueLedPin.setState(PinState.HIGH);
                break;
            case RED:
                redLedPin.setState(PinState.HIGH);
                greenLedPin.setState(PinState.LOW);
                blueLedPin.setState(PinState.LOW);
                break;
            case MAGENTA:
                redLedPin.setState(PinState.HIGH);
                greenLedPin.setState(PinState.LOW);
                blueLedPin.setState(PinState.HIGH);
                break;
            case BLUE:
                redLedPin.setState(PinState.LOW);
                greenLedPin.setState(PinState.LOW);
                blueLedPin.setState(PinState.HIGH);
                break;
            case CYAN:
                redLedPin.setState(PinState.LOW);
                greenLedPin.setState(PinState.HIGH);
                blueLedPin.setState(PinState.HIGH);
                break;
            case GREEN:
                redLedPin.setState(PinState.LOW);
                greenLedPin.setState(PinState.HIGH);
                blueLedPin.setState(PinState.LOW);
                break;
            case YELLOW:
                redLedPin.setState(PinState.HIGH);
                greenLedPin.setState(PinState.HIGH);
                blueLedPin.setState(PinState.LOW);
                break;
            case OFF:
            default:
                redLedPin.setState(PinState.LOW);
                greenLedPin.setState(PinState.LOW);
                blueLedPin.setState(PinState.LOW);
                break;
        }

        currentSearchlightState = state;
    }
}
