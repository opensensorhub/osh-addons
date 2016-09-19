package org.sensorhub.impl.sensor.gamma;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;

public class GammaConfig extends SensorConfig
{
	@Required
    @DisplayInfo(desc="Sensor serial number (used as suffix to generate unique identifier URI)")
    public String serialNumber = "701149";
	
	@Required
    @DisplayInfo(desc="Sensor model number (used as suffix to generate unique identifier URI)")
    public String modelNumber = "2070";
	
	
    @DisplayInfo(label="Communication Settings", desc="Settings for selected communication port")
    public CommProviderConfig<?> commSettings;
    
    
    public GammaConfig()
    {
        this.moduleClass = GammaSensor.class.getCanonicalName();
    }
}
