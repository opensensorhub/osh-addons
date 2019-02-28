/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Botts Innovative Research Inc. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.virbxe;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.api.sensor.SensorDataEvent;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.TextEncoding;
import org.vast.swe.helper.GeoPosHelper;
import com.google.gson.Gson;


/**
 * <p>
 * Implementation of the Garmin Navigation Output
 * </p>
 *
 * @author Mike Botts
 * @since April 14, 2016
 */
public class VirbXeAntOutput extends AbstractSensorOutput<VirbXeDriver>
{    
    DataComponent healthData;
    DataBlock healthBlock;
    TextEncoding textEncoding;
    Timer timer;
    
    // set default timezone to GMT; check TZ in init below
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");    

	boolean healthSensorSupported = false;

    
    public VirbXeAntOutput(VirbXeDriver parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "healthSensors";
    }
    

    public boolean hasSensors()
    {
    	return healthSensorSupported;
    }
    
    
    protected void init() 
    {    	
        GeoPosHelper fac = new GeoPosHelper();
        
        // Common data record for all vectors
        // start SWE Common record structure
        healthData = fac.newDataRecord(5);
        healthData.setName(getName());
        healthData.setDefinition("http://sensorml.com/ont/swe/property/stateVectors");
        
        // Time stamp
        healthData.addComponent("time", fac.newTimeStampIsoUTC());
           
    	// now check to see if there ARE any ANT/BlueTooth sensors and not just "LOCAL"
    	 	
    	try
        {        	
//         	String json = getSensorData( "{\"command\":\"sensors\",\"names\":[\"HeartRate\"]}"  );
        	
         	String json = getSensorData( "{\"command\":\"sensors\"}"  );
       	
            if (json.equalsIgnoreCase("0"))
                	return;   //unsuccessful connection to Garmin
      		
        	Gson gson = new Gson(); 	
          	SensorDataArray sensorArray = gson.fromJson(json, SensorDataArray.class);      	
          	SensorData[] sensors = sensorArray.sensors;
         	
          	for (int i=0; i < sensors.length; i++)
          	{
          		if ((sensors[i].type).equalsIgnoreCase("LOCAL"))
          			continue;
          		
          		else if ((sensors[i].name).equalsIgnoreCase("HeartRate"))
          		{
          			healthSensorSupported = true;
          		
	                Quantity heartRate = fac.newQuantity("http://sensorml.com/ont/swe/property/heartRate", 
	                		"Heart Rate", 
	                		"The rate of heartbeat", 
	                		"{hb}/min", DataType.FLOAT);
	                 healthData.addComponent("heartRate", heartRate);
                 
          		}
          		else if ((sensors[i].name).equalsIgnoreCase("Cadence"))
          		{
          			healthSensorSupported = true;
          		
	                Quantity cadence = fac.newQuantity("http://sensorml.com/ont/swe/property/cadence", 
	                		"Cadence", 
	                		"Rate of rhythmic movement ", 
	                		"{rev}/min", DataType.FLOAT);
	                 healthData.addComponent("cadence", cadence);
                 
          		}
          		else if ((sensors[i].name).equalsIgnoreCase("FootSpeed"))
          		{
          			healthSensorSupported = true;
          		
	                Quantity footSpeed = fac.newQuantity("http://sensorml.com/ont/swe/property/footSpeed", 
	                		"Foot Speed", 
	                		"user's speed detected by foot pod ", 
	                		"m/s", DataType.DOUBLE);
	                 healthData.addComponent("footSpeed", footSpeed);                 
          		}
          		else if ((sensors[i].name).equalsIgnoreCase("Temperature"))
          		{
          			healthSensorSupported = true;
          		
	                Quantity temperature = fac.newQuantity("http://sensorml.com/ont/swe/property/temperature", 
	                		"Temperature", 
	                		"Reported temperature", 
	                		"degC", DataType.DOUBLE);
	                 healthData.addComponent("temperature", temperature);
                 
          		}
          		else if ((sensors[i].name).equalsIgnoreCase("WheelSpeed"))
          		{
          			healthSensorSupported = true;
          		
	                Quantity wheelSpeed = fac.newQuantity("http://sensorml.com/ont/swe/property/wheelSpeed", 
	                		"Wheel Speed", 
	                		"user's speed detected by wheel sensor", 
	                		"m/s", DataType.DOUBLE);
	                 healthData.addComponent("wheelSpeed", wheelSpeed);
                 
          		}
           		        		
          		if (healthSensorSupported)
          		    healthBlock = healthData.createDataBlock();
        		
          		// encoding
          		textEncoding = fac.newTextEncoding(",", "\n");
          	}                	           	              	
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
 	       
    }
    
    protected void start()
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
	                 	String json = getSensorData( "{\"command\":\"sensors\"}"  );
	                 	
	//                	if (json.equalsIgnoreCase("0"))
	//                		return false;
	                 	
	                 	// get new data block
                        DataBlock data = healthBlock.renew();
	                 	
	                    // set sampling time
	                    double time = System.currentTimeMillis() / 1000.;
	                    data.setDoubleValue(0, time);	                 	

	                	// serialize the DeviceInfo JSON result object
	                	Gson gson = new Gson(); 	
	                  	SensorDataArray sensorArray = gson.fromJson(json, SensorDataArray.class);
	                  	SensorData[] sensors = sensorArray.sensors;	                  	
	                  	
	                  	// TODO general this by setting all values to double and Caps for field names
	                  	for (int i=0; i < sensors.length; i++)
	                  	{	                  		
	                  		// skip if a navigation sensor
	                  		if ((sensors[i].type).equalsIgnoreCase("LOCAL"))
	                  			continue;
	                  		
	                  		else if ((sensors[i].name).equalsIgnoreCase("HeartRate"))
	                        {
	                  			float val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Float.NaN;
	                  			else	
	                  				 val = Float.parseFloat(sensors[i].data);
	                            data.setFloatValue(healthData.getComponentIndex("heartRate"), val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("Cadence"))
	                        {
	                  			float val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Float.NaN;
	                  			else	
	                  				 val = Float.parseFloat(sensors[i].data);
	                            data.setFloatValue(healthData.getComponentIndex("cadence"), val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("FootSpeed"))
	                        {
	                  			double val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Double.NaN;
	                  			else	
	                  				 val = Double.parseDouble(sensors[i].data);
	                            data.setDoubleValue(healthData.getComponentIndex("footSpeed"), val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("Temperature"))
	                        {
	                  			double val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Double.NaN;
	                  			else	
	                  				 val = Double.parseDouble(sensors[i].data);
	                            data.setDoubleValue(healthData.getComponentIndex("temperature"), val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("WheelSpeed"))
	                        {
	                  			double val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Double.NaN;
	                  			else	
	                  				 val = Double.parseDouble(sensors[i].data);
	                            data.setDoubleValue(healthData.getComponentIndex("wheelSpeed"), val);
	                        }
	                  			                  			                  		
	                  	}
	                  	
	                    latestRecord = data;
	                    latestRecordTime = System.currentTimeMillis();
	                    eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, VirbXeAntOutput.this, latestRecord));	 	                  		
	                  	
	                }
	                catch (Exception e)
	                {
	                    parentSensor.getLogger().error("Cannot get ANT sensor data", e);
	                }
	            }         	           	              	
	        };

	        timer = new Timer();
	        timer.scheduleAtFixedRate(timerTask, 0, (long)(getAverageSamplingPeriod()*1000));
	    }
	    catch (Exception e)
	    {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }   	
        
    }
    
    public String getSensorData(String command) throws IOException
    {
    	StringBuffer response = null;
    	BufferedReader in = null;
    	
    	try
        {
            final URL urlVirb = new URL(parentSensor.getHostUrl() + "/virb");  
            
            HttpURLConnection con = (HttpURLConnection) urlVirb.openConnection();    		
            con.setRequestMethod("POST");
 
            	con.setDoOutput(true);
            	DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            	wr.writeBytes(command);
            	wr.flush();
            	wr.close();

            	// check response code for an error
            String responseCode = Integer.toString(con.getResponseCode());
            if ((responseCode.equalsIgnoreCase("-1")) || (responseCode.equalsIgnoreCase("401")))
            	return "0";
            
            // read response line by line to string buffer
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            response = new StringBuffer();
            while ((inputLine = in.readLine()) != null)
            	response.append(inputLine);
            
            return response.toString();
        }
        finally
        {       
            if (in != null)
                in.close();
        }
    }
    

    // Class to serialize JSON response for "sensors"
    private class SensorData
    {     	
    	String name;
    	String type;
    	String has_data;
//    	String units;
//    	String data_type;
    	String data;  	
     }
    
    private class SensorDataArray
    {
    	SensorData[] sensors;
    }
    

    protected void stop()
    {
    	if (timer != null)
    	{
	        timer.cancel();
	        timer = null;
    	}
    }


    @Override
    public double getAverageSamplingPeriod()
    {
    	return 1.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return healthData;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return textEncoding;
    }
}
