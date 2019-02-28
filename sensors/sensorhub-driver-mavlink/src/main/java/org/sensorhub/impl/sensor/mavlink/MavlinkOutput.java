/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mavlink;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import com.MAVLink.Messages.MAVLinkMessage;


/**
 * <p>
 * Abstract base for all outputs corresponding to MAVLink messages
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 10, 2015
 */
public abstract class MavlinkOutput extends AbstractSensorOutput<MavlinkDriver>
{
    protected DataComponent dataStruct;
    protected DataEncoding dataEncoding;
    protected double samplingPeriod;
    protected long lastMsgTime = Long.MIN_VALUE;
    

    public MavlinkOutput(MavlinkDriver parentSensor)
    {
        super(parentSensor);
    }
    
    
    @Override
    public double getAverageSamplingPeriod()
    {
        return samplingPeriod;
    }
    
    
    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }
    
    
    protected void sendOutput(long msgTime, DataBlock dataBlock)
    {
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = msgTime;
        eventHandler.publishEvent(new SensorDataEvent(msgTime, this, dataBlock));
    }
    
    
    protected void updateSamplingPeriod(long msgTime)
    {
        if (lastMsgTime != Long.MIN_VALUE)
            samplingPeriod = (msgTime - lastMsgTime) / 1000.;
        
        lastMsgTime = msgTime;
    }
    
    
    protected final DataBlock getNewDataBlock()
    {
        if (latestRecord == null)
            return dataStruct.createDataBlock();
        else
            return latestRecord.renew();
    }
    
    
    /**
     * Method to be implemented by concrete outputs to handle appropriate MAVLink messages
     * @param msgTime system time at which message was received
     * @param msg MAVLink message object
     */
    protected abstract void handleMessage(long msgTime, MAVLinkMessage msg);
    

}
