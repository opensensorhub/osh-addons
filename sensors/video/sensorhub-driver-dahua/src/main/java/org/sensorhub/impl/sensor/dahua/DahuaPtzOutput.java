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
import net.opengis.swe.v20.DataBlock;
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
    DataComponent ptzDataStruct;
    TextEncoding textEncoding;
    URL getSettingsUrl;
    Timer timer;
        
    // set default timezone to GMT; check TZ in init below
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");    
    
    // latest PTZ values
    float pan, tilt, zoom;
    

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
        double minZoom = 0.0;
        double maxZoom = 1.0;  // can't retrieve zoom range for Dahua; set high
    	
        // figure out pan and tilt ranges
        try
        {
            URL optionsURL = new URL(parentSensor.getHostUrl() + "/ptz.cgi?action=getCurrentProtocolCaps&channel=0");
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
        	throw new SensorException("Cannot parse PTZ limits", e);
        }

        // generate output structure and encoding
        VideoCamHelper videoHelper = new VideoCamHelper();        
        ptzDataStruct = videoHelper.newPtzOutput(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom);        
        textEncoding = videoHelper.newTextEncoding();
        textEncoding.setBlockSeparator("\n");
        textEncoding.setTokenSeparator(",");
    }


    protected void start() throws SensorException
    {
        if (timer != null)
            return;

        try
        {
            getSettingsUrl = new URL(parentSensor.getHostUrl() + "/ptz.cgi?action=getStatus");
	        final long samplingPeriod = (long)(getAverageSamplingPeriod()*1000);
	        
	        TimerTask timerTask = new TimerTask()
	        {
	            @Override
	            public void run()
	            {
	            	synchronized (DahuaPtzOutput.this)
	            	{
	            	    // only update if control module hasn't pushed an update recently
	            	    if (System.currentTimeMillis() - latestRecordTime >= samplingPeriod/2)
	            	        requestPtzStatus();
	            	}
                }
	        };
	
	        timer = new Timer();
	        timer.scheduleAtFixedRate(timerTask, 0, samplingPeriod);
	    }
	    catch (Exception e)
	    {
	        throw new SensorException("Cannot start PTZ status read task", e);
	    }
    }
    
    
    protected void requestPtzStatus()
    {
        InputStream is = null;
        
        try
        {
            is = getSettingsUrl.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null)
            {
                // parse response
                String[] tokens = line.split("=");

                if (tokens[0].trim().equalsIgnoreCase("status.Postion[0]"))
                    pan = Float.parseFloat(tokens[1]);                    
                else if (tokens[0].trim().equalsIgnoreCase("status.Postion[1]"))
                    tilt = -Float.parseFloat(tokens[1]);                    
                else if (tokens[0].trim().equalsIgnoreCase("status.Postion[2]"))
                    zoom = (float)(Double.parseDouble(tokens[1])/120.0);
            }
            
            sendPtzStatus();            
        }
        catch (Exception e)
        {
            getParentModule().getLogger().error("Error requesting PTZ status", e);
        }
        finally
        {
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
    
    
    protected synchronized void sendPtzStatus()
    {
        // generate new data block
        DataBlock ptzData;
        if (latestRecord == null)
            ptzData = ptzDataStruct.createDataBlock();
        else
            ptzData = latestRecord.renew();
        
        // set sampling time
        long now = System.currentTimeMillis();
        double time = now / 1000.;
        ptzData.setDoubleValue(0, time);
        ptzData.setFloatValue(1, pan);
        ptzData.setFloatValue(2, tilt);
        ptzData.setFloatValue(3, zoom);
        
        latestRecord = ptzData;
        latestRecordTime = now;
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DahuaPtzOutput.this, latestRecord));
    }

    
    @Override
    public double getAverageSamplingPeriod()
    {
        // generating 1 record per second for PTZ settings
        return 10.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return ptzDataStruct;
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
