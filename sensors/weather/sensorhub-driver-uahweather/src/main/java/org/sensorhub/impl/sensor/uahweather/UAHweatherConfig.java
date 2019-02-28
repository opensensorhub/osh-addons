package org.sensorhub.impl.sensor.uahweather;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;

public class UAHweatherConfig extends SensorConfig
{
	@Required
    @DisplayInfo(desc="Sensor serial number (used as suffix to generate unique identifier URI)")
    public String serialNumber = "001";
	
	@Required
    @DisplayInfo(desc="Sensor model number (used as suffix to generate unique identifier URI)")
    public String modelNumber = "A";
	
	
    @DisplayInfo(label="Communication Settings", desc="Settings for selected communication port")
    public CommProviderConfig<?> commSettings;
    
    
    public UAHweatherConfig()
    {
        this.moduleClass = UAHweatherSensor.class.getCanonicalName();
    }
}
