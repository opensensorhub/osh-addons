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

package org.sensorhub.impl.sensor.waterdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.TextEncoding;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import com.google.gson.Gson;


/**
 * <p>
 * Implementation of sensor interface for USGS Water Data using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Lee Butler <labutler10@gmail.com>
 * @since October 30, 2014
 */

public class WaterDataOutput extends AbstractSensorOutput<WaterDataDriver>
{
	WaterML2 waterData;
	
	// Define format of incoming time stamp
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
    DataRecord waterRecordStruct;
    TextEncoding waterRecordEncoding;
    Timer timer;

    public WaterDataOutput(WaterDataDriver driver)
    {
        super(driver);
    }


    @Override
    public String getName()
    {
        return "waterData";
    }
  
    
    protected void init()
    {   	
        SWEHelper fac = new SWEHelper();
        GeoPosHelper geo = new GeoPosHelper();
        
        // build SWE Common record structure
        waterRecordStruct = fac.newDataRecord(6);
        waterRecordStruct.setName(getName());
        waterRecordStruct.setDefinition("http://sensorml.com/ont/swe/property/StreamData");
        waterRecordStruct.addComponent("time", fac.newTimeStampIsoUTC());
        waterRecordStruct.addComponent("siteCode", fac.newText("http://sensorml.com/ont/swe/property/StationID", "Site ID", null));
        waterRecordStruct.addComponent("siteName", fac.newText("http://sensorml.com/ont/swe/property/StationName", "Site Name", null));
        waterRecordStruct.addComponent("location", geo.newLocationVectorLatLon(SWEConstants.DEF_SENSOR_LOC));
        waterRecordStruct.addComponent("discharge", fac.newQuantity("http://sensorml.com/ont/swe/property/StreamDischarge", "Stream Discharge", null, "ft3/s"));
        waterRecordStruct.addComponent("gaugeHeight", fac.newQuantity("http://sensorml.com/ont/swe/property/GaugeHeight", "Gauge Height", null, "ft"));
        
        // default encoding is text
        waterRecordEncoding = fac.newTextEncoding("$$", "\n");           
    }
    
    
    protected void start() throws SensorHubException
    {
    	if (timer != null)
            return;
        
		try
		{
			TimerTask timerTask = new TimerTask()
			{
				@Override
				public void run()
				{
	          	try
	          	{
	          		PublishData();
	          	}
	          	
	          	catch (Exception e)
	          	{
	          	}
	          }
	      };
	      timer = new Timer();
	      timer.scheduleAtFixedRate(timerTask, 0, (long)(getAverageSamplingPeriod()*1000));
	    }
		catch (Exception e)
		{
			e.printStackTrace();
		}  
    }
    
    public void PublishData() throws SensorHubException, IOException, ParseException
    {
    	URL waterURL = new URL(parentSensor.jsonURL);
    	InputStream isData = waterURL.openStream();
    	BufferedReader reader = null;
    	try
    	{
    		reader = new BufferedReader(new InputStreamReader(isData));
    		StringBuffer response = new StringBuffer();
	    	String line;
	    	
	    	while ((line = reader.readLine()) != null)
	    	{
	    		response.append(line);
	    	}
	    	
	    	Gson gsonL = new Gson();
	    	waterData = gsonL.fromJson(response.toString(), WaterML2.class);
	    	
//	    	System.out.println("");
//	    	System.out.println("time = " + waterData.value.timeSeries[0].values[0].value[0].dateTime);
//	    	System.out.println("siteCode = " + waterData.value.timeSeries[0].sourceInfo.siteCode[0].value);
//	    	System.out.println("siteName = " + waterData.value.timeSeries[0].sourceInfo.siteName);
//	    	System.out.println("lat = " + waterData.value.timeSeries[0].sourceInfo.geoLocation.geogLocation.latitude);
//	    	System.out.println("lon = " + waterData.value.timeSeries[0].sourceInfo.geoLocation.geogLocation.longitude);
//	    	System.out.println("discharge = " + waterData.value.timeSeries[0].values[0].value[0].value);
//	    	System.out.println("gaugeHeight = " + waterData.value.timeSeries[1].values[0].value[0].value);
	    	
	    	//dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	    	Date date = dateFormat.parse(waterData.value.timeSeries[0].values[0].value[0].dateTime);
	    	long seconds = date.getTime();
	    	
	        DataBlock dataBlock;
	    	if (latestRecord == null)
	    	    dataBlock = waterRecordStruct.createDataBlock();
	    	else
	    	    dataBlock = latestRecord.renew();
	    	
	    	dataBlock.setDoubleValue(0, seconds/1000);
	    	dataBlock.setStringValue(1, waterData.value.timeSeries[0].sourceInfo.siteCode[0].value);
	    	dataBlock.setStringValue(2, waterData.value.timeSeries[0].sourceInfo.siteName);
	    	dataBlock.setDoubleValue(3, waterData.value.timeSeries[0].sourceInfo.geoLocation.geogLocation.latitude);
	    	dataBlock.setDoubleValue(4, waterData.value.timeSeries[0].sourceInfo.geoLocation.geogLocation.longitude);
	    	dataBlock.setDoubleValue(5, Double.parseDouble(waterData.value.timeSeries[0].values[0].value[0].value));
	    	dataBlock.setDoubleValue(6, Double.parseDouble(waterData.value.timeSeries[1].values[0].value[0].value));
	        // update latest record and send event
	        latestRecord = dataBlock;
	        latestRecordTime = System.currentTimeMillis();
	        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, WaterDataOutput.this, dataBlock));  
    	}
    	
    	catch (IOException e)
        {
            throw new IOException("Cannot read server response", e);
        }
    	finally
    	{
    		if (reader != null)
    			reader.close();
    		if (isData != null)
    			isData.close();
    	}	
    }

    
    @Override
    public double getAverageSamplingPeriod()
    {
        // generating 1 record per second for PTZ settings
        return 1.0*60*15;
    	//return 2.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return waterRecordStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return waterRecordEncoding;
    }


	public void stop()
	{
	    if (timer != null)
        {
            timer.cancel();
            timer = null;
        }		
	}
}
