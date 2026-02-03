package org.sensorhub.controller.models;

import java.util.ArrayList;

public class ControllerData {

    public ControllerData(String controllerName, ControllerType controllerType, ArrayList<ControllerComponent> components) {
        this.name = controllerName;
        this.outputs = components;
        this.controllerType = controllerType;
    }

    private String name;

    private ArrayList<ControllerComponent> outputs;
    private ControllerType controllerType;

    public String getName() {
        return name;
    }

    public ArrayList<ControllerComponent> getOutputs() {
        return outputs;
    }
    public ControllerType getControllerType() { return controllerType; }
    public float getValue(String name) {
        return outputs.stream().filter((x) -> x.getName().equals(name)).findFirst().get().getValue();
    }

}
