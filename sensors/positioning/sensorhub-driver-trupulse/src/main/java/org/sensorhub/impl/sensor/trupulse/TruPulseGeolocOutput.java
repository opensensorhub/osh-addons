/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.trupulse;

import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.event.IEventSourceInfo;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.event.BasicEventHandler;
import org.sensorhub.impl.event.EventSourceInfo;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


/**
 * <p>
 * Wrapper class to expose target geoloc output on sensor object
 * </p>
 *
 * @author Alex Robin
 * @since Apr 10, 2019
 */
public class TruPulseGeolocOutput implements IStreamingDataInterface, IEventListener
{
    TruPulseWithGeolocSensor parentSensor;
    IStreamingDataInterface processOutput;
    BasicEventHandler eventHandler = new BasicEventHandler();
    IEventSourceInfo eventSrcInfo;
    
    
    TruPulseGeolocOutput(TruPulseWithGeolocSensor parentSensor, IStreamingDataInterface processOutput)
    {
        this.parentSensor = parentSensor;
        this.processOutput = processOutput;
        processOutput.registerListener(this);
        
        var procUID = parentSensor.getUniqueIdentifier();
        String sourceID = EventUtils.getDataStreamTopicID(procUID, getName());
        this.eventSrcInfo = new EventSourceInfo(procUID, sourceID);
    }
    
    
    @Override
    public boolean isEnabled()
    {
        return processOutput.isEnabled();
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return processOutput.getRecordDescription();
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return processOutput.getRecommendedEncoding();
    }


    @Override
    public long getLatestRecordTime()
    {
        return processOutput.getLatestRecordTime();
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return 1200.0; // 20min
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


    @Override
    public ISensorModule<?> getParentProducer()
    {
        return parentSensor;
    }


    @Override
    public String getName()
    {
        return processOutput.getName();
    }


    @Override
    public DataBlock getLatestRecord()
    {
        return processOutput.getLatestRecord();
    }


    @Override
    public void handleEvent(Event e)
    {
        if (e instanceof DataEvent)
            eventHandler.publish(new DataEvent(e.getTimeStamp(), this, ((DataEvent)e).getRecords()));
        
    }


    @Override
    public IEventSourceInfo getEventSourceInfo()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
