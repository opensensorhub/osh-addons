package org.sensorhub.impl.sensor.BNO085.config;

import org.sensorhub.api.config.DisplayInfo;


/**
    Configuration settings for the BNO085 driver exposed via the OpenSensorHub Admin panel.
    Specifically, allows for selection of outputs to provide
    @author Bill Brown
    @since June 9, 2025
 */
public class Outputs {
    @DisplayInfo(label = "Time Interval (seconds)", desc="Provide a time interval for sensor to provide readings")
    public int timeIntervalSeconds = 1;

    @DisplayInfo(label = "Accelerometer Vector", desc="Do you want sensor to display gravity vector")
    public boolean isAccelerometer = false;

    @DisplayInfo(label = "Gravity Vector", desc="Do you want sensor to display gravity vector")
    public boolean isGravity = false;

    @DisplayInfo(label = "Calibrated Gyroscope Vector", desc="Do you want sensor to display the calibrated Gyroscope Vector")
    public boolean isGyroCal = false;

    @DisplayInfo(label = "Calibrated Magnetic Field Vector", desc="Do you want sensor to display the calibrated Magnetic Field Vector")
    public boolean isMagFieldCal = false;

    @DisplayInfo(label = "Rotation Vector", desc="Do you want sensor to display the calibrated Rotation Vector")
    public boolean isRotation = true;

}
