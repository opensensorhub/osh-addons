package org.sensorhub.impl.sensor.isa;

import java.util.Date;
import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;


public class ISAConfig extends SensorConfig
{
	@Required
    @DisplayInfo(desc="ISA Network ID (used as suffix to generate unique identifier URI)")
    public String networkID = "701149";
	
	
    @DisplayInfo(label="Communication Settings", desc="Settings for selected communication port")
    public CommProviderConfig<?> commSettings;
    
    
    public Date simulationStartTime;
    
    
    public Date simulationEndTime;
    
    
    public ISAConfig()
    {
        this.moduleClass = ISADriver.class.getCanonicalName();
    }
}
