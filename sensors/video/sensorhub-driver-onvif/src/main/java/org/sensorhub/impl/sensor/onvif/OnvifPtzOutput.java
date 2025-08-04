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
import org.onvif.ver10.schema.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.SWEFactory;

import de.onvif.soap.OnvifDevice;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol. This particular class provides output from the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 *
 * 
 * @author Kyle Fitzpatrick, Joshua Wolfe <developer.wolfe@gmail.com>
 * @since June 13, 2017
 */

public class OnvifPtzOutput extends AbstractSensorOutput<OnvifCameraDriver>
{
    private static final Logger log = LoggerFactory.getLogger(OnvifPtzOutput.class);

    DataComponent settingsDataStruct;
    TextEncoding textEncoding;
    Timer timer;
    VideoCamHelper videoHelper;
    OnvifDevice camera = null;
    Profile profile = null;
    boolean hasStatus = true;
    boolean hasPan = true;
    boolean hasTilt = true;
    boolean hasZoom = true;
    SWEHelper helper = new  SWEHelper();
    DataComponent dataStruct;
    PTZStatus ptzStatus = null;

    // Set default timezone to GMT; check TZ in init below
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

    public OnvifPtzOutput(OnvifCameraDriver driver)
    {
        super("ptzOutput", driver);
        VideoCamHelper videoHelper = new VideoCamHelper();

        // set default values
        double minPan = -180.0;
        double maxPan = 180.0;
        double minTilt = -180.0;
        double maxTilt = 0.0;
        double minZoom = 1.0;
        double maxZoom = 9999;

        try {
            if (driver.ptzProfile != null) {
                PTZConfiguration devicePtzConfig = driver.ptzProfile.getPTZConfiguration();
                if (devicePtzConfig != null) {
                    PanTiltLimits panTiltLimits = devicePtzConfig.getPanTiltLimits();
                    if (panTiltLimits != null) {
                        minPan = panTiltLimits.getRange().getXRange().getMin();
                        maxPan = panTiltLimits.getRange().getXRange().getMax();
                        minTilt = panTiltLimits.getRange().getYRange().getMin();
                        maxTilt = panTiltLimits.getRange().getYRange().getMax();
                    }
                    ZoomLimits zoomLimits = devicePtzConfig.getZoomLimits();
                    if (zoomLimits != null) {
                        minZoom = zoomLimits.getRange().getXRange().getMin();
                        maxZoom = zoomLimits.getRange().getXRange().getMax();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not determine PTZ limits.", e);
        }
        // Build SWE Common Data structure
        settingsDataStruct = videoHelper.newPtzOutput(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom);
    }

    protected void init()
    {

    	
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

        camera = parentSensor.camera;
        profile = parentSensor.ptzProfile;

        // Immediately stop ptz output if ptz is not supported
        if (profile == null) {
            stop();
            return;
        }

        // start the timer thread
        try
        {

            dataStruct = settingsDataStruct.copy();
            dataStruct.assignNewDataBlock();
            try {
                ptzStatus = camera.getPtz().getStatus(profile.getToken());
            } catch (Exception e) {
                hasStatus = false;
                getLogger().warn("Could not get ptz status.", e);
            }
            TimerTask timerTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    // send http query
                    try
                    {
                        dataStruct.renewDataBlock();

                        // set sampling time
                        double time = System.currentTimeMillis() / 1000.;
                        dataStruct.getComponent("time").getData().setDoubleValue(time);
                        ptzStatus = camera.getPtz().getStatus(profile.getToken());

                        if (ptzStatus != null) {
                            PTZVector ptzVec = ptzStatus.getPosition();

                            if (ptzVec != null) {
                                try {
                                    if (hasPan) {
                                        dataStruct.getComponent("pan").getData().setDoubleValue(parent.genericToDeg(ptzVec.getPanTilt().getX(), 0));
                                    }
                                } catch (Exception e) {
                                    hasPan = false;
                                    log.warn("Could not get pan, ignoring.", e);
                                }
                                try {
                                    if (hasTilt) {
                                        dataStruct.getComponent("tilt").getData().setDoubleValue(parent.genericToDeg(ptzVec.getPanTilt().getY(), 1));
                                    }
                                } catch (Exception e) {
                                    hasTilt = false;
                                    log.warn("Could not get tilt, ignoring.", e);
                                }
                                try {
                                    if (hasZoom) {
                                        dataStruct.getComponent("zoomFactor").getData().setDoubleValue(parent.genericToDeg(ptzVec.getZoom().getX(), 2));
                                    }
                                } catch (Exception e) {
                                    hasZoom = false;
                                    log.warn("Could not get zoom, ignoring.", e);
                                }
                            }
                        }


						latestRecord = dataStruct.getData();
						latestRecordTime = System.currentTimeMillis();
						eventHandler.publish(new DataEvent(latestRecordTime, OnvifPtzOutput.this, latestRecord));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            };

            if (hasStatus) {
                timer.scheduleAtFixedRate(timerTask, 0, (long) (getAverageSamplingPeriod() * 1000));
            }
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
