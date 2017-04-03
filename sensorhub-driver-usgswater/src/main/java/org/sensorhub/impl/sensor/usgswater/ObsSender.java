package org.sensorhub.impl.sensor.usgswater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;

public class ObsSender implements Iterator<DataBlock> {
	
    static final String BASE_URL = USGSWaterDriver.BASE_USGS_URL + "iv?";
    static final String FOI_UID_PREFIX = USGSWaterDriver.UID_PREFIX + "site:";
    static final String AREA_UID_PREFIX = USGSWaterDriver.UID_PREFIX + "region:";
    
    AbstractModule<?> module;
    
    BufferedReader reader;
    USGSWaterConfig config = new USGSWaterConfig();
    ParamValueParser[] paramReaders;
    DataBlock templateRecord, nextRecord;
    
    
    public ObsSender(DataComponent recordDesc)
    {
        this.templateRecord = recordDesc.createDataBlock();
    }
    
    
    protected String buildIvRequest(Map<String, AbstractFeature> siteFois)
    {
        StringBuilder buf = new StringBuilder(BASE_URL);
        
        buf.append("sites=");
        for (Map.Entry<String, AbstractFeature> entry : siteFois.entrySet())
        	buf.append(entry.getValue().getId()).append(',');
        buf.setCharAt(buf.length()-1, '&');
        
        // constant options
        buf.append("format=rdb"); // output format
        
        return buf.toString();
    }
    
    
    public DataBlock sendRequest(Map<String, AbstractFeature> fois) throws IOException
    {
        String requestUrl = buildIvRequest(fois);
        
        System.out.println("Requesting observations from: " + requestUrl);
        
        // TODO Fix logger
        //module.getLogger().debug("Requesting observations from: ", requestUrl);        
        URL url = new URL(requestUrl);
        
        reader = new BufferedReader(new InputStreamReader(url.openStream()));
        nextRecord = null;
        return preloadNext();
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
                
                // parse section header when data for next site begins
                if (line.startsWith("#"))
                {
                    parseHeader();
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
        	System.out.println("Error while reading tabular data");
            //module.getLogger().error("Error while reading tabular data", e);
        }
        
        return null;
    }
    
    protected void parseHeader() throws IOException
    {
        // skip header comments
        String line;
        while ((line = reader.readLine()) != null)
        {
            line = line.trim();
            if (!line.startsWith("#"))
                break;
        }
        
        // parse field names and prepare corresponding readers
        String[] fieldNames = line.split("\t");
        initParamReaders(config.exposeFilter.parameters, fieldNames);
        
        // skip field sizes
        reader.readLine();
    }
    
    protected void initParamReaders(Set<ObsParam> params, String[] fieldNames)
    {
        ArrayList<ParamValueParser> readers = new ArrayList<ParamValueParser>();
        int i = 0;
        
        // always add time stamp and site ID readers
        readers.add(new TimeStampParser(2, i++));
        readers.add(new SiteNumParser(1, i++));
        
        // create a reader for each selected param
        for (ObsParam param: params)
        {
            // look for field with same param code
            int fieldIndex = -1;
            for (int j = 0; j < fieldNames.length; j++)
            {
                if (fieldNames[j].endsWith(param.getCode()))
                {
                    fieldIndex = j;
                    break;
                }
            }
            
            readers.add(new FloatValueParser(fieldIndex, i++));
        }  
        
        paramReaders = readers.toArray(new ParamValueParser[0]);
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
                System.out.println("time = " + ts);
                data.setDoubleValue(toIndex, ts/1000.);
            }
            catch (ParseException e)
            {
                throw new IOException("Invalid time stamp " + buf.toString());
            }
        }
    }
    
    /*
     * Parser for site ID
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
            System.out.println("site num = " + val);
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
                    if (!val.isEmpty() && !val.startsWith("*"))
                        f = Float.parseFloat(val);
                }
                System.out.println("float = " + f);
                data.setFloatValue(toIndex, f);
            }
            catch (NumberFormatException e)
            {
                throw new IOException("Invalid numeric value " + tokens[fromIndex], e);
            }
        }
    }


	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public DataBlock next() {
		// TODO Auto-generated method stub
		return null;
	}

}
