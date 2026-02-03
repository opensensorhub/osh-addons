package com.botts.controller.models;

public enum ControllerDirection {

    UP_LEFT("UP_LEFT"), UP("UP"), UP_RIGHT("UP_RIGHT"), RIGHT("RIGHT"), DOWN_RIGHT("DOWN_RIGHT"), DOWN("DOWN"), DOWN_LEFT("DOWN_LEFT"), LEFT("LEFT"), NULL("NULL");

    private String direction;
    ControllerDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return direction;
    }
}
