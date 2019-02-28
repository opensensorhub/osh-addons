package org.sensorhub.impl.ndbc;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.ndbc.BuoyEnums.ObsParam;
import org.vast.util.Bbox;

public class DataFilter {
	
    @DisplayInfo(desc="List of station identifiers")
    public Set<String> stationIds = new LinkedHashSet<>();
    
    @DisplayInfo(desc="Geographic region (BBOX)")
    public Bbox siteBbox = new Bbox();
    
    @DisplayInfo(desc="Observed parameters")
    public Set<ObsParam> parameters = new LinkedHashSet<>();
    
    @DisplayInfo(desc="Minimum time stamp of requested objects")
    public Date startTime = null;
    
    @DisplayInfo(desc="Maximum time stamp of requested objects")
    public Date endTime = null;
}
