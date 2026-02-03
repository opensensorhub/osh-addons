package com.botts.impl.sensor.universalcontroller.helpers;

public enum UniversalControllerComponent {
    A_BUTTON("A"),
    B_BUTTON("B"),
    C_BUTTON("C"),
    X_BUTTON("X"),
    Y_BUTTON("Y"),
    Z_BUTTON("Z"),
    LT_BUTTON("LeftThumb"),
    RT_BUTTON("RightThumb"),
    LT2_BUTTON("LeftThumb2"),
    RT2_BUTTON("RightThumb2"),
    LT3_BUTTON("LeftThumb3"),
    RT3_BUTTON("RightThumb3"),
    SELECT_BUTTON("Select"),
    START_BUTTON("Start"),
    HOME_OR_MODE_BUTTON("Mode"),
    _0_BUTTON("0"),
    _1_BUTTON("1"),
    _2_BUTTON("2"),
    _3_BUTTON("3"),
    _4_BUTTON("4"),
    _5_BUTTON("5"),
    _6_BUTTON("6"),
    _7_BUTTON("7"),
    _8_BUTTON("8"),
    _9_BUTTON("9"),
    D_PAD("pov"),
    // Dpad is different from up-down-l-r buttons
    UP_BUTTON("Forward"),
    DOWN_BUTTON("Back"),
    LEFT_BUTTON("Left"),
    RIGHT_BUTTON("Right"),
    X_AXIS("x"),
    Y_AXIS("y"),
    Z_AXIS("z"),
    RX_AXIS("rx"),
    RY_AXIS("ry"),
    RZ_AXIS("rz"),
    X_ACCELERATION("x-acceleration"),
    Y_ACCELERATION("y-acceleration"),
    Z_ACCELERATION("z-acceleration"),
    RX_ACCELERATION("rx-acceleration"),
    RY_ACCELERATION("ry-acceleration"),
    RZ_ACCELERATION("rz-acceleration");
    // TODO: add the rest
    private String componentName;

    UniversalControllerComponent(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }

    public static UniversalControllerComponent fromString(String text) {
        for (UniversalControllerComponent c : UniversalControllerComponent.values()) {
            if (c.componentName.equals(text)) {
                return c;
            }
        }
        return null;
    }

    public enum DPadDirection {
        UP_LEFT(0.125f), UP(0.25f), UP_RIGHT(0.375f), RIGHT(0.5f), DOWN_RIGHT(0.625f), DOWN(0.75f), DOWN_LEFT(0.875f), LEFT(1.0f);
        private float povValue;

        DPadDirection(float povValue) {
            this.povValue = povValue;
        }

        public float getValue() {
            return povValue;
        }
    }
}
