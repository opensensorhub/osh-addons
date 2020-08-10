package org.sensorhub.impl.ndbc;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sensorhub.api.config.DisplayInfo;
import org.vast.util.Bbox;

@Deprecated // Use NDBCConfig. Refactored to have all these params be in that class directly
public class DataFilter {
	
    @DisplayInfo(desc="List of station identifiers")
    public Set<String> stationIds = new LinkedHashSet<>();
    
    @DisplayInfo(desc="Geographic region (BBOX)")
    public Bbox siteBbox = new Bbox();
    
    @DisplayInfo(desc="Required at least one:  Observed parameters")
    public Set<BuoyParam> parameters = new LinkedHashSet<>();
    
    @DisplayInfo(desc="Required:  Minimum ISO-8601 time stamp of requested objects")
    public String startTimeIso;
//    private Long startTime = null;
    
    @DisplayInfo(desc="Required:  Maximum ISO-8601 time stamp of requested objects")
    public String stopTimeIso;
//    private Long endTime = null;
    
    public Long getStartTime() {
    	return Instant.parse(startTimeIso).toEpochMilli();
    }

    public Long getStopTime() {
    	return Instant.parse(stopTimeIso).toEpochMilli();
    }
    
    public void setStartTime(Long time) {
    	this.startTimeIso = Instant.ofEpochMilli(time).toString();
    }

    public void setStopTime(Long time) {
    	this.stopTimeIso = Instant.ofEpochMilli(time).toString();
    }
    
    @DisplayInfo(desc="Limit the max amount of days for a single request from NDBC server")
    public Integer maxRequestTimeRange = 7;

}
