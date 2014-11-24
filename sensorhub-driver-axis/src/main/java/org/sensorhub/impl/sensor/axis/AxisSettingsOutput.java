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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import net.opengis.swe.v20.AllowedValues;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Time;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.vast.data.AllowedValuesImpl;
import org.vast.data.BooleanImpl;
import org.vast.data.CountImpl;
import org.vast.data.DataRecordImpl;
import org.vast.data.QuantityImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.data.TimeImpl;
import org.vast.sweCommon.SWEConstants;

/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol
 * </p>
 *
 * <p>
 * Copyright (c) 2014
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */


public class AxisSettingsOutput implements ISensorDataInterface
{
    AxisCameraDriver driver;
    IEventHandler eventHandler;
    DataComponent settingsDataStruct;
    DataBlock latestRecord;
    boolean polling;

    // Setup ISO Time Components
    // Set TimeZone to "UTC" ????
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

    String ipAddress;


    public AxisSettingsOutput(AxisCameraDriver driver)
    {
        this.driver = driver;
        this.eventHandler = new BasicEventHandler();
    }


    protected void init()
    {

        df.setTimeZone(tz);
        ipAddress = driver.getConfiguration().ipAddress;

        // TODO: Need to generalize this by first checking if PTZ supported

        try
        {
            // request PTZ Limits
            URL optionsURL = new URL("http://" + ipAddress + "/axis-cgi/view/param.cgi?action=list&group=PTZ.Limit");
            InputStream is = optionsURL.openStream();
            BufferedReader limitReader = new BufferedReader(new InputStreamReader(is));

            // set default values
            double minPan = -180.0;
            double maxPan = 180.0;
            double minTilt = -180.0;
            double maxTilt = 0.0;
            double maxZoom = 13333;

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

            // Build SWE Common Data structure
            settingsDataStruct = new DataRecordImpl(3);

            Time t = new TimeImpl();
            t.getUom().setHref(Time.ISO_TIME_UNIT);
            t.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
            settingsDataStruct.addComponent("time", t);

            AllowedValues constraints;
            
            Quantity q = new QuantityImpl(DataType.FLOAT);
            q.getUom().setCode("deg");
            q.setDefinition("http://sensorml.com/ont/swe/property/Pan");
            constraints = new AllowedValuesImpl();
            constraints.addInterval(new double[] {minPan, maxPan});
            q.setConstraint(constraints);
            settingsDataStruct.addComponent("pan", q);

            q = new QuantityImpl(DataType.FLOAT);
            q.getUom().setCode("deg");
            q.setDefinition("http://sensorml.com/ont/swe/property/Tilt");
            constraints = new AllowedValuesImpl();
            constraints.addInterval(new double[] {minTilt, maxTilt});
            q.setConstraint(constraints);
            settingsDataStruct.addComponent("tilt", q);

            Count c = new CountImpl();
            c.setDefinition("http://sensorml.com/ont/swe/property/AxisZoomFactor");
            constraints = new AllowedValuesImpl();
            constraints.addInterval(new double[] {0, maxZoom});
            c.setConstraint(constraints);
            settingsDataStruct.addComponent("zoomFactor", q);

            c = new CountImpl();
            c.setDefinition("http://sensorml.com/ont/swe/property/AxisBrightnessFactor");
            settingsDataStruct.addComponent("brightnessFactor", c);

            net.opengis.swe.v20.Boolean b = new BooleanImpl();
            b.setDefinition("http://sensorml.com/ont/swe/property/AutoFocusEnabled");
            settingsDataStruct.addComponent("autofocus", b);

            // start the thread (probably best not to start in init but in driver start() method.)
            startPolling();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }


    protected void startPolling()
    {
        try
        {
            //String ipAddress = driver.getConfiguration().ipAddress;
            final URL getSettingsUrl = new URL("http://" + ipAddress + "/axis-cgi/com/ptz.cgi?query=position");
            polling = true;

            Thread t = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    DataComponent dataStruct = settingsDataStruct.copy();
                    dataStruct.assignNewDataBlock();

                    while (polling)
                    {
                        // send http query
                        try
                        {
                            InputStream is = getSettingsUrl.openStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            dataStruct.renewDataBlock();

                            // get Time and assign to datablock (put it in UTC?)
                            // should be a SWE Common function; may be a faster way
                            //TimeZone tz = TimeZone.getTimeZone("UTC");
                            //DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                            //df.setTimeZone(tz);

                            // ALEX: should this be a String?
                            dataStruct.getComponent("time").getData().setStringValue(df.format(new Date()));

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
                                else if (tokens[0].trim().equalsIgnoreCase("zoomFactor"))
                                {
                                    float val = Float.parseFloat(tokens[1]);
                                    dataStruct.getComponent("zoom").getData().setFloatValue(val);

                                }
                                else if (tokens[0].trim().equalsIgnoreCase("brightness"))
                                {
                                    float val = Float.parseFloat(tokens[1]);
                                    dataStruct.getComponent("brightnessFactor").getData().setFloatValue(val);

                                }
                                else if (tokens[0].trim().equalsIgnoreCase("autofocus"))
                                {
                                    if (tokens[1].trim().equalsIgnoreCase("on"))
                                        dataStruct.getComponent("autofocus").getData().setBooleanValue(true);
                                    else
                                        dataStruct.getComponent("autofocus").getData().setBooleanValue(false);

                                }
                            }

                            latestRecord = dataStruct.getData();
                            long time = System.currentTimeMillis();
                            eventHandler.publishEvent(new SensorDataEvent(AxisSettingsOutput.this, time, settingsDataStruct, latestRecord));

                            // TODO use a timer; set for every 1 second
                            Thread.sleep(1000);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    ;
                }
            });

            t.start();
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    @Override
    public ISensorModule<?> getSensorInterface()
    {
        return driver;
    }


    @Override
    public boolean isEnabled()
    {
        return true;
    }


    @Override
    public boolean isStorageSupported()
    {
        return false;
    }


    @Override
    public boolean isPushSupported()
    {
        return true;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        // assuming 30 frames per second
        return 1 / 30.0;
    }


    @Override
    public DataComponent getRecordDescription() throws SensorException
    {
        return settingsDataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding() throws SensorException
    {
        // Token = "," Block = "\n"
        return new TextEncodingImpl(",", "\n");
    }


    @Override
    public DataBlock getLatestRecord() throws SensorException
    {
        return latestRecord;
    }


    @Override
    public int getStorageCapacity() throws SensorException
    {
        return 0;
    }


    @Override
    public int getNumberOfAvailableRecords() throws SensorException
    {
        return 1;
    }


    @Override
    public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<DataBlock> getAllRecords(boolean clear) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int clearAllRecords() throws SensorException
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }

}
