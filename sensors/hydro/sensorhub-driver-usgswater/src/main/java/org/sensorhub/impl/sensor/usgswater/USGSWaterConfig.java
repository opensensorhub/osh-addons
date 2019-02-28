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
//    	exposeFilter.stateCodes.add(StateCode.NY);
//    	exposeFilter.countyCodes.add("01003");
//    	exposeFilter.countyCodes.add("01089");
        exposeFilter.parameters.add(ObsParam.DISCHARGE);
        exposeFilter.parameters.add(ObsParam.GAGE_HEIGHT);
        exposeFilter.parameters.add(ObsParam.OXY);
        exposeFilter.parameters.add(ObsParam.PH);
        exposeFilter.parameters.add(ObsParam.WATER_TEMP);
        exposeFilter.parameters.add(ObsParam.CONDUCTANCE);
    }
}