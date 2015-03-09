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

import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Time;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataRecordImpl;
import org.vast.data.QuantityImpl;
import org.vast.data.SWEFactory;
import org.vast.data.TextEncodingImpl;
import org.vast.data.TimeImpl;
import org.vast.swe.SWEConstants;


public class NexradOutput extends AbstractSensorOutput<NexradSensor>
{
    private static final Logger log = LoggerFactory.getLogger(NexradOutput.class);
    DataComponent nexradStruct;
    DataBlock latestRecord;
    boolean sendData;
    Timer timer;
    static int NUM_BINS = 720;  // this should be fixed at construction time as part of the config

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
        nexradStruct = new DataRecordImpl(3);
        nexradStruct.setName(getName());
        nexradStruct.setDefinition("http://sensorml.com/ont/swe/property/Location");
    	
    	//  Time,el,az,data[]
        Time c1 = new TimeImpl();
        c1.getUom().setHref(Time.ISO_TIME_UNIT);
        c1.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
        nexradStruct.addComponent("time", c1);

        Quantity c;
        c = new QuantityImpl();
        c.getUom().setCode("deg");
        c.setDefinition("http://sensorml.com/ont/swe/property/ElevationAngle");
//        c.setReferenceFrame("http://www.opengis.net/def/crs/EPSG/0/4979");
        nexradStruct.addComponent("elevation",c);

        c = new QuantityImpl();
        c.getUom().setCode("deg");
        c.setDefinition("http://sensorml.com/ont/swe/property/AzimuthAngle");
//        c.setReferenceFrame("http://www.opengis.net/def/crs/EPSG/0/4979");
        nexradStruct.addComponent("azimuth",c);

		SWEFactory fac = new SWEFactory();
		
        DataArray data = fac.newDataArray(NUM_BINS);
//        data.getUom() - how to set units
        data.setDefinition("http://sensorml.com/ont/swe/propertyx/values");  // does not exist- will be reflectivity,velocity,or spectrumWidth- choice here?
        nexradStruct.addComponent("data", data);
    }


    private void sendMeasurement()
    {
        // build and publish datablock
        DataBlock dataBlock = nexradStruct.createDataBlock();
        double time = System.currentTimeMillis() / 1000.;
        dataBlock.setDoubleValue(0, time);
        dataBlock.setDoubleValue(1, 0.5);
        dataBlock.setDoubleValue(2, 0.0);
        //  
        double [] bins = new double[NUM_BINS];
        dataBlock.setUnderlyingObject(bins);
        
//        dataBlock.set
        //        
//        // update latest record and send event
//        latestRecord = dataBlock;
        eventHandler.publishEvent(new SensorDataEvent(1, NexradOutput.this, dataBlock));
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
        return nexradStruct;
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
