package org.sensorhub.impl.sensor.avl;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;


public class AVLConfig extends SensorConfig
{
    @DisplayInfo(label="Fleet ID", desc="ID of vehicle fleet (will be appended to system UID)")
    public String fleetID;
    
    @DisplayInfo(label="Agency Name", desc="Naming of agency operating the vehicles")
    public String agencyName;
	
    @DisplayInfo(desc="Communication settings to connect to AVL data")
    public CommProviderConfig<?> commSettings;
    
}
