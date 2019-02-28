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
import net.opengis.swe.v20.Vector;
import org.vast.swe.SWEConstants;
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
public class VirbXeNavOutput extends AbstractSensorOutput<VirbXeDriver>
{    
    DataComponent navData;
    DataBlock navBlock;
    TextEncoding navEncoding;
    Timer timer;
    
    // set default timezone to GMT; check TZ in init below
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");    

    
    public VirbXeNavOutput(VirbXeDriver parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "navData";
    }


    protected void init() 
    {
        GeoPosHelper fac = new GeoPosHelper();
        
        // Common data record for all vectors
        // build SWE Common record structure
        navData = fac.newDataRecord();
        navData.setName(getName());
        navData.setDefinition("http://sensorml.com/ont/swe/property/stateVectors");
        
        // Time stamp
        navData.addComponent("time", fac.newTimeStampIsoUTC());
      
        // Latitude, Longitude, Altitude
        Vector locVector = fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
        locVector.setLabel("Location");
        locVector.setDescription("Location measured by GPS device");
        navData.addComponent("location", locVector);
        
        // set local reference frame
        String localFrame = parentSensor.getUniqueIdentifier() + "#" + VirbXeDriver.CRS_ID;

        // raw gyro inertial measurements
        Vector angRate = fac.newAngularVelocityVector(null, localFrame, "deg/s");
        angRate.setDataType(DataType.FLOAT);
        navData.addComponent("gyro", angRate);

        // Acceleration
        Vector accel = fac.newAccelerationVector(null, localFrame, "m/s2");
        accel.setDataType(DataType.FLOAT);
        navData.addComponent("accel", accel);
      
        // Acceleration Magnitude 
        Quantity accelMag = fac.newQuantity(GeoPosHelper.DEF_ACCELERATION_MAG, 
        		"Acceleration Magnitude", 
        		"Magnitude of the acceleration Vector", 
        		"m/s2", DataType.FLOAT);
        navData.addComponent("accelMag", accelMag);
         
        navBlock = navData.createDataBlock();
        
        // encoding
        navEncoding = fac.newTextEncoding(",", "\n");
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
	                // send post query
	                try
	                {	                	
	                 	String json = getSensorData();
	                 	
	//                	if (json.equalsIgnoreCase("0"))
	//                		return false;
	              		
	                 	// get new data block
	                 	DataBlock data = navBlock.renew();
	                 	
	                    // set sampling time
	                    double time = System.currentTimeMillis() / 1000.;
	                    data.setDoubleValue(0, time);	                 	

	                	// serialize the DeviceInfo JSON result object
	                	Gson gson = new Gson(); 	
	                  	SensorDataArray sensorArray = gson.fromJson(json, SensorDataArray.class);	
	                  	SensorData[] sensors = sensorArray.sensors;
	                  	
	                  	//  Identify each array component and assign to correct block index	                  	
	                  	for (int i=0; i < sensors.length; i++)
	                  	{
	                  		// check for no data
	                  		if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  		{
	                  			continue;
	                  		}
	                  		else if ((sensors[i].name).equalsIgnoreCase("Latitude"))
	                        {
	                  			double val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Double.NaN;
	                  			else	
	                  				 val = Double.parseDouble(sensors[i].data);
	                            data.setDoubleValue(1, val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("Longitude"))
	                        {
	                  			double val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Double.NaN;
	                  			else	
	                  				 val = Double.parseDouble(sensors[i].data);
	                            data.setDoubleValue(2, val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("Altitude"))
	                        {
	                  			double val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Double.NaN;
	                  			else	
	                  				 val = Double.parseDouble(sensors[i].data);
	                            data.setDoubleValue(3, val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("InternalGyroX"))
	                        {
	                  			float val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Float.NaN;
	                  			else	
	                  				 val = Float.parseFloat(sensors[i].data);
	                            data.setFloatValue(4, val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("InternalGyroY"))
	                        {
	                  			float val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Float.NaN;
	                  			else	
	                            	 val = Float.parseFloat(sensors[i].data);
	                            data.setFloatValue(5, val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("InternalGyroZ"))
	                        {
	                  			float val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Float.NaN;
	                  			else	
	                            	 val = Float.parseFloat(sensors[i].data);
	                            data.setFloatValue(6, val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("InternalAccelX"))
	                        {
	                  			float val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Float.NaN;
	                  			else	
	                            	 val = (float)(Float.parseFloat(sensors[i].data) * 9.81);
	                            data.setFloatValue(7, val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("InternalAccelY"))
	                        {
	                  			float val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Float.NaN;
	                  			else	
	                            	 val = (float)(Float.parseFloat(sensors[i].data) * 9.81);
	                            data.setFloatValue(8, val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("InternalAccelZ"))
	                        {
	                  			float val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Float.NaN;
	                  			else	
	                            	 val = (float)(Float.parseFloat(sensors[i].data) * 9.81);
	                            data.setFloatValue(9, val);
	                        }
	                  		else if ((sensors[i].name).equalsIgnoreCase("InternalAccelG"))
	                        {
	                  			float val;
	                  			if ((sensors[i].has_data).equalsIgnoreCase("0"))
	                  				 val = Float.NaN;
	                  			else	
	                            	 val = (float)(Float.parseFloat(sensors[i].data) * 9.81);
	                            data.setFloatValue(10, val);
	                        }	                			                  		
	                  	}
	                  	
		                latestRecord = data;
		                latestRecordTime = System.currentTimeMillis();
		                eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, VirbXeNavOutput.this, latestRecord)); 	                  		
	                }
	                catch (Exception e)
	                {
	                    parentSensor.getLogger().error("Cannot get navigation data", e);
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
    
    public String getSensorData() throws IOException
    {
    	StringBuffer response = null;
    	BufferedReader in = null;
    	
    	try
    	{
            final URL urlVirb = new URL(parentSensor.getHostUrl() + "/virb");  
    		
    		HttpURLConnection con = (HttpURLConnection) urlVirb.openConnection();    		
    		con.setRequestMethod("POST");
    		con.setConnectTimeout(5000);
 
    		String urlParameters = "{\"command\":\"sensors\"}";
    		
    		con.setDoOutput(true);
    		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    		wr.writeBytes(urlParameters);
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
//    	String type;
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
        return navData;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return navEncoding;
    }
}
