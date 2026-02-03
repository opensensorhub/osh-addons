package com.botts.controller.models;

public enum ControllerType {

    GAMEPAD("GAMEPAD"), WIIMOTE("WIIMOTE");

    private String type;
    ControllerType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

}
