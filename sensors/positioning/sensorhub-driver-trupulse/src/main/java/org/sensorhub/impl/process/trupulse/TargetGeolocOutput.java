/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.trupulse;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.impl.SensorHub;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Target Location Output
 * </p>
 *
 * <p>Copyright (c) 2015 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Apr 5, 2015
 */
public class TargetGeolocOutput implements IStreamingDataInterface
{
    TargetGeolocProcess parentProcess;
    IEventHandler eventHandler;
    DataComponent outputDef;
    DataEncoding outputEncoding;
    long latestRecordTime = Long.MIN_VALUE;
    DataBlock latestRecord;
    double samplingPeriod = 10.0;
    

    public TargetGeolocOutput(TargetGeolocProcess parentProcess)
    {
        this.parentProcess = parentProcess;
        
        // create output structure
        GeoPosHelper fac = new GeoPosHelper();        
        DataRecord rec = fac.newDataRecord();
        rec.setName(getName());
        rec.addField("time", fac.newTimeStampIsoUTC());
        rec.addField("location", fac.newLocationVectorLLA(SWEHelper.getPropertyUri("TargetLocation")));
        this.outputDef = rec;        
        this.outputEncoding = fac.newTextEncoding();
        
        // obtain an event handler for this output
        String moduleID = parentProcess.getLocalID();
        String topic = getName();
        this.eventHandler = SensorHub.getInstance().getEventBus().registerProducer(moduleID, topic);
    }
    
    
    protected void sendLocation(double time, double lat, double lon, double alt)
    {
        // create and populate datablock
        DataBlock dataBlock;
        if (latestRecord == null)
        {
            dataBlock = outputDef.createDataBlock();
        }
        else
        {
            dataBlock = latestRecord.renew();
            samplingPeriod = time - latestRecord.getDoubleValue(0);
        }
        
        dataBlock.setDoubleValue(0, time);
        dataBlock.setDoubleValue(1, lat);
        dataBlock.setDoubleValue(2, lon);
        dataBlock.setDoubleValue(3, alt);
        TargetGeolocProcess.log.debug("Target pos = [{},{},{}]" , lat, lon, alt);
            
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new DataEvent(latestRecordTime, this, dataBlock));
    }


    @Override
    public TargetGeolocProcess getParentModule()
    {
        return parentProcess;
    }


    @Override
    public String getName()
    {
        return "targetLocation";
    }


    @Override
    public boolean isEnabled()
    {
        return true;
    }
    
    
    @Override
    public DataComponent getRecordDescription()
    {
        return outputDef;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return outputEncoding;
    }


    @Override
    public DataBlock getLatestRecord()
    {
        return latestRecord;
    }


    @Override
    public long getLatestRecordTime()
    {
        return latestRecordTime;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return samplingPeriod;
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
