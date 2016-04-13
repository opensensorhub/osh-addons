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

package org.sensorhub.impl.sensor.dahua;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.TextEncoding;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;


/**
 * <p>
 * Implementation of sensor interface for generic Dahua Cameras using IP
 * protocol. This particular class provides output from the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 *
  * @author Mike Botts <mike.botts@botts-inc.com>
 * @since March 2016
 */
public class DahuaPtzOutput extends AbstractSensorOutput<DahuaCameraDriver>
{
    DataComponent settingsDataStruct;
    TextEncoding textEncoding;
    Timer timer;
        
    // set default timezone to GMT; check TZ in init below
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");    
    

    public DahuaPtzOutput(DahuaCameraDriver driver)
    {
        super(driver);
    }


    @Override
    public String getName()
    {
        return "ptzOutput";
    }
    
    
    protected void init() throws SensorException
    {
        // set default PTZ min-max values
        double minPan = 0.0;
        double maxPan = 360.0;
        double minTilt = 0.0;
        double maxTilt = 90.0;
        double minZoom = 1.0;
        double maxZoom = 10000.0;  // can't retrieve zoom range for Dahua; set high
    	
        // figure out pan and tilt ranges
        try
        {
            URL optionsURL = new URL("http://" + parentSensor.getHostName() + "/cgi-bin/ptz.cgi?action=getCurrentProtocolCaps&channel=0");
        	InputStream is = optionsURL.openStream();
        	BufferedReader bReader = new BufferedReader(new InputStreamReader(is));

        	// get limit values from IP stream
        	String line;
        	while ((line = bReader.readLine()) != null)
        	{
        		// parse response
        		String[] tokens = line.split("=");

        		if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.HorizontalAngle[0]"))
        			minPan = Double.parseDouble(tokens[1]);
        		else if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.HorizontalAngle[1]"))
        			maxPan = Double.parseDouble(tokens[1]);
        		else if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.VerticalAngle[0]"))
        			minTilt = Double.parseDouble(tokens[1]);
        		else if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.VerticalAngle[1]"))
        			maxTilt = Double.parseDouble(tokens[1]);
        		// can't currently get maxZoom from Dahua API
        		//else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxZoom"))
        		//    maxZoom = Double.parseDouble(tokens[1]);
        	}
        }
        catch (IOException e)
        {
        	throw new SensorException("", e);
        }

        // generate output structure and encoding
        VideoCamHelper videoHelper = new VideoCamHelper();        
        settingsDataStruct = videoHelper.getPtzOutput(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom);        
        textEncoding = videoHelper.newTextEncoding();
        textEncoding.setBlockSeparator("\n");
        textEncoding.setTokenSeparator(",");
    }


    protected void start()
    {
        if (timer != null)
            return;

        try{
        	
	        final URL getSettingsUrl = new URL("http://" + parentSensor.getHostName() + "/cgi-bin/ptz.cgi?action=getStatus");
	        
	        final DataComponent dataStruct = settingsDataStruct.copy();
	        dataStruct.assignNewDataBlock();
	
	        TimerTask timerTask = new TimerTask()
	        {
	            @Override
	            public void run()
	            {
	            	
                    InputStream is = null;
                    
                    // send http query
                    try
                    {
                        is = getSettingsUrl.openStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        dataStruct.renewDataBlock();

                        // set sampling time
                        double time = System.currentTimeMillis() / 1000.;
                        dataStruct.getComponent("time").getData().setDoubleValue(time);

                        String line;
                        while ((line = reader.readLine()) != null)
                        {
                            // parse response
                            String[] tokens = line.split("=");

                            if (tokens[0].trim().equalsIgnoreCase("status.Postion[0]"))
                            {
                                float val = Float.parseFloat(tokens[1]);
                                dataStruct.getComponent("pan").getData().setFloatValue(val);
                            }
                            else if (tokens[0].trim().equalsIgnoreCase("status.Postion[1]"))
                            {
                                float val = Float.parseFloat(tokens[1]);
                                dataStruct.getComponent("tilt").getData().setFloatValue(val);
                            }
                            else if (tokens[0].trim().equalsIgnoreCase("status.Postion[2]"))
                            {
                                float val = Float.parseFloat(tokens[1]);
                                dataStruct.getComponent("zoomFactor").getData().setFloatValue(val);
                            }
                        }
 	
		                latestRecord = dataStruct.getData();
		                latestRecordTime = System.currentTimeMillis();
		                eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DahuaPtzOutput.this, latestRecord));
 
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        // always close the stream even in case of error
                        try
                        {
                            if (is != null)
                                is.close();
                        }
                        catch (IOException e)
                        {
                        }
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

    
    @Override
    public double getAverageSamplingPeriod()
    {
        // generating 1 record per second for PTZ settings
        return 1.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return settingsDataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return textEncoding;
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
