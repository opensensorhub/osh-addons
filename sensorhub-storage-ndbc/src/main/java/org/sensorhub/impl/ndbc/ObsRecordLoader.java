package org.sensorhub.impl.ndbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.NoSuchElementException;
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
        this.data = recordDesc.createDataBlock();
        
    }
    
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
        
//        // state codes
//        else if (!filter.stateCodes.isEmpty())
//        {
//            buf.append("stateCd=");
//            for (StateCode state: filter.stateCodes)
//                buf.append(state.name()).append(',');
//            buf.setCharAt(buf.length()-1, '&');
//        }
        
//        // county codes
//        else if (!filter.countyCodes.isEmpty())
//        {
//            buf.append("countyCd=");
//            for (String countyCd: filter.countyCodes)
//            	buf.append(countyCd).append(',');
//            buf.setCharAt(buf.length()-1, '&');
//        }            
        
//        // site bbox
//        else if (filter.siteBbox != null && !filter.siteBbox.isNull())
//        {
//            Bbox bbox = filter.siteBbox;
//            buf.append("bbox=")
//               .append(bbox.getMinX()).append(",")
//               .append(bbox.getMaxY()).append(",")
//               .append(bbox.getMaxX()).append(",")
//               .append(bbox.getMinY()).append("&");
//        }
        
//        // site types
//        if (!filter.siteTypes.isEmpty())
//        {
//            buf.append("siteType=");
//            for (SiteType type: filter.siteTypes)
//                buf.append(type.name()).append(',');
//            buf.setCharAt(buf.length()-1, '&');
//        }
        
        // parameters
        if (!filter.parameters.isEmpty())
        {
            buf.append("observedProperty=");
            for (ObsParam param: filter.parameters)
                buf.append(param.toString().toLowerCase()).append(',');
            buf.setCharAt(buf.length()-1, '&');
        }
        
        buf.append("responseformat=text/csv"); // output type
        
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
    	System.out.println("Requesting observations from: " + requestURL);
    	module.getLogger().debug("Requesting observations from: " + requestURL);
    	URL url = new URL(requestURL);
    	
    	reader = new BufferedReader(new InputStreamReader(url.openStream()));

    	this.filter = filter;
    	
    	// preload first record
        nextRecord = null;
        preloadNext();
    }
    
    
    protected DataBlock preloadNext()
    {
        try
        {
            DataBlock currentRecord = nextRecord;        
            nextRecord = null;
            
            String line;
            System.out.println("");
            while ((line = reader.readLine()) != null)
            {                
                line = line.trim();
                System.out.println(line);
                
                // parse section header when data for next site begins
                if (line.startsWith("#"))
                {
//                    parseHeader();
                    line = reader.readLine();
                    if (line == null || line.trim().isEmpty())
                        return null;
                }
                
                String[] fields = line.split("\t");
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
        StringBuilder buf = new StringBuilder();
        
        public TimeStampParser(int fromIndex, int toIndex)
        {
            super(fromIndex, toIndex);
        }
        
        public void parse(String[] tokens, DataBlock data) throws IOException
        {
            buf.setLength(0);
            buf.append(tokens[fromIndex].trim());
            buf.append(' ');
            buf.append(tokens[fromIndex+1].trim());
            
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
     * Parser for station ID
     */
    static class SiteNumParser extends ParamValueParser
    {
        public SiteNumParser(int fromIndex, int toIndex)
        {
            super(fromIndex, toIndex);
        }
        
        public void parse(String[] tokens, DataBlock data)
        {
            String val = tokens[fromIndex].trim();
//            data.setStringValue(toIndex, val);
            data.setStringValue(toIndex, "AA001");
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
                    if (!val.isEmpty() && !val.startsWith("*"))
                        f = Float.parseFloat(val);
                }
                
//                data.setFloatValue(toIndex, f);
                data.setFloatValue(toIndex, 95.234f);
            }
            catch (NumberFormatException e)
            {
                throw new IOException("Invalid numeric value " + tokens[fromIndex], e);
            }
        }
    }
}
