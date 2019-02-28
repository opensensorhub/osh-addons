package org.sensorhub.impl.sensor.waterdata;

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
public class WaterDataConfig extends SensorConfig {
    
    @DisplayInfo(desc="Site geographic position")
    public PositionConfig position = new PositionConfig();
    
    @DisplayInfo(desc="Base URL (e.g. https://waterservices.usgs.gov/nwis/iv/?format=json)")
    public String urlBase = "https://waterservices.usgs.gov/nwis/iv/?format=json";
    
    public String getUrlBase()
    {
    	return urlBase;
    }
    
    @DisplayInfo(desc="Site ID")
    public String siteCode = "02339495";
    
    public String getSiteCode()
    {
    	return siteCode;
    }
    
    @DisplayInfo(desc="Site Name")
    public String siteName = "Oseligee Creek";
    
    @Override
    public LLALocation getLocation()
    {
        return position.location;
    }
}