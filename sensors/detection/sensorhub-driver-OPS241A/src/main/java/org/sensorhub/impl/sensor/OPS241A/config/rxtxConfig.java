package org.sensorhub.impl.sensor.OPS241A.config;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.sensor.OPS241A.OPS241aConstants;


/**
 Configuration settings for the i2c Connection.
 Specifically, allows for selection of outputs to provide
 @author Bill Brown
 @since June 9, 2025
 */

public class rxtxConfig {
    @DisplayInfo.Required
    @DisplayInfo(label="Port Address", desc="Provide the USB Port for your Sensor (Usually /dev/ttyACM0")
    public String portAddress = OPS241aConstants.portAddress;

    @DisplayInfo.Required
    @DisplayInfo(label="Baud Rate", desc="Provide the baud rate per the specifications")
    public int baudRate = OPS241aConstants.baudRate;
}
