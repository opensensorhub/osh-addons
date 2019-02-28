package org.sensorhub.impl.sensor.vaisala;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.impl.comm.UARTConfig;

public class VaisalaWeatherConfig extends SensorConfig
{
	public String serialNumber = "aaa0001bb";
	
    @DisplayInfo(label="Communication Settings", desc="Settings for selected communication port")
    public CommProviderConfig<?> commSettings;
    
//    @DisplayInfo(desc="Station Location")
//    public LLALocation location = new LLALocation();
    
    @DisplayInfo(desc="Station Geographic Position")
    public PositionConfig position = new PositionConfig();
    
    public VaisalaWeatherConfig()
    {
        this.moduleClass = VaisalaWeatherSensor.class.getCanonicalName();
        
//        UARTConfig serialConf = new UARTConfig();
//        serialConf.moduleClass = "org.sensorhub.impl.comm.rxtx.RxtxSerialCommProvider";
//        serialConf.portName = "/dev/ttyUSB0";
//        serialConf.baudRate = 19200;
//        this.commSettings = serialConf;
    }
    
//    @Override
//    public LLALocation getLocation()
//    {
//        return position.location;
//    }
    
}
