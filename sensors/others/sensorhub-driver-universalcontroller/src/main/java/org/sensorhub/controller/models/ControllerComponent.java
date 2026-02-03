package org.sensorhub.controller.models;

public class ControllerComponent {

    private String name;
    private float value;

    public ControllerComponent(String name, float initialValue) {
        this.name = name;
        this.value = initialValue;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
