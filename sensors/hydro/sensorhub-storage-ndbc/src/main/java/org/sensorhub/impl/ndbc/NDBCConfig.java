/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.ndbc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.persistence.ObsStorageConfig;
import org.vast.util.Bbox;

public class NDBCConfig extends ObsStorageConfig
{
	//  Need to format ISO with integer seconds + "Z"
	static final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("GMT"));
    
    @DisplayInfo(desc="Base URL of NDBC server")
	public String ndbcUrl = "https://sdf.ndbc.noaa.gov/sos/server.php";
	
    @DisplayInfo(desc="List of station identifiers")
    public Set<String> stationIds = new LinkedHashSet<>();
    
    @DisplayInfo(desc="Geographic region (BBOX)")
    public Bbox siteBbox = new Bbox();
    
    @DisplayInfo(desc="Required at least one:  Observed parameters")
    public Set<BuoyParam> parameters = new LinkedHashSet<>();
    
    @DisplayInfo(desc="Period in minutes to harvest FOI info from NDBC caps doc")
    public Long foiUpdatePeriodMinutes = TimeUnit.MINUTES.toMillis(60L);
    
    @DisplayInfo(desc="Required:  ISO-8601 timestamp of earliest data available via NDBC SOS, and used for start timestamp requests to NDBC SOS")
    public String startTimeIso = "2006-01-01T00:00:00Z";  
    
    @DisplayInfo(desc="ISO-8601 for stop timestamp requests to NDBC SOS")
    public String stopTimeIso;
    
    public Long getStartTime() {
    	return Instant.parse(startTimeIso).toEpochMilli();
    }

    public Long getStopTime() {
    	return Instant.parse(stopTimeIso).toEpochMilli();
    }
    
    public void setStartTime(Long timeMs) {
//    	this.startTimeIso = Instant.ofEpochMilli(time).toString();
    	this.startTimeIso = formatIso(timeMs);
    }

    public void setStopTime(Long timeMs) {
//    	this.stopTimeIso = Instant.ofEpochMilli(time).toString();
    	this.stopTimeIso = formatIso(timeMs);
    }
    
    @DisplayInfo(desc="Limit the max amount of days for a single request from NDBC server")
    public Integer maxRequestTimeRange = 7;

    /**
     * 
     * @param timeMs
     * @return iso 8601 formatted String 
     */
    public static String formatIso(long timeMs) {
    	Instant inst = Instant.ofEpochMilli(timeMs);
		LocalDateTime ldt = LocalDateTime.ofInstant(inst, ZoneId.of("GMT"));
    	String iso = ldt.format(formatter);
    	iso = iso.replace(" ", "T") + "Z";
    	return iso;
    }

	@Override
	public void setStorageIdentifier(String name) {
		this.name = name;
	}
}
