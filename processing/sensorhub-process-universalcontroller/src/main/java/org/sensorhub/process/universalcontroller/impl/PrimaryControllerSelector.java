package org.sensorhub.process.gamepadptz.impl;

import com.sample.impl.sensor.universalcontroller.helpers.UniversalControllerProcessHelper;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.data.DataBlockList;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockParallel;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;

public class PrimaryControllerSelector extends ExecutableProcessImpl {
    public static final OSHProcessInfo INFO = new OSHProcessInfo("primarycontrollerselector", "Primary Controller Selector Process", null, PrimaryControllerSelector.class);
    private DataRecord gamepadRecordInput;
    private DataRecord componentRecordOutput;
    final Object lock = new Object();
    public PrimaryControllerSelector() {
        super(INFO);

        UniversalControllerProcessHelper fac = new UniversalControllerProcessHelper();

        inputData.add(fac.createGamepadRecord().getName(), gamepadRecordInput = fac.createGamepadRecord());

        outputData.add(fac.getComponentRecord().getName(), componentRecordOutput = fac.getComponentRecord());
    }

    @Override
    public void execute() throws ProcessException {
        synchronized (lock) {
            int numGamepads = gamepadRecordInput.getComponent("numGamepads").getData().getIntValue();

            if (numGamepads > 0) {
                for (int gamepadIndex = 0; gamepadIndex < numGamepads; gamepadIndex++) {
                    DataBlockMixed gamepad = (DataBlockMixed) ((DataBlockList) gamepadRecordInput.getComponent("gamepads").getData()).get(gamepadIndex);

                    boolean isPrimaryController = gamepad.getBooleanValue(1);
                    int numComponents = gamepad.getIntValue(2);
                    DataBlockParallel componentArray = (DataBlockParallel) gamepad.getUnderlyingObject()[3];

                    if(isPrimaryController) {
                        componentRecordOutput.getComponent("primaryControlStreamIndex").getData().setIntValue(gamepadRecordInput.getComponent("primaryControlStreamIndex").getData().getIntValue());
                        componentRecordOutput.getComponent("numComponents").getData().setIntValue(numComponents);
                        componentRecordOutput.getComponent("gamepadComponents").getData().setUnderlyingObject(componentArray.getUnderlyingObject());

                        // May not need this line
                        outputData.getComponent(componentRecordOutput.getName()).setData(componentRecordOutput.getData());
                    }
                }
            }
        }
    }
}
