package org.sensorhub.impl.process.universalcontroller;

import org.sensorhub.impl.sensor.universalcontroller.helpers.UniversalControllerComponent;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.process.universalcontroller.helpers.AbstractControllerTaskingProcess;
import org.vast.process.ProcessException;

public class ControllerProcessTemplate extends AbstractControllerTaskingProcess {

    // TODO: Configure process information. URI, name, and process class
    public static final OSHProcessInfo INFO = new OSHProcessInfo(
            "controllerprocesstemplate",
            "Controller Process Template",
            null,
            ControllerProcessTemplate.class);

    // TODO: Create constructor matching superclass, with command outputs
    public ControllerProcessTemplate() {
        // Make sure to pass your process information to superclass
        super(INFO);

        // IMPORTANT!!!
        // fac includes predefined inputs and parameters
        // fac.inputData includes the "componentRecord" input which is your list of component data passed from the PrimaryControllerSelector process.
        // This is how you are able to access fac.getComponentValueInput(). PrimaryControllerSelector process must be added as a component in XML and linked to this process input.
        // fac.paramData includes the param for "controlStreamIndex" which is how this process knows whether to execute the commands based on the Universal Controller source's "primaryControlStreamIndex"

        // TODO: Add command outputs that will be linked to a control stream input.
        //  Make sure the structure of this output matches the control input exactly
        outputData.add("commandValue", fac.createQuantity()
                .label("Command 1")
                .description("Process output of command to send to control stream input")
                .build());
    }


    @Override
    public void updateOutputs() throws ProcessException {
        // TODO: Implement logic to update command outputs based on controller values.
        //  All controller component values are output as floats. Button values are true(1.0) or false(0.0)
        //  Axis components such as x, y, rx, or ry are on an axis from -1.0 -> 1.0.
        //  D-pad values are arranged in 8 directions from 0.0 -> 1.0 in increments of 1/8. You may also use the UniversalControllerComponent.DPadDirection enum values.
        boolean isXButtonPressed = fac.getComponentValueInput(UniversalControllerComponent.X_BUTTON) == 1.0f;

        float commandValue = 0.0f;
        if(isXButtonPressed) {
            // Update command data
            outputData.getComponent("commandValue").getData().setFloatValue(commandValue);
        }
    }

    // TODO: Use ProcessHelper to write XML for process (NOT IMPLEMENTED YET)

}
