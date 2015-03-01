/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.nexrad;

import java.util.Timer;
import java.util.TimerTask;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;


public class NexradOutput extends AbstractSensorOutput<NexradSensor>
{
    private static final Logger log = LoggerFactory.getLogger(NexradOutput.class);
    DataComponent posDataStruct;
    DataBlock latestRecord;
    boolean sendData;
    Timer timer;
    double currentTrackPos;
    

    public NexradOutput(NexradSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "NexradData";
    }


    @Override
    protected void init()
    {
        // SWE Common data structure
//        posDataStruct = new DataRecordImpl(3);
//        posDataStruct.setName(getName());
//        posDataStruct.setDefinition("http://sensorml.com/ont/swe/property/Location");
//        
//        Time c1 = new TimeImpl();
//        c1.getUom().setHref(Time.ISO_TIME_UNIT);
//        c1.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
//        posDataStruct.addComponent("time", c1);
//
//        Quantity c;
//        c = new QuantityImpl();
//        c.getUom().setCode("deg");
//        c.setDefinition("http://sensorml.com/ont/swe/property/Latitude");
//        c.setReferenceFrame("http://www.opengis.net/def/crs/EPSG/0/4979");
//        c.setAxisID("Lat");
//        posDataStruct.addComponent("lat",c);
//
//        c = new QuantityImpl();
//        c.getUom().setCode("deg");
//        c.setDefinition("http://sensorml.com/ont/swe/property/Longitude");
//        c.setReferenceFrame("http://www.opengis.net/def/crs/EPSG/0/4979");
//        c.setAxisID("Long");
//        posDataStruct.addComponent("lon", c);
//
//        c = new QuantityImpl();
//        c.getUom().setCode("m");
//        c.setDefinition("http://sensorml.com/ont/swe/property/Altitude");
//        c.setReferenceFrame("http://www.opengis.net/def/crs/EPSG/0/4979");
//        c.setAxisID("h");
//        posDataStruct.addComponent("alt", c);        
    }


    private void sendMeasurement()
    {
        // build and publish datablock
//        DataBlock dataBlock = posDataStruct.createDataBlock();
//        dataBlock.setDoubleValue(0, time);
//        dataBlock.setDoubleValue(1, lat);
//        dataBlock.setDoubleValue(2, lon);
//        dataBlock.setDoubleValue(3, alt);
//        
//        // update latest record and send event
//        latestRecord = dataBlock;
//        eventHandler.publishEvent(new SensorDataEvent(time, NexradOutput.this, dataBlock));
    }


    protected void start()
    {
        if (timer != null)
            return;
        timer = new Timer();
        
        // start main measurement generation thread
        TimerTask task = new TimerTask() {
            public void run()
            {
                sendMeasurement();
            }            
        };
        
        timer.scheduleAtFixedRate(task, 0, (long)(getAverageSamplingPeriod()*1000));        
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
        return posDataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return new TextEncodingImpl(",", "\n");
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
            return latestRecord.getDoubleValue(0);
        
        return Double.NaN;
    }

}
