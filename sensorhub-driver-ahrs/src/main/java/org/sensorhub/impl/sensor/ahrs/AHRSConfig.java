package org.sensorhub.impl.sensor.ahrs;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;


public class AHRSConfig extends SensorConfig 
{
	public String serialNumber;
	
	public String contactPerson;
	
	
	@DisplayInfo(desc="Communication settings to connect to range finder data stream")
    public CommProviderConfig<?> commSettings;
	
	
    public AHRSConfig()
    {
        this.moduleClass = AHRSSensor.class.getCanonicalName();        
    }
}
