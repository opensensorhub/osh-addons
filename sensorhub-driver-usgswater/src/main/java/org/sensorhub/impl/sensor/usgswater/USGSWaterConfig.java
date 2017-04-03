package org.sensorhub.impl.sensor.usgswater;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;
import org.sensorhub.impl.usgs.water.CodeEnums.StateCode;
import org.sensorhub.impl.usgs.water.DataFilter;


/**
 * <p>
 * Implementation of sensor interface for USGS Water Data using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Lee Butler <labutler10@gmail.com>
 * @since March 22, 2017
 */
public class USGSWaterConfig extends SensorConfig {
    
    @DisplayInfo(desc="Only data matching this filter will be accessible through this storage instance")
    public DataFilter exposeFilter = new DataFilter();
    
    public USGSWaterConfig()
    {
//    	exposeFilter.siteIds.add("02339495");
//    	exposeFilter.siteIds.add("03574500");
//    	exposeFilter.siteIds.add("03574100");
//    	exposeFilter.siteIds.add("03575100");
//    	exposeFilter.stateCodes.add(StateCode.AL);
    	exposeFilter.countyCodes.add("01053");
    	exposeFilter.countyCodes.add("01133");
        exposeFilter.parameters.add(ObsParam.DISCHARGE);
        exposeFilter.parameters.add(ObsParam.GAGE_HEIGHT);
    }
    
//    @DisplayInfo(label="Get Data By Site Code", desc="Get water data by site code")
//    public boolean getBySiteCode;
//    
//    @DisplayInfo(label="Get Data By State", desc="Get water data by US state")
//    public boolean getByState;
//    
//    @DisplayInfo(label="Get Data By County", desc="Get water data by US county")
//    public boolean getByCounty;
}