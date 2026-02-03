/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.universalcontroller;

import com.botts.controller.interfaces.IController;
import com.botts.controller.models.ControllerComponent;
import com.botts.controller.models.ControllerType;
import com.botts.impl.sensor.universalcontroller.helpers.ControllerCyclingAction;
import com.botts.impl.sensor.universalcontroller.helpers.ControllerMappingPreset;
import com.botts.impl.sensor.universalcontroller.helpers.UniversalControllerComponent;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.*;
import org.vast.swe.SWEHelper;

import java.lang.Boolean;

/**
 * Output specification and provider for {@link UniversalControllerSensor}.
 *
 * @author your_name
 * @since date
 */
public class UniversalControllerOutput extends AbstractSensorOutput<UniversalControllerSensor> implements Runnable {

    private static final String SENSOR_OUTPUT_NAME = "UniversalControllerOutput";
    private static final String SENSOR_OUTPUT_LABEL = "UniversalController";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Currently connected controller outputs.";

    private static final Logger logger = LoggerFactory.getLogger(UniversalControllerOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    // Stuff from config
    private int numControlStreams;

    public void setPrimaryControlStreamIndex(int primaryControlStreamIndex) {
        if(numControlStreams > primaryControlStreamIndex && primaryControlStreamIndex >= 0) {
            this.primaryControlStreamIndex = primaryControlStreamIndex;
        }
    }

    public void setPrimaryControllerIndex(int primaryControllerIndex) {
        if(parentSensor.allControllers.size() > primaryControllerIndex && primaryControllerIndex >= 0) {
            this.primaryControllerIndex = primaryControllerIndex;
        }
    }

    private int primaryControlStreamIndex;
    private int primaryControllerIndex;
    private long pollingRate;
    private ControllerLayerConfig controllerLayerConfig;

    private Thread worker;
    private final Object gamepadLock = new Object();

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    UniversalControllerOutput(UniversalControllerSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("UniversalControllerOutput created");
    }

    DataRecord createDataRecord() {
        int[] controllerIndices = new int[parentSensor.allControllers.size()];
        for (int i = 0; i < parentSensor.allControllers.size(); i++) {
            controllerIndices[i] = i;
        }

        int[] controlStreamIndices = new int[numControlStreams];
        for (int i = 0; i < numControlStreams; i++) {
            controlStreamIndices[i] = i;
        }

        SWEHelper sweFactory = new SWEHelper();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        return dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .updatable(true)
                .label(SENSOR_OUTPUT_LABEL)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("primaryControlStreamIndex", sweFactory.createCount()
                        .label("Primary Control Stream Index")
                        .description("Index of the primary control stream")
                        .definition(SWEHelper.getPropertyUri("PrimaryControlStreamIndex"))
                        .value(primaryControlStreamIndex)
                        .addAllowedValues(controlStreamIndices))
                .addField("numControlStreams", sweFactory.createCount()
                        .label("Num Control Streams")
                        .description("Number of Control Streams")
                        .definition(SWEHelper.getPropertyUri("NumControlStreams"))
                        .id("numControlStreams")
                        .value(numControlStreams))
                .addField("primaryControllerIndex", sweFactory.createCount()
                        .label("Primary Controller Index")
                        .description("Index of the primary controller in use")
                        .definition(SWEHelper.getPropertyUri("PrimaryControllerIndex"))
                        .value(primaryControllerIndex)
                        .addAllowedValues(controllerIndices))
                .addField("numGamepads", sweFactory.createCount()
                        .label("Num Gamepads")
                        .description("Number of connected gamepads")
                        .definition(SWEHelper.getPropertyUri("GamepadCount"))
                        .id("numGamepads"))
                .addField("gamepads", sweFactory.createArray()
                        .name("gamepads")
                        .label("Gamepads")
                        .description("List of connected gamepads.")
                        .definition(SWEHelper.getPropertyUri("GamepadArray"))
                        .withVariableSize("numGamepads")
                        .withElement("gamepad", sweFactory.createRecord()
                                .label("Gamepad")
                                .description("Gamepad Data")
                                .definition(SWEHelper.getPropertyUri("Gamepad"))
                                .addField("gamepadName", sweFactory.createText()
                                        .label("Gamepad Name")
                                        .definition("GamepadName"))
                                .addField("isPrimaryController", sweFactory.createBoolean()
                                        .label("Is Primary Controller")
                                        .definition(SWEHelper.getPropertyUri("IsPrimaryController"))
                                        .value(false))
                                .addField("numComponents", sweFactory.createCount()
                                        .label("Num Components")
                                        .description("Number of button and axis components on gamepad")
                                        .definition(SWEHelper.getPropertyUri("NumGamepadComponents"))
                                        .id("numComponents")
                                        .build())
                                .addField("gamepadComponents", sweFactory.createArray()
                                        .name("gamepadComponents")
                                        .label("Gamepad Components")
                                        .description("Data of Connected Gamepad Components")
                                        .definition(SWEHelper.getPropertyUri("GamepadComponentArray"))
                                        .withVariableSize("numComponents")
                                        .withElement("component", sweFactory.createRecord()
                                                .name("component")
                                                .label("Component")
                                                .description("Gamepad Component (A button, B button, X axis, etc.)")
                                                .definition(SWEHelper.getPropertyUri("GamepadComponent"))
                                                .addField("componentName", sweFactory.createText()
                                                        .label("Component Name")
                                                        .description("Name of component")
                                                        .definition(SWEHelper.getPropertyUri("ComponentName"))
                                                        .value(""))
                                                .addField("componentValue", sweFactory.createQuantity()
                                                        .label("Component Value")
                                                        .description("Value of component")
                                                        .definition(SWEHelper.getPropertyUri("ComponentValue"))
                                                        .dataType(DataType.FLOAT)
                                                        .value(0.0f)
                                                        .addAllowedInterval(-1.0f, 1.0f)))
                                        .build())))
                .build();

    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() throws SensorException {

        logger.debug("Initializing UniversalControllerOutput");

        // Get values from config
        primaryControllerIndex = parentSensor.getConfiguration().primaryControllerIndex;
        pollingRate = parentSensor.getConfiguration().pollingRate;
        controllerLayerConfig = parentSensor.getConfiguration().controllerLayerConfig;
        // Control stream cycling
        numControlStreams = parentSensor.getConfiguration().numControlStreams;
        primaryControlStreamIndex = parentSensor.getConfiguration().primaryControlStreamIndex;


        dataStruct = createDataRecord();

        logger.debug("Initializing UniversalControllerOutput Complete");
    }

    /**
     * Begins processing data for output
     */
    public void doStart() {

        // Instantiate a new worker thread
        worker = new Thread(this, this.name);

        // TODO: Perform other startup

        logger.info("Starting worker thread: {}", worker.getName());

        // Start the worker thread
        worker.start();
    }

    /**
     * Terminates processing data for output
     */
    public void doStop() {

        synchronized (processingLock) {

            stopProcessing = true;
        }

        try{
            parentSensor.cancelWiiMoteSearch();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        // stop all controller observers and disconnect all controllers
        for(IController controller : parentSensor.allControllers) {
            if(controller.getControllerData().getControllerType() == ControllerType.WIIMOTE) {
                parentSensor.cancelWiiMoteSearch();
            }
            controller.getObserver().doStop();
            controller.disconnect();
        }
        // TODO: Perform other shutdown procedures
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {

        return worker.isAlive();
    }

    @Override
    public DataComponent getRecordDescription() {

        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {

        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {

        long accumulator = 0;

        synchronized (histogramLock) {

            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {

                accumulator += timingHistogram[idx];
            }
        }

        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    /**
     * Updates the primary controller index or control stream index based on controller mappings.
     */
    public void applyHotkeys() {
        // Go through controller mapping
        for (ControllerMappingPreset preset : controllerLayerConfig.presets) {
            IController controller = parentSensor.allControllers.get(preset.controllerIndex);
            int componentsForCombination = preset.componentNames.size();

            if (preset.controllerCyclingAction.equals(ControllerCyclingAction.CYCLES_PRIMARY_CONTROLLER)) {
                for (ControllerComponent controllerComponent : controller.getControllerData().getOutputs()) {
                    if (preset.componentNames.contains(UniversalControllerComponent.fromString(controllerComponent.getName()))) {
                        if (controllerComponent.getValue() == 1.0f) {
                            componentsForCombination--;
                        }
                        if(componentsForCombination == 0) {
                            primaryControllerIndex++;
                            if (primaryControllerIndex >= parentSensor.allControllers.size()) {
                                primaryControllerIndex = 0;
                            }
                        }
                    }
                }
            }

            if (preset.controllerCyclingAction.equals(ControllerCyclingAction.OVERRIDES_PRIMARY_CONTROLLER)) {
                componentsForCombination = preset.componentNames.size();
                for(ControllerComponent controllerComponent : controller.getControllerData().getOutputs()) {
                    if(preset.componentNames.contains(UniversalControllerComponent.fromString(controllerComponent.getName()))) {
                        if(controllerComponent.getValue() == 1.0f) {
                            componentsForCombination--;
                        }
                        if(componentsForCombination == 0) {
                            primaryControllerIndex = parentSensor.allControllers.indexOf(controller);
                        }
                    }
                }
            }

            if (preset.controllerCyclingAction.equals(ControllerCyclingAction.PASS_PRIMARY_TO_NEXT)) {
                componentsForCombination = preset.componentNames.size();
                for (ControllerComponent controllerComponent : controller.getControllerData().getOutputs()) {
                    if(preset.componentNames.contains(UniversalControllerComponent.fromString(controllerComponent.getName()))) {
                        if(controllerComponent.getValue() == 1.0f) {
                            componentsForCombination--;
                        }
                        if(componentsForCombination == 0) {
                            if(primaryControllerIndex == preset.controllerIndex) {
                                primaryControllerIndex++;
                                if(primaryControllerIndex >= parentSensor.allControllers.size()) {
                                    primaryControllerIndex = 0;
                                }
                            }
                        }
                    }
                }
            }

            if (preset.controllerCyclingAction.equals(ControllerCyclingAction.CYCLES_CONTROL_STREAM)) {
                componentsForCombination = preset.componentNames.size();
                for(ControllerComponent controllerComponent : controller.getControllerData().getOutputs()) {
                    if(preset.componentNames.contains(UniversalControllerComponent.fromString(controllerComponent.getName()))) {
                        if(controllerComponent.getValue() == 1.0f) {
                            componentsForCombination--;
                        }
                        if(componentsForCombination == 0) {
                            primaryControlStreamIndex++;
                            if(primaryControlStreamIndex >= numControlStreams) {
                                primaryControlStreamIndex = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void run() {

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {

            while (processSets) {
                synchronized (gamepadLock) {

                    // adjust polling rate from config
                    Thread.sleep(pollingRate);

                    dataStruct = createDataRecord();
                    DataBlock dataBlock = dataStruct.createDataBlock();
                    dataStruct.setData(dataBlock);

                    synchronized (histogramLock) {

                        int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

                        // Get a sampling time for latest set based on previous set sampling time
                        timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

                        // Set latest sampling time to now
                        lastSetTimeMillis = timingHistogram[setIndex];
                    }

                    ++setCount;

                    // Update stuff configured from hotkeys
                    applyHotkeys();

                    double timestamp = System.currentTimeMillis() / 1000d;

                    int index = 0;

                    dataBlock.setDoubleValue(index++, timestamp);
                    dataBlock.setIntValue(index++, primaryControlStreamIndex);
                    dataBlock.setIntValue(index++, numControlStreams);
                    dataBlock.setIntValue(index++, primaryControllerIndex);
                    dataBlock.setIntValue(index++, parentSensor.allControllers.size());


                    var gamepadArray = (DataArrayImpl) dataStruct.getComponent("gamepads");
                    gamepadArray.updateSize();
                    dataBlock.updateAtomCount();

                    for (int i = 0; i < parentSensor.allControllers.size(); i++) {
                        IController controller = parentSensor.allControllers.get(i);
                        DataRecord gamepadDataBlock = (DataRecord) gamepadArray.getComponent(i);

                        // Set each element for underlying gamepad object
                        dataBlock.setStringValue(index++, controller.getControllerData().getName());
                        dataBlock.setBooleanValue(index++, i == primaryControllerIndex);
                        dataBlock.setIntValue(index++, controller.getControllerData().getOutputs().size());

                        var componentArray = ((DataArrayImpl) gamepadDataBlock.getComponent("gamepadComponents"));
                        componentArray.updateSize();
                        dataBlock.updateAtomCount();

                        // Set component data from controllerData outputs
                        for (int componentIndex = 0; componentIndex < controller.getControllerData().getOutputs().size(); componentIndex++) {
                            ControllerComponent componentData = controller.getControllerData().getOutputs().get(componentIndex);
                            String joinedName = componentData.getName().replace(" ", "");
                            dataBlock.setStringValue(index++, joinedName);
                            dataBlock.setFloatValue(index++, componentData.getValue());
                        }
                    }

                    dataBlock.updateAtomCount();

                    latestRecord = dataBlock;

                    latestRecordTime = System.currentTimeMillis();

                    eventHandler.publish(new DataEvent(latestRecordTime, UniversalControllerOutput.this, dataBlock));

                    synchronized (processingLock) {

                        processSets = !stopProcessing;
                    }
                }
            }

        } catch (Exception e) {

            logger.error("Error in worker thread: {}", Thread.currentThread().getName(), e);

        } finally {

            // Reset the flag so that when driver is restarted loop thread continues
            // until doStop called on the output again
            stopProcessing = false;

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }
}
