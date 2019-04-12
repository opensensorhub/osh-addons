package org.sensorhub.impl.ndbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.ndbc.BuoyEnums.ObsParam;
import org.vast.util.Bbox;
import org.vast.util.DateTimeFormat;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;

public class ObsRecordLoader implements Iterator<DataBlock> {
	static final String BASE_URL = NDBCArchive.BASE_NDBC_URL + "/sos/server.php?request=GetObservation&service=SOS&version=1.0.0";
	AbstractModule<?> module;
	DataFilter filter;
	BufferedReader reader;
	String requestURL;
	ParamValueParser[] paramReaders;
	DataBlock data;
	DataBlock templateRecord, nextRecord;
	
    public ObsRecordLoader(AbstractModule<?> module, DataComponent recordDesc)
    {
        this.module = module;
        this.templateRecord = recordDesc.createDataBlock();
        this.data = recordDesc.createDataBlock();
        
    }
    
//    protected String buildInstantValuesRequest(DataFilter filter, Map<String, String[]> sensorOfferings)
    protected String buildInstantValuesRequest(DataFilter filter)
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
        DateTimeFormat timeFormat = new DateTimeFormat();
        if (filter.startTime != null)
            buf.append("&eventtime=")
               .append(timeFormat.formatIso(filter.startTime.getTime()/1000., 0));
        if (filter.endTime != null)
            buf.append("/")
               .append(timeFormat.formatIso(filter.endTime.getTime()/1000., 0));
        
        return buf.toString();
    }
    
    public void sendRequest(DataFilter filter) throws IOException {
    	requestURL = buildInstantValuesRequest(filter);
    	module.getLogger().debug("Requesting observations from: " + requestURL);
    	URL url = new URL(requestURL);
    	
    	reader = new BufferedReader(new InputStreamReader(url.openStream()));

    	this.filter = filter;
    	
    	// preload first record
        nextRecord = null;
        preloadNext();
    }
    
    protected void initParamReaders(Set<ObsParam> params, String[] fieldNames)
    {
        ArrayList<ParamValueParser> readers = new ArrayList<ParamValueParser>();
        int i = 0;
        
        // always add time stamp, site ID, and location readers
        readers.add(new TimeStampParser(4, i++));
        readers.add(new StationIdParser(0, i++));
        readers.add(new FloatValueParser(2, i++)); // buoy loc lat
        readers.add(new FloatValueParser(3, i++)); // buoy loc lon
        
        // create a reader for each selected param
        for (ObsParam param: params)
        {
        	if (param == ObsParam.CURRENTS) {
        		readers.add(new FloatValueParser(5, i++)); // bin
        		readers.add(new BuoyDepthParser(6, i++)); // buoy depth
        		readers.add(new FloatValueParser(7, i++)); // direction of sea water velocity (deg)
        		readers.add(new FloatValueParser(8, i++)); // sea water speed (cm/s)
        		readers.add(new FloatValueParser(9, i++)); // upward sea water velocity (cm/s)
        		readers.add(new FloatValueParser(10, i++)); // error velocity (cm/s)
        		readers.add(new FloatValueParser(11, i++)); // platform orientation (deg)
        		readers.add(new FloatValueParser(12, i++)); // platform pitch angle (deg)
        		readers.add(new FloatValueParser(13, i++)); // platform roll angle (deg)
        		readers.add(new FloatValueParser(14, i++)); // sea water temperature (degC)
        		readers.add(new FloatValueParser(15, i++)); // percent good 3 beam (%)
        		readers.add(new FloatValueParser(16, i++)); // percent good 4 beam (%)
        		readers.add(new FloatValueParser(17, i++)); // percent rejected (%)
        		readers.add(new FloatValueParser(18, i++)); // percent bad (%)
        		readers.add(new FloatValueParser(19, i++)); // echo intensity beam 1 (count)
        		readers.add(new FloatValueParser(20, i++)); // echo intensity beam 2 (count)
        		readers.add(new FloatValueParser(21, i++)); // echo intensity beam 3 (count)
        		readers.add(new FloatValueParser(22, i++)); // echo intensity beam 4 (count)
        		readers.add(new FloatValueParser(23, i++)); // correlation magnitude beam 1 (count)
        		readers.add(new FloatValueParser(24, i++)); // correlation magnitude beam 2 (count)
        		readers.add(new FloatValueParser(25, i++)); // correlation magnitude beam 3 (count)
        		readers.add(new FloatValueParser(26, i++)); // correlation magnitude beam 4 (count)
        		readers.add(new FloatValueParser(27, i++)); // quality flags (au)
        	}
        	else if (param == ObsParam.SEA_FLOOR_DEPTH_BELOW_SEA_SURFACE) {
        		readers.add(new FloatValueParser(5, i++)); // sea floor depth below sea surface (m)
        		readers.add(new FloatValueParser(6, i++)); // averaging interval
        	}
        	else if (param == ObsParam.WAVES) {
        		readers.add(new FloatValueParser(5, i++)); // sea surface wave significant height (m)
        		readers.add(new FloatValueParser(6, i++)); // sea surface wave peak period (s)
        		readers.add(new FloatValueParser(7, i++)); // sea surface wave mean period (s)
        		readers.add(new FloatValueParser(8, i++)); // sea surface swell wave significant height (m)
        		readers.add(new FloatValueParser(9, i++)); // sea surface swell wave period (s)
        		readers.add(new FloatValueParser(10, i++)); // sea surface wind wave significant height (m)
        		readers.add(new FloatValueParser(11, i++)); // sea surface wind wave period (s)
        		readers.add(new FloatValueParser(12, i++)); // sea water temperature (degC)
        		readers.add(new FloatValueParser(13, i++)); // sea surface wave to direction (deg)
        		readers.add(new FloatValueParser(14, i++)); // sea surface swell wave to direction (deg)
        		readers.add(new FloatValueParser(15, i++)); // sea surface wind wave to direction (deg)
        		
        		// Ingnoring the below parameters for now
        		// To include them, we need to replace all semicolons in "line" to commas
        		
//        		readers.add(new FloatValueParser(16, i++)); // number of frequencies (count)
//        		readers.add(new FloatValueParser(17, i++)); // center frequencies (Hz)
        		// delimiter changes from comma to semicolon here...
//        		readers.add(new FloatValueParser(18, i++)); // bandwidths (Hz)
//        		readers.add(new FloatValueParser(19, i++)); // spectral energy (m^2/Hz)
//        		readers.add(new FloatValueParser(20, i++)); // mean wave direction (deg)
//        		readers.add(new FloatValueParser(21, i++)); // principle wave direction (deg)
//        		readers.add(new FloatValueParser(22, i++)); // polar coordinate r1 (1)
//        		readers.add(new FloatValueParser(23, i++)); // polar coordinate r2 (1)
//        		readers.add(new FloatValueParser(24, i++)); // calculation method
//        		readers.add(new FloatValueParser(25, i++)); // sampling rate (Hz)
        	}
        	else if (param == ObsParam.WINDS) {
        		readers.add(new BuoyDepthParser(5, i++)); // buoy depth
        		readers.add(new FloatValueParser(6, i++)); // wind from direction (deg)
        		readers.add(new FloatValueParser(7, i++)); // wind speed (m/s)
        		readers.add(new FloatValueParser(8, i++)); // wind speed of gust (m/s)
        		readers.add(new FloatValueParser(9, i++)); // upward air velocity
        	}
        	else {
        		readers.add(new BuoyDepthParser(5, i++));
	            // look for field with same param code
	            int fieldIndex = -1;
	            for (int j = 0; j < fieldNames.length; j++)
	            {
	                if (fieldNames[j].contains(param.toString().toLowerCase()))
	                {
	                    fieldIndex = j;
	                    break;
	                }
	            }
	            
	            readers.add(new FloatValueParser(fieldIndex, i++));
        	}
        }  
        
        paramReaders = readers.toArray(new ParamValueParser[0]);
    }
    
    
    protected DataBlock preloadNext()
    {
        try
        {
            DataBlock currentRecord = nextRecord;        
            nextRecord = null;
            
            String line;
            while ((line = reader.readLine()) != null)
            {                
                line = line.trim();
                
                // parse header
                if (line.startsWith("station_id,sensor_id"))
                {
                	String[] fieldNames = line.split(",");
                	initParamReaders(filter.parameters, fieldNames);
                    line = reader.readLine();
                    if (line == null || line.trim().isEmpty())
                        return null;
                }
                
                if (line.endsWith(","))
                	line = line + "NaN";
                
                if (line.contains(",,"))
                {
                	while (line.contains(",,"))
                	{
	            		line = line.replaceAll(",,", ",NaN,");
                	}
                }
                
                String[] fields = line.split(",");
                nextRecord = templateRecord.renew();
                
                // read all requested fields to datablock
                for (ParamValueParser reader: paramReaders)
                    reader.parse(fields, nextRecord);
                
                break;
            }
            
            return currentRecord;
        }
        catch (IOException e)
        {
            module.getLogger().error("Error while reading tabular data", e);
        }
        
        return null;
    }
    
    
    @Override
    public boolean hasNext()
    {
        return (nextRecord != null);
    }
    

    @Override
    public DataBlock next()
    {
        if (!hasNext())
            throw new NoSuchElementException();
        return preloadNext();
    }
    
    
    public void close()
    {
        try
        {
            if (reader != null)
                reader.close();
        }
        catch (IOException e)
        {
        }
    }
    
    // The following classes are used to parse individual values from the tabular data
    // A list of parsers is built according to the desired output and parsers are applied
    // in sequence to fill the datablock with the proper values
    
    /*
     * Base value parser class
     */
    static abstract class ParamValueParser
    {
        int fromIndex;
        int toIndex;
        
        public ParamValueParser(int fromIndex, int toIndex)
        {
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }
        
        public abstract void parse(String[] tokens, DataBlock data) throws IOException;
    }
    
    /*
     * Parser for time stamp field, including time zone
     */
    static class TimeStampParser extends ParamValueParser
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        StringBuilder buf = new StringBuilder();
        
        public TimeStampParser(int fromIndex, int toIndex)
        {
            super(fromIndex, toIndex);
            dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
        }
        
        public void parse(String[] tokens, DataBlock data) throws IOException
        {
            buf.setLength(0);
            buf.append(tokens[fromIndex].trim());
            
            try
            {
                long ts = dateFormat.parse(buf.toString()).getTime();
                data.setDoubleValue(toIndex, ts/1000.);
            }
            catch (ParseException e)
            {
                throw new IOException("Invalid time stamp " + buf.toString());
            }
        }
    }
    
    /*
     * Parser for buoy depth field, in meters
     */
    static class BuoyDepthParser extends ParamValueParser
    {        
        public BuoyDepthParser(int fromIndex, int toIndex)
        {
            super(fromIndex, toIndex);
        }
        
        public void parse(String[] tokens, DataBlock data) throws IOException
        {
            try
            {
                float f = Float.NaN;
                
                String val = tokens[fromIndex].trim();
                if (!val.isEmpty())
                    f = Float.parseFloat(val);
                
                data.setFloatValue(toIndex, f);
            }
            catch (NumberFormatException e)
            {
                throw new IOException("Invalid numeric value " + tokens[fromIndex], e);
            }
        }
    }
    
    /*
     * Parser for station ID
     */
    static class StationIdParser extends ParamValueParser
    {
        public StationIdParser(int fromIndex, int toIndex)
        {
            super(fromIndex, toIndex);
        }
        
        public void parse(String[] tokens, DataBlock data)
        {
            String val = tokens[fromIndex].trim();
            data.setStringValue(toIndex, val);
        }
    }
    
    /*
     * Parser for floating point value
     */
    static class FloatValueParser extends ParamValueParser
    {
        public FloatValueParser(int fromIndex, int toIndex)
        {
            super(fromIndex, toIndex);
        }
        
        public void parse(String[] tokens, DataBlock data) throws IOException
        {
            try
            {
                float f = Float.NaN;
                
                if (fromIndex >= 0)
                {
                    String val = tokens[fromIndex].trim();
                    if (!val.isEmpty() && val!= "NaN")
                    	f = Float.parseFloat(val);
                }
                
                data.setFloatValue(toIndex, f);
            }
            catch (NumberFormatException e)
            {
                throw new IOException("Invalid numeric value " + tokens[fromIndex], e);
            }
        }
    }
}
