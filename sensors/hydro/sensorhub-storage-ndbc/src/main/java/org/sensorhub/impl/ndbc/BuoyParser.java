package org.sensorhub.impl.ndbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.sensorhub.impl.ndbc.BuoyEnums.ObsParam;
import org.slf4j.Logger;
import org.vast.util.Bbox;

public class BuoyParser {
	static final String BASE_URL = NDBCArchive.BASE_NDBC_URL + "/sos/server.php?request=GetObservation&service=SOS&version=1.0.0";
	DataFilter filter;
	ObsParam recordType;
	String requestURL;
	Logger logger;
	
    public BuoyParser(DataFilter filter, ObsParam recordType)
    {
    	this.filter = filter;
    	this.recordType = recordType;
    }
    
//    protected String buildInstantValuesRequest(DataFilter filter, Map<String, String[]> sensorOfferings)
    protected String buildRequest()
    {
        StringBuilder buf = new StringBuilder(BASE_URL);
        
        // site ids
        if (!filter.stationIds.isEmpty())
        {
            buf.append("&offering=");
            for (String id: filter.stationIds)
                buf.append("urn:ioos:station:wmo:").append(id).append(',');
            buf.setCharAt(buf.length()-1, '&');
        }
        
        // site bbox
        else if (filter.siteBbox != null && !filter.siteBbox.isNull())
        {
            Bbox bbox = filter.siteBbox;
            buf.append("&offering=urn:ioos:network:noaa.nws.ndbc:all&featureofinterest=BBOX:")
               .append(bbox.getMinX()).append(",")
               .append(bbox.getMinY()).append(",")
               .append(bbox.getMaxX()).append(",")
               .append(bbox.getMaxY()).append("&");
        }
        
        // parameters
        if (!filter.parameters.isEmpty()) {
			buf.append("&observedProperty=");
			int idx = 0;
			for (ObsParam param : filter.parameters) {
				buf.append(param.toString().toLowerCase()); //.append(',');
				if(++idx < filter.parameters.size())
					buf.append(",");
			}
		}
        
        buf.append("&responseformat=text/csv"); // output type
        
        // time range
        if (filter.startTime != null)
            buf.append("&eventtime=")
        	    .append(Instant.ofEpochMilli(filter.startTime).toString().substring(0,19) + "Z");
        if (filter.endTime != null)
            buf.append("/")
            	.append(Instant.ofEpochMilli(filter.endTime).toString().substring(0,19) + "Z");
        
        return buf.toString();
    }
    
    public void sendRequest() throws IOException {
    	requestURL = buildRequest();
    	logger.info("Requesting observations from: " + requestURL);
    	URL url = new URL(requestURL);
    	
    	parseResponse(url.openStream());
    }
    
    protected List<BuoyRecord> parseResponse(InputStream is) throws IOException {
    	List<BuoyRecord> recs = new ArrayList<>();
    	try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
    		// header line
    		String inline = br.readLine();
    		while(true) {
    			if(inline == null)   break;
    			// read common params
    			String [] values = inline.split(",");
    			BuoyRecord rec = new BuoyRecord(recordType);
    			rec.stationId = values[0];
    			rec.sensorId = values[1];
    			rec.lat = Double.parseDouble(values[2]);
    			rec.lon = Double.parseDouble(values[3]);
    			rec.timeStr = values[4];
    			//rec.timeMs = ;
    			switch(recordType) {
    			case AIR_PRESSURE_AT_SEA_LEVEL:
    				rec.depth = parseDouble(values[5]);
    				rec.airPressure = parseDouble(values[6]);
    				break;
    			case AIR_TEMPERATURE:
    				rec.depth = parseDouble(values[5]);
    				rec.airTemperature = parseDouble(values[6]);
    				break;
			    case SEA_WATER_ELECTRICAL_CONDUCTIVITY:
			    	rec.depth = parseDouble(values[5]);
			    	rec.conductivity = parseDouble(values[6]);
    				break;
			    case SEA_WATER_SALINITY:
			    	rec.depth = parseDouble(values[5]);
			    	rec.salinity = parseDouble(values[6]);
    				break;
			    case SEA_WATER_TEMPERATURE:
			    	rec.depth = parseDouble(values[5]);
			    	rec.waterTemperature = parseDouble(values[6]);
    				break;
			    case SEA_FLOOR_DEPTH_BELOW_SEA_SURFACE:
			    	rec.seaFloorDepth = parseDouble(values[5]);
			    	rec.depthAveragingInterval = parseInt(values[6]);
    				break;
			    case CURRENTS:
    				break;
			    case WAVES:
    				break;
			    case WINDS:
    				break;
			    default:
    			}
    		}
    	}
    	return recs;
    }
    
    private Double parseDouble(String s) {
    	try {
    		return parseDouble(s);
    	} catch (Exception e) {
    		return null;
    	}
    }
    
    private Integer parseInt(String s) {
    	try {
    		return parseInt(s);
    	} catch (Exception e) {
    		return null;
    	}
    }
}
