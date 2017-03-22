package org.sensorhub.impl.sensor.usgswater;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;


/**
 * <p>
 * Implementation of sensor interface for USGS Water Data using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Lee Butler <labutler10@gmail.com>
 * @since October 30, 2014
 */
public class USGSWaterConfig extends SensorConfig {
    
    @DisplayInfo(desc="Base URL (e.g. https://waterservices.usgs.gov/nwis/iv/?format=json)")
    public String urlBase = "https://waterservices.usgs.gov/nwis/iv/?format=json";
    
    public String getUrlBase()
    {
    	return urlBase;
    }
    
    @DisplayInfo(label="Site Codes", desc="List of site codes to provide data")
    public List<String> siteCodes = new ArrayList<String>();
    //public List<String> siteCodes = Arrays.asList("02339495", "03574500", "03574100");
    
    public List<String> getSiteCodes()
    {
    	return siteCodes;
    }
}