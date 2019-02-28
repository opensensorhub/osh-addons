/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.nmea.gps;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;


/**
 * <p>
 * Abstract base for all outputs corresponding to NMEA sentences
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 27, 2015
 */
public abstract class NMEAGpsOutput extends AbstractSensorOutput<NMEAGpsSensor>
{
    protected static final String NMEA_SEP_REGEX = ",|\\*";
    
    protected DataComponent dataStruct;
    protected DataEncoding dataEncoding;
    protected double samplingPeriod;
    protected long lastMsgTime = Long.MIN_VALUE;
    

    public NMEAGpsOutput(NMEAGpsSensor parentSensor)
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
     * Method to be implemented by concrete outputs to handle appropriate NMEA messages
     * @param msgTime system time at which message was received
     * @param msgID NMEA message ID (not including the $XX talker ID)
     * @param msg Complete NMEA message string including the full message ID
     */
    protected abstract void handleMessage(long msgTime, String msgID, String msg);
    

}
