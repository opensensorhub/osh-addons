package org.sensorhub.controller.models;

public enum Sensitivity {

    NULL(0.0f), VERY_LOW(0.25f), LOW(0.5f), MEDIUM(1.2f), HIGH(1.6f), VERY_HIGH(2.5f);

    private float sensitivityModifier;

    Sensitivity(float sensitivityModifier) {
        this.sensitivityModifier = sensitivityModifier;
    }

    public float getSensitivityModifier() {
        return sensitivityModifier;
    }
}
