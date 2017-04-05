/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc.. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.usgswater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.TextEncoding;
import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.usgswater.ObsSender.ParamValueParser;
import org.sensorhub.impl.usgs.water.RecordStore;
import org.sensorhub.impl.usgs.water.WaterDataRecord;
import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Implementation of sensor interface for USGS Water Data using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Lee Butler <labutler10@gmail.com>
 * @since March 22, 2017
 */

public class USGSWaterOutput extends AbstractSensorOutput <USGSWaterDriver> implements IMultiSourceDataInterface
{
    DataRecord dataStruct;
    TextEncoding encoding;
    Timer timer;
    BufferedReader reader;
    Map<String, Long> latestUpdateTimes;
    Map<String, DataBlock> latestRecords = new LinkedHashMap<String, DataBlock>();
    static final String BASE_URL = USGSWaterDriver.BASE_USGS_URL + "iv?sites=";
    final int urlChunkSize = 100;
    
    List<String> qualCodes = new ArrayList<String>();

    public USGSWaterOutput(USGSWaterDriver driver)
    {
        super(driver);
        latestUpdateTimes = new HashMap<String, Long>();
    }


    @Override
    public String getName()
    {
        return "waterData";
    }
  
    
    protected void init()
    {   
        SWEHelper swe = new SWEHelper();
        GeoPosHelper geo = new GeoPosHelper();
        
        dataStruct = swe.newDataRecord();
        dataStruct.setName(getName());
        dataStruct.setDefinition("http://sensorml.com/ont/swe/property/StreamData");
        dataStruct.addField("time", swe.newTimeStampIsoUTC());
        dataStruct.addField("site", swe.newText("http://sensorml.com/ont/swe/property/SiteID", "Site ID", null));
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        dataStruct.addField("location", geo.newLocationVectorLatLon(SWEConstants.DEF_SENSOR_LOC));
        
        for (ObsParam param: parentSensor.getConfiguration().exposeFilter.parameters)
        {
            String paramName = param.name().toLowerCase();
            
            DataComponent c = swe.newQuantity(
                    getDefUri(param),
                    getLabel(param),
                    getDesc(param),
                    getUom(param),
                    DataType.FLOAT);
            
            dataStruct.addComponent(paramName, c);
        }
        
        // use text encoding with "$$" separators
        encoding = swe.newTextEncoding("$$", "\n");
    }
    
    protected String getDefUri(ObsParam param)
    {
        String name = param.toString().replaceAll(" ", "");
        return SWEHelper.getPropertyUri(name);
    }
    
    
    protected String getLabel(ObsParam param)
    {
        return param.toString();
    }
    
    
    protected String getDesc(ObsParam param)
    {
        return param.toString() + " parameter, USGS code " + param.getCode();
    }
    
    
    protected String getUom(ObsParam param)
    {
        switch (param)
        {
            case WATER_TEMP:
                return "Cel";                
            case DISCHARGE:
                return "[cft_i]/s";
            case GAGE_HEIGHT:
                return "[ft_i]";
            case CONDUCTANCE:
                return "uS/cm";
            case OXY:
                return "mg/L";
            case PH:
                return "1";
        }
        
        return null;
    }
    
    
    protected void start(final Map<String, AbstractFeature> fois)
    {
    	//sender = new ObsSender(getRecordDescription());
    	if (timer != null)
            return;
        
    	TimerTask timerTask = new TimerTask()
    	{
			public void run()
    		{	
				String[] requestUrl = buildIvRequest(fois);
				
				try
				{
					for (int i=0; i<requestUrl.length; i++)
						sendObs(requestUrl[i], fois); // Pass url array and fois map to sendObs()
				}
				catch (IOException e1)
				{
					e1.printStackTrace();
				}
				// wait a bit before querying next station
            	try { Thread.sleep(1000); }
                catch (InterruptedException e) { }
			}
		};
		timer = new Timer();
		timer.scheduleAtFixedRate(timerTask, 0, (long)(getAverageSamplingPeriod()*1000));
    }
    
    public void sendObs(String requestUrl, Map<String, AbstractFeature> fois) throws IOException
    {
    	// Populate list of qualification codes
    	// Codes found at https://waterdata.usgs.gov/nwis?codes_help
    	
    	qualCodes.add("Ssn"); // Parameter monitored seasonally
    	qualCodes.add("Bkw"); // Flow affected by backwater
    	qualCodes.add("Ice"); // Ice affected
    	qualCodes.add("Pr"); // Partial-record site
    	qualCodes.add("Rat"); // Rating being developed or revised
    	qualCodes.add("Eqp"); // Eqp = equipment malfunction
    	qualCodes.add("Fld"); // Flood damage
    	qualCodes.add("Dry"); // Dry
    	qualCodes.add("Dis"); // Data-collection discontinued
    	qualCodes.add("--"); // Parameter not determined
    	qualCodes.add("Mnt"); // Maintenance in progress
    	qualCodes.add("ZFl"); // Zero flow
    	qualCodes.add("***"); // *** = temporarily unavailable
    	
    	
    	AbstractFeature foiValue;
    	
    	Set<ObsParam> params = parentSensor.getConfiguration().exposeFilter.parameters;
    	String siteId, point;
    	String[] pointArr, locArr;
    	long ts;
    	double siteLat, siteLon;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
        StringBuilder buf = new StringBuilder();
    	Float[] f = new Float[parentSensor.getConfiguration().exposeFilter.parameters.size()];
    	
    	URL url = new URL(requestUrl);
    	USGSWaterDriver.log.debug("Requesting observations from: " + url);
    	reader = new BufferedReader(new InputStreamReader(url.openStream()));
    	
    	String line;
        while ((line = reader.readLine()) != null)
        {                
            line = line.trim();
            
            // parse section header when data for next site begins
            if (line.startsWith("#"))
            	continue;
	        
	        // Get data field names
	        String[] fields = line.split("\t");
	        
	//        for (int s=0; s<fields.length; s++)
	//        	System.out.println("fields[s]: " + fields[s]);
	        
	        // Skip middle line in data section
	        reader.readLine();
	        
	        // Loop to get all lines with data values
	        // There are sometimes more than one
	        while ((line = reader.readLine()) != null)
	        {
		        // Now get line with data values
	        	line = line.trim();
		        
	        	// if line is not a data line, exit this loop
	        	// and go to main loop to start next section
		    	if (line.startsWith("#"))
		    		break;
		        
		        // And separate them, put them into array
		        String[] data = line.split("\t");
		        
//		        for (int d=0; d<data.length; d++)
//		        	System.out.println("data[d]: " + data[d]);
		        
		        // Create float array. Should be safe since this should be same
		        // number of float components added to data record in init()
		        Arrays.fill(f, Float.NaN); // Set all float values to NaN
		        siteId = "unreported"; // Initialize siteId in case it isn't reported
		        point = null;
		        buf.setLength(0); // Clear string builder for datetime
		        ts = 0; // Initialize time variable

		        // Initialize Site Lat/Lon
		        siteLat = Double.NaN;
		        siteLon = Double.NaN;
		        
		        int paramIt = 0;
		    	for (ObsParam param: params) // Loop over list of observation types requested in config
		    	{
		    		// Loop over data array; size should be <= size of fields[]
		    		for (int i=0; i<data.length; i++)
		    		{
		    			if ("site_no".equalsIgnoreCase(fields[i]))
		    			{
		    				siteId = data[i];
		    				// Get location of this site
		    				foiValue = fois.get(siteId);
		    				pointArr = foiValue.getLocation().toString().split("\\(");
		    				point = pointArr[1].substring(0, pointArr[1].length()-1);
		    				locArr = point.split("\\s+");
		    				
		    				siteLat = Double.parseDouble(locArr[0]);
		    				siteLon = Double.parseDouble(locArr[1]);
		    			}
		    			
		    			if ("datetime".equalsIgnoreCase(fields[i]))
		    			{
		    				buf.append(data[i].trim());
		    				buf.append(' ');
		    				buf.append(data[i+1].trim());
		    				
		    				try
		    				{
								ts = dateFormat.parse(buf.toString()).getTime();
							}
		    				catch (ParseException e)
		    				{
		    					throw new IOException("Invalid time stamp " + buf.toString());
							}
		    			}
		    				
		    			if (fields[i].endsWith(param.getCode()) && !(data[i].isEmpty()) && !qualCodes.contains(data[i]))
		    				f[paramIt] = Float.parseFloat(data[i]);
		    			
		    		}
		    		paramIt++;
		    	}
		    	
//		    	System.out.println("time = " + ts);
//		    	System.out.println("site = " + siteId);
//		    	System.out.println("lat = " + siteLat);
//		    	System.out.println("lon = " + siteLon);
		    	
//		    	for (int k=0; k<f.length; k++)
//		    		System.out.println("val " + k + " = " + f[k]);
		    	
		    	DataBlock dataBlock = dataStruct.createDataBlock();
		    	
		    	int blockPos = 0;
		    	dataBlock.setDoubleValue(blockPos++, ts/1000);
		    	dataBlock.setStringValue(blockPos++, siteId);
		    	dataBlock.setDoubleValue(blockPos++, siteLat);
		    	dataBlock.setDoubleValue(blockPos++, siteLon);
		    	
		    	for (int bi=0; bi<f.length; bi++)
		    		dataBlock.setFloatValue(blockPos++, f[bi]);
		    	
		    	latestRecord = dataBlock;
		    	latestRecordTime = System.currentTimeMillis();
		    	eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, USGSWaterOutput.this, dataBlock));
		    	
//		    	System.out.println();
	        }

        }
    	
    }
    
    public String[] buildIvRequest(Map<String, AbstractFeature> fois)
    {
    	String[] idArr = new String[fois.size()];
    	int idIt = 0;
        for (Map.Entry<String, AbstractFeature> entry : fois.entrySet())
        {
        	idArr[idIt] = entry.getValue().getId();
        	idIt++;
        }
        
    	
    	StringBuilder buf = new StringBuilder(BASE_URL);
    	int bufStartLen = buf.length();
    	
    	int numUrl = (int)Math.ceil((double)fois.size()/urlChunkSize);
    	int maxNumSites = (int)Math.ceil((double)fois.size()/numUrl);
    	String[] urlArr = new String[numUrl];
    	
    	int idMarker = 0;
    	for (int i=0; i<numUrl; i++)
    	{
    		for (int k=0; k<maxNumSites; k++)
    		{
    			if (idMarker <= (idArr.length-1))
    			{
    				buf.append(idArr[idMarker]).append(',');
				
					if (idMarker == (idArr.length-1))
						break;
					
					if (k!=(maxNumSites-1))
						idMarker++;
    			}
    		}
			buf.setCharAt(buf.length()-1, '&');
			buf.append("format=rdb"); // output format
			idMarker++;
			
    		urlArr[i] = buf.toString();
    		buf.delete(bufStartLen, buf.length());
    	}
		return urlArr;	
    }
    
    
    @Override
    public double getAverageSamplingPeriod()
    {
        //return 1.0*60*15;
    	//return 60.0;
    	// Make data request every 5 minutes
    	return 5.0*60.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return encoding;
    }


	public void stop()
	{
	    if (timer != null)
        {
            timer.cancel();
            timer = null;
        }		
	}
	
    @Override
    public Collection<String> getEntityIDs()
    {
        return parentSensor.getEntityIDs();
    }


    @Override
    public Map<String, DataBlock> getLatestRecords()
    {
        return Collections.unmodifiableMap(latestRecords);
    }


    @Override
    public DataBlock getLatestRecord(String entityID)
    {
        return latestRecords.get(entityID);
    }
}
