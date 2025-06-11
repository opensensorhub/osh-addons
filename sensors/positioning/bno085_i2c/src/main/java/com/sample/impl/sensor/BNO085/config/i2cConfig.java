package com.sample.impl.sensor.BNO085.config;
import org.sensorhub.api.config.DisplayInfo;


/**
 Configuration settings for the i2c Connection.
 Specifically, allows for selection of outputs to provide
 @author Bill Brown
 @since June 9, 2025
 */

public class i2cConfig {
    @DisplayInfo.Required
    @DisplayInfo(label="I2C Bus", desc="Provide the I2C bus number. Example (1) for /dev/i2c-1")
    public int I2C_BUS_NUMBER = 1;

    @DisplayInfo.Required
    @DisplayInfo(label="Sensor Address", desc="Provide the Register Address for the Sensor. Most likely 0x4A or 0x4B")
    public int SENSOR_ADDRESS = 0x4A;
}
