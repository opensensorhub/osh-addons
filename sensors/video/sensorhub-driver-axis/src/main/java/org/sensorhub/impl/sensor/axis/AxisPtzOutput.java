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

package org.sensorhub.impl.sensor.axis;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
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
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.vast.data.SWEFactory;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol. This particular class provides output from the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 *
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */

public class AxisPtzOutput extends AbstractSensorOutput<AxisCameraDriver>
{
    DataComponent settingsDataStruct;
    TextEncoding textEncoding;
    boolean polling;
    Timer timer;
    VideoCamHelper videoHelper;

    // Set default timezone to GMT; check TZ in init below
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    URL optionsURL = null;
    URL ptzURL = null;

    public AxisPtzOutput(AxisCameraDriver driver)
    {
        super(driver);
        
        try {

            optionsURL = new URL(parentSensor.getHostUrl() + driver.VAPIX_QUERY_PARAMS_LIST_GROUP_PTZ);
            ptzURL = new URL(parentSensor.getHostUrl() + driver.VAPIX_QUERY_PTZ_PATH);

        } catch (MalformedURLException e) {
           
            e.printStackTrace();
        }
    }


    @Override
    public String getName()
    {
        return "ptzOutput";
    }
  
    
    protected void init()
    {    	
        videoHelper = new VideoCamHelper();
    	
    	SWEFactory fac = new SWEFactory();
        textEncoding =  fac.newTextEncoding();
    	textEncoding.setBlockSeparator("\n");
    	textEncoding.setTokenSeparator(",");

        // set default values
        double minPan = -180.0;
        double maxPan = 180.0;
        double minTilt = -180.0;
        double maxTilt = 0.0;
        double minZoom = 1.0;
        double maxZoom = 13333;
        
        try
        {

            /** request PTZ Limits  **/
            InputStream is = optionsURL.openStream();
            BufferedReader limitReader = new BufferedReader(new InputStreamReader(is));

            // get limit values from IP stream
            String line;
            while ((line = limitReader.readLine()) != null)
            {
                // parse response
                String[] tokens = line.split("=");

                if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MinPan"))
                    minPan = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxPan"))
                    maxPan = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MinTilt"))
                    minTilt = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxTilt"))
                    maxTilt = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxZoom"))
                    maxZoom = Double.parseDouble(tokens[1]);
            }
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Build SWE Common Data structure
        settingsDataStruct = videoHelper.newPtzOutput(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom);   
    }
    
    
    protected void start()
    {
        if (timer != null)
            return;
        timer = new Timer();

        // start the timer thread
        try
        {
         
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
                        is = ptzURL.openStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        dataStruct.renewDataBlock();

                        // set sampling time
                        double time = System.currentTimeMillis() / 1000.;
                        dataStruct.getComponent("time").getData().setDoubleValue(time);

                        String line;

                        short lines = 0;
                        final byte totalReads = 3;

                        while ((line = reader.readLine()) != null)
                        {
                            // parse response
                            String[] tokens = line.split("=");
                            if (tokens[0].trim().equalsIgnoreCase("pan"))
                            {
                                dataStruct.getComponent("pan").getData().setFloatValue(Float.parseFloat(tokens[1]));
                                lines++;
                            }
                            else if (tokens[0].trim().equalsIgnoreCase("tilt"))
                            {
                                dataStruct.getComponent("tilt").getData().setFloatValue(Float.parseFloat(tokens[1]));
                                lines++;
                            }
                            else if (tokens[0].trim().equalsIgnoreCase("zoom"))
                            {
                                dataStruct.getComponent("zoomFactor").getData().setIntValue(Integer.parseInt(tokens[1]));
                                lines++;
                            } 
                            
                        }

                        if (lines == totalReads) {
                            latestRecord = dataStruct.getData();
                            latestRecordTime = System.currentTimeMillis();
                            eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, AxisPtzOutput.this, latestRecord));
                        } else {
                            throw new SensorException("Invalid sensor data from AXIS camera. AXIS url: " + ptzURL.toString());
                        }
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
