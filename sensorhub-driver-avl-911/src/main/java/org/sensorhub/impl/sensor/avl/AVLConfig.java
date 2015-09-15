package org.sensorhub.impl.sensor.avl;

import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.sensor.SensorConfig;


public class AVLConfig extends SensorConfig
{
	// Vehicle agency (e.g. "Huntsville Fire Department")
	public String agencyName;
	
	// comm settings - used to denote data source
    public CommConfig commSettings;
    
}
