package org.sensorhub.impl.sensor.openhab;

import java.io.IOException;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.HTTPConfig;
import org.sensorhub.impl.comm.RobustIPConnectionConfig;

public class OpenHabConfig extends SensorConfig
{
	@Required
    @DisplayInfo(desc="Network serial number (used as suffix to generate unique identifier URI)")
    public String serialNumber = "01";
	
	@Required
    @DisplayInfo(desc="Network model number (used as suffix to generate unique identifier URI)")
    public String modelNumber = "AA";
	
    @DisplayInfo(label="HTTP", desc="HTTP configuration")
    public HTTPConfig http = new HTTPConfig();
    
    @DisplayInfo(desc="geographic position of z-wave controller")
    public PositionConfig position = new PositionConfig();
    
    @DisplayInfo(label="Connection Options")
    public RobustIPConnectionConfig connection = new RobustIPConnectionConfig();
    
    
    public OpenHabConfig() throws IOException
    {
        http.user = "botts";
        http.password = "t3a42or24t3a";
        http.remoteHost = "192.168.0.22";
        
//        http.user = "MOTOROLA-7FFBC";
//        http.password = "db0e10daa990751ddf0c";
//        http.remoteHost = "192.168.0.13";
        
//        http.user = "osh";
//        http.password = "osh12345";
//        http.remoteHost = "192.168.1.164";
        
        http.remotePort = 8080;
    }
    
    @Override
    public LLALocation getLocation()
    {
        return position.location;
    }
    
    
    @Override
    public EulerOrientation getOrientation()
    {
        return position.orientation;
    }
}
