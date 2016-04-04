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

    // since Dahua doesn't allow you to retrieve current PTZ positions, save the
    // last state here and receive updates from the controller module
    double pan = 0.0;
    double tilt = 0.0;
    double zoom = 1.0;
    
    // maxZoom is used in scaling calculation so put as class variable
    double maxZoom = 100.0;  
    
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
        double maxZoom = 100.0;  // optical zoom for small PTZ (4x) camera
    	
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
        
        // NOTE: can't currently get PTZ position from Dahua API
    	// so we maintain ptz state in the driver class

        final DataComponent dataStruct = settingsDataStruct.copy();
        dataStruct.assignNewDataBlock();

        TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                dataStruct.renewDataBlock();

                // set sampling time
                double time = System.currentTimeMillis() / 1000.;
                dataStruct.getComponent("time").getData().setDoubleValue(time);                
                
                // Set PTZ data using current state provided by DahuaPtzController
                dataStruct.getComponent("pan").getData().setDoubleValue(pan);
                dataStruct.getComponent("tilt").getData().setDoubleValue(tilt);
                
                // convert zoom to between 1.0 to 100.0
                dataStruct.getComponent("zoomFactor").getData().setDoubleValue(zoom * 100.0 /maxZoom);
                //dataStruct.getComponent("zoomFactor").getData().setDoubleValue(zoom);

                latestRecord = dataStruct.getData();
                latestRecordTime = System.currentTimeMillis();
                eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DahuaPtzOutput.this, latestRecord));
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, (long)(getAverageSamplingPeriod()*1000));
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
	
	
	public void setPan(double value)
	{
		pan = value;
	}

	
	public void setTilt(double value)
	{
		tilt = value;
	}
	

	public void setZoom(double value)
	{
		zoom = value;
	}

}
