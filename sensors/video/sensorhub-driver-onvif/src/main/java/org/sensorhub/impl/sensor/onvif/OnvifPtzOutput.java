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

package org.sensorhub.impl.sensor.onvif;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.TextEncoding;
import org.onvif.ver10.schema.FloatRange;
import org.onvif.ver10.schema.PTZVector;
import org.onvif.ver10.schema.Profile;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.vast.data.SWEFactory;

import de.onvif.soap.OnvifDevice;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol. This particular class provides output from the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 *
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since June 13, 2017
 */

public class OnvifPtzOutput extends AbstractSensorOutput<OnvifCameraDriver>
{
    DataComponent settingsDataStruct;
    TextEncoding textEncoding;
    Timer timer;
    VideoCamHelper videoHelper;

    // Set default timezone to GMT; check TZ in init below
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

    public OnvifPtzOutput(OnvifCameraDriver driver)
    {
        super(driver);

        // set default values
        double minPan = -180.0;
        double maxPan = 180.0;
        double minTilt = -180.0;
        double maxTilt = 0.0;
        double minZoom = 1.0;
        double maxZoom = 9999;

        FloatRange panSpaces = driver.camera.getPtz().getPanSpaces(driver.profile.getToken());
        minPan = panSpaces.getMin();
        maxPan = panSpaces.getMax();

        FloatRange tiltSpaces = driver.camera.getPtz().getPanSpaces(driver.profile.getToken());
        minTilt = tiltSpaces.getMin();
        maxTilt = tiltSpaces.getMax();

        FloatRange zoomSpaces = driver.camera.getPtz().getPanSpaces(driver.profile.getToken());
        minZoom = zoomSpaces.getMin();
        maxZoom = zoomSpaces.getMax();
        
        // Build SWE Common Data structure
        videoHelper = new VideoCamHelper();
        settingsDataStruct = videoHelper.newPtzOutput(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom);
    }

    protected void init()
    {    	
        videoHelper = new VideoCamHelper();
    	
    	SWEFactory fac = new SWEFactory();
        textEncoding =  fac.newTextEncoding();
    	textEncoding.setBlockSeparator("\n");
    	textEncoding.setTokenSeparator(",");
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
                    // send http query
                    try
                    {
						OnvifDevice camera = parentSensor.camera;
						Profile profile = parentSensor.profile;

                        dataStruct.renewDataBlock();

                        // set sampling time
                        double time = System.currentTimeMillis() / 1000.;
                        dataStruct.getComponent("time").getData().setDoubleValue(time);

						PTZVector ptzVec = camera.getPtz().getPosition(profile.getToken());
						dataStruct.getComponent("pan").getData().setFloatValue(ptzVec.getPanTilt().getX());
						dataStruct.getComponent("tilt").getData().setFloatValue(ptzVec.getPanTilt().getY());
						dataStruct.getComponent("zoomFactor").getData().setIntValue((int) ptzVec.getZoom().getX());

						latestRecord = dataStruct.getData();
						latestRecordTime = System.currentTimeMillis();
						eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OnvifPtzOutput.this, latestRecord));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
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

	public void stop()
	{
	    if (timer != null)
        {
            timer.cancel();
            timer = null;
        }		
	}

    @Override
    public String getName()
    {
        return "ptzOutput";
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
}
