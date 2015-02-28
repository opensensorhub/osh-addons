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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.AllowedValues;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.TextEncoding;
import net.opengis.swe.v20.Time;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.SWEFactory;
import org.vast.swe.SWEConstants;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol. This particular class provides output from the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 *
 * <p>
 * Copyright (c) 2014
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */

public class AxisSettingsOutput extends AbstractSensorOutput<AxisCameraDriver>
{
    DataComponent settingsDataStruct;
    DataBlock latestRecord;
    boolean polling;
    Timer timer;

    // Setup ISO Time Components
    
    // Set default timezone to GMT; check TZ in init below
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

    String ipAddress;
    
    TextEncoding textEncoding;
    

    public AxisSettingsOutput(AxisCameraDriver driver)
    {
        super(driver);
    }


    @Override
    public String getName()
    {
        return "ptzOutput";
    }
    
    
    protected void init()
    {    	
        SWEFactory fac = new SWEFactory();
        textEncoding =  fac.newTextEncoding();
    	textEncoding.setBlockSeparator("\n");
    	textEncoding.setTokenSeparator(",");

        ipAddress = parentSensor.getConfiguration().ipAddress;

        // set default values
        double minPan = -180.0;
        double maxPan = 180.0;
        double minTilt = -180.0;
        double maxTilt = 0.0;
        double maxZoom = 13333;
//        double minFieldAngle = 44;
//        double maxFieldAngle = 516;

        
        try
        {
        	         
	        /** Need to set TimeZone  **/
        	// getting the time zone should be done in driver class
        	// just using computer time instead of camera time for now
        	// NOTE: SET TIMEZONE TO UTC ON CAMERA OR GET FROM LOCAL SYSTEM OR CONVERT
        	// NOTE: this particular command may have trouble without admin password
            /*URL optionsURL = new URL("http://" + ipAddress + "/axis-cgi/admin/param.cgi?action=list&group=root.Time.TimeZone");
            HttpURLConnection connect = (HttpURLConnection)optionsURL.openConnection();
            connect.addRequestProperty(key, value);
            //InputStream is = optionsURL.openStream();
            BufferedReader limitReader = new BufferedReader(new InputStreamReader(is));
            
            String line;
            while ((line = limitReader.readLine()) != null)
            {
                // parse response
                String[] tokens = line.split("=");

    	        // root.Time.TimeZone=GMT-6
                if (tokens[0].trim().equalsIgnoreCase("root.Time.TimeZone"))
                	df.setTimeZone(TimeZone.getTimeZone(tokens[1]));   	
            }*/


            /** request PTZ Limits  **/
            URL optionsURL = new URL("http://" + ipAddress + "/axis-cgi/view/param.cgi?action=list&group=PTZ.Limit");
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
//                else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MinFieldAngle"))
//                    minFieldAngle = Double.parseDouble(tokens[1]);
//                else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxFieldAngle"))
//                    maxFieldAngle = Double.parseDouble(tokens[1]);
            }
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Build SWE Common Data structure
        // Settings output includes time, pan, tilt, zoom, field angle
        // NOTE: move brightness, autofocus to camera settings

        settingsDataStruct = fac.newDataRecord(4);
        settingsDataStruct.setName(getName());

        // time needs to be in UTC !!!
        // either set camera and convert
        
        Time t = fac.newTime();
        t.getUom().setHref(Time.ISO_TIME_UNIT);
        t.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
        t.setLabel("Time");
        settingsDataStruct.addComponent("time", t);

        AllowedValues constraints;
        
        Quantity q = fac.newQuantity(DataType.FLOAT);
        q.getUom().setCode("deg");
        q.setDefinition("http://sensorml.com/ont/swe/property/Pan");
        constraints = fac.newAllowedValues();
        constraints.addInterval(new double[] {minPan, maxPan});
        q.setConstraint(constraints);
        q.setLabel("Pan");
        settingsDataStruct.addComponent("pan", q);

        q = fac.newQuantity(DataType.FLOAT);
        q.getUom().setCode("deg");
        q.setDefinition("http://sensorml.com/ont/swe/property/Tilt");
        constraints = fac.newAllowedValues();
        constraints.addInterval(new double[] {minTilt, maxTilt});
        q.setConstraint(constraints);
        q.setLabel("Tilt");
        settingsDataStruct.addComponent("tilt", q);

        Count c = fac.newCount();
        c.setDefinition("http://sensorml.com/ont/swe/property/AxisZoomFactor");
        constraints = fac.newAllowedValues();
        constraints.addInterval(new double[] {1, maxZoom});
        c.setConstraint(constraints);
        c.setLabel("Zoom Factor");
        settingsDataStruct.addComponent("zoomFactor", c);

//		  NOTE: current field angle is not returned by position request       
//        c = fac.newCount();
//        c.setDefinition("http://sensorml.com/ont/swe/property/CameraFieldAngle");
//        constraints = fac.newAllowedValues();
//        constraints.addInterval(new int[] {minFieldAngle, maxFieldAngle});
//        c.setConstraint(constraints);
//        q.setLabel("Field Angle");
//        settingsDataStruct.addComponent("fieldAngle", q);

//		  MOVE THESE TO CAMERA SETTINGS      
//        c = fac.newCount();
//        c.setDefinition("http://sensorml.com/ont/swe/property/AxisBrightnessFactor");
//        settingsDataStruct.addComponent("brightnessFactor", c);
//
//        net.opengis.swe.v20.Boolean b = fac.newBoolean();
//        b.setDefinition("http://sensorml.com/ont/swe/property/AutoFocusEnabled");
//        q.setLabel("Autofocus");
//        settingsDataStruct.addComponent("autofocus", b);

        // start the thread (probably best not to start in init but in driver start() method.) ????
        startPolling();


    }


    protected void startPolling()
    {
        if (timer != null)
            return;
        timer = new Timer();
        
        try
        {
            //String ipAddress = driver.getConfiguration().ipAddress;
            final URL getSettingsUrl = new URL("http://" + ipAddress + "/axis-cgi/com/ptz.cgi?query=position");
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

                            if (tokens[0].trim().equalsIgnoreCase("pan"))
                            {
                                float val = Float.parseFloat(tokens[1]);
                                dataStruct.getComponent("pan").getData().setFloatValue(val);
                            }
                            else if (tokens[0].trim().equalsIgnoreCase("tilt"))
                            {
                                float val = Float.parseFloat(tokens[1]);
                                dataStruct.getComponent("tilt").getData().setFloatValue(val);
                            }
                            else if (tokens[0].trim().equalsIgnoreCase("zoom"))
                            {
                                int val = Integer.parseInt(tokens[1]);
                                dataStruct.getComponent("zoomFactor").getData().setIntValue(val);
                            }
                            // NOTE: position doesn't return field angle !!!
//                            else if (tokens[0].trim().equalsIgnoreCase("fieldAngle"))
//                            {
//                                int val = Integer.parseInteger(tokens[1]);
//                                dataStruct.getComponent("fieldAngle").getData().setIntValue(val);
//
//                            }
                              // MOVE TO CAMERA SETTINGS?
//                            else if (tokens[0].trim().equalsIgnoreCase("brightness"))
//                            {
//                                float val = Float.parseFloat(tokens[1]);
//                                dataStruct.getComponent("brightnessFactor").getData().setFloatValue(val);
//
//                            }
//                            else if (tokens[0].trim().equalsIgnoreCase("autofocus"))
//                            {
//                                if (tokens[1].trim().equalsIgnoreCase("on"))
//                                    dataStruct.getComponent("autofocus").getData().setBooleanValue(true);
//                                else
//                                    dataStruct.getComponent("autofocus").getData().setBooleanValue(false);
//
//                            }
                        }

                        latestRecord = dataStruct.getData();                            
                        eventHandler.publishEvent(new SensorDataEvent(time, AxisSettingsOutput.this, latestRecord));
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


    @Override
    public DataBlock getLatestRecord()
    {
        return latestRecord;
    }


    @Override
    public double getLatestRecordTime()
    {
        if (latestRecord != null)
            return latestRecord.getDoubleValue(0); // first component is sampling time
        
        return Double.NaN;
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
