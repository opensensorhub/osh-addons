package com.botts.controller.gamepad;

public enum POVDirection {
    UP_LEFT(0.125f), UP(0.25f), UP_RIGHT(0.375f), RIGHT(0.5f), DOWN_RIGHT(0.625f), DOWN(0.75f), DOWN_LEFT(0.875f), LEFT(1.0f);

    private float povValue;

    POVDirection(float povValue) {
        this.povValue = povValue;
    }

    public float getPovValue() {
        return povValue;
    }
}
