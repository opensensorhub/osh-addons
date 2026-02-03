package org.sensorhub.impl.sensor.universalcontroller.helpers;

import net.opengis.swe.v20.*;
import org.vast.data.*;
import org.vast.swe.SWEHelper;

import java.util.Objects;

public class UniversalControllerProcessHelper extends SWEHelper {
    
    private DataRecord componentRecord;
    private Count controlStreamIndexParameter;
    private DataBlock outputRecord;
    
    public UniversalControllerProcessHelper() {
        this.componentRecord = createComponentRecord();
    }

    public DataRecord getComponentRecord() {
        return this.componentRecord;
    }

    public int getPrimaryControlStreamIndexInput() {
        return this.componentRecord.getComponent("primaryControlStreamIndex").getData().getIntValue();
    }

    public int getNumComponentsInput() {
        return this.componentRecord.getComponent("numComponents").getData().getIntValue();
    }

    public void setComponentRecord(DataBlock dataBlock) {
        this.componentRecord.setData(dataBlock);
    }

    public boolean hasComponent(String componentName) {
        if(this.getComponentRecord().getComponent("gamepadComponents") != null) {
            DataBlockParallel componentArray = ((DataBlockParallel) this.componentRecord.getComponent("gamepadComponents").getData());

            int numComponents = getNumComponentsInput();

            for (int j = 0; j < numComponents; j++) {
                String[] componentNames = (String[]) componentArray.getUnderlyingObject()[0].getUnderlyingObject();

                if (Objects.equals(componentNames[j], componentName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasComponent(UniversalControllerComponent component) {
        return hasComponent(component.getComponentName());
    }

    public float getComponentValueInput(String componentName) {
        float componentValue = 0.0f;
        if(this.getComponentRecord().getComponent("gamepadComponents") != null) {

            DataBlockParallel componentArray = ((DataBlockParallel) this.componentRecord.getComponent("gamepadComponents").getData());

            int numComponents = getNumComponentsInput();

            for (int j = 0; j < numComponents; j++) {
                String[] componentNames = (String[]) componentArray.getUnderlyingObject()[0].getUnderlyingObject();

                boolean isFloat = false;
                float[] componentFloatValues = new float[0];
                double[] componentDoubleValues = new double[0];

                if(componentArray.getUnderlyingObject()[1].getUnderlyingObject() instanceof float[]) {
                    componentFloatValues = (float[]) componentArray.getUnderlyingObject()[1].getUnderlyingObject();
                    isFloat = true;
                } else {
                    componentDoubleValues = (double[]) componentArray.getUnderlyingObject()[1].getUnderlyingObject();
                }

                if (Objects.equals(componentNames[j], componentName)) {
                    return isFloat ? componentFloatValues[j] : (float) componentDoubleValues[j];
                }
            }
        }
        return componentValue;
    }

    public float getComponentValueInput(UniversalControllerComponent component) {
        return getComponentValueInput(component.getComponentName());
    }

    private DataRecord createComponentRecord() {
        return createRecord()
                .name("componentRecord")
                .label("Process Input Record")
                .description("Component Data of Primary Controller")
                .definition("ControllerTaskingProcessInput")
                .updatable(true)
                .addField("primaryControlStreamIndex", createCount()
                        .label("Primary Control Stream Index")
                        .description("Index of the primary control stream")
                        .definition(SWEHelper.getPropertyUri("PrimaryControlStreamIndex"))
                        .value(0))
                .addField("numComponents", createCount()
                        .label("Num Components")
                        .description("Number of button and axis components on gamepad")
                        .definition(SWEHelper.getPropertyUri("NumGamepadComponents"))
                        .id("numComponents")
                        .value(0))
                .addField("gamepadComponents", createArray()
                        .name("gamepadComponents")
                        .label("Gamepad Components")
                        .description("Data of Connected Gamepad Components")
                        .definition(SWEHelper.getPropertyUri("GamepadComponentArray"))
                        .withVariableSize("numComponents")
                        .withElement("component", createRecord()
                                .name("component")
                                .label("Component")
                                .description("Gamepad Component (A button, B button, X axis, etc.)")
                                .definition(SWEHelper.getPropertyUri("GamepadComponent"))
                                .addField("componentName", createText()
                                        .label("Component Name")
                                        .description("Name of component")
                                        .definition(SWEHelper.getPropertyUri("ComponentName"))
                                        .value(""))
                                .addField("componentValue", createQuantity()
                                        .label("Component Value")
                                        .description("Value of component")
                                        .definition(SWEHelper.getPropertyUri("ComponentValue"))
                                        .dataType(DataType.FLOAT)
                                        .value(0.0f)
                                        .addAllowedInterval(-1.0f, 1.0f))))
                .build();
    }
    
    public Count createControlStreamIndexParameter() {
        return createCount()
                .name("controlStreamIndex")
                .label("Control Stream Index")
                .description("Index of this process's control stream")
                .definition(getPropertyUri("ControlStream"))
                .value(-1)
                .build();
    }


    public DataRecord createGamepadRecord() {
        return createRecord()
                .name("gamepadRecord")
                .label("Gamepad Output Record")
                .definition(SWEHelper.getPropertyUri("GamepadOutputRecord"))
                .updatable(true)
                .addField("primaryControlStreamIndex", createCount()
                        .label("Primary Control Stream Index")
                        .description("Index of the primary control stream")
                        .definition(SWEHelper.getPropertyUri("PrimaryControlStreamIndex")))
                .addField("numControlStreams", createCount()
                        .label("Num Control Streams")
                        .description("Number of Control Streams")
                        .definition(SWEHelper.getPropertyUri("NumControlStreams")))
                .addField("numGamepads", createCount()
                        .name("numGamepads")
                        .label("Num Gamepads")
                        .description("Number of connected gamepads")
                        .definition(SWEHelper.getPropertyUri("GamepadCount"))
                        .id("numGamepads").build())
                .addField("gamepads", createArray()
                        .name("gamepads")
                        .label("Gamepads")
                        .description("List of connected gamepads.")
                        .definition(SWEHelper.getPropertyUri("GamepadArray"))
                        .withVariableSize("numGamepads")
                        .withElement("gamepad", createRecord()
                                .label("Gamepad")
                                .description("Gamepad Data")
                                .definition(SWEHelper.getPropertyUri("Gamepad"))
                                .addField("gamepadName", createText()
                                        .label("Gamepad Name")
                                        .definition("GamepadName"))
                                .addField("isPrimaryController", createBoolean()
                                        .label("Is Primary Controller")
                                        .definition(SWEHelper.getPropertyUri("IsPrimaryController"))
                                        .value(false))
                                .addField("numComponents", createCount()
                                        .label("Num Components")
                                        .description("Number of button and axis components on gamepad")
                                        .definition(SWEHelper.getPropertyUri("NumGamepadComponents"))
                                        .id("numComponents")
                                        .build())
                                .addField("gamepadComponents", createArray()
                                        .name("gamepadComponents")
                                        .label("Gamepad Components")
                                        .description("Data of Connected Gamepad Components")
                                        .definition(SWEHelper.getPropertyUri("GamepadComponentArray"))
                                        .withVariableSize("numComponents")
                                        .withElement("component", createRecord()
                                                .name("component")
                                                .label("Component")
                                                .description("Gamepad Component (A button, B button, X axis, etc.)")
                                                .definition(SWEHelper.getPropertyUri("GamepadComponent"))
                                                .addField("componentName", createText()
                                                        .label("Component Name")
                                                        .description("Name of component")
                                                        .definition(SWEHelper.getPropertyUri("ComponentName"))
                                                        .value(""))
                                                .addField("componentValue", createQuantity()
                                                        .label("Component Value")
                                                        .description("Value of component")
                                                        .definition(SWEHelper.getPropertyUri("ComponentValue"))
                                                        .dataType(DataType.FLOAT)
                                                        .value(0.0f)
                                                        .addAllowedInterval(-1.0f, 1.0f)))
                                )).build()).build();
    }

}
