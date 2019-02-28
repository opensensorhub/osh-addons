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
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.sensor.SensorException;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_attitude;


/**
 * <p>
 * Output for MAVLink ATTITUDE_QUATERNION messages
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 12, 2015
 */
public class AttitudeEulerOutput extends MavlinkOutput
{
    
    public AttitudeEulerOutput(MavlinkDriver parentSensor)
    {
        super(parentSensor);
        this.samplingPeriod = 0.1; // default to 10Hz on startup        
    }
    
    
    @Override
    public String getName()
    {
        return "platformAtt";
    }
    
    
    protected void init() throws SensorException
    {
        GeoPosHelper fac = new GeoPosHelper();
        
        // create output structure
        dataStruct = fac.newDataRecord(2);
        dataStruct.setName(getName());
        
        // UTC time stamp
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        
        // attitude quaternion
        Vector att = fac.newEulerOrientationNED(SWEConstants.DEF_PLATFORM_ORIENT);
        att.setLocalFrame("#" + MavlinkDriver.BODY_FRAME);
        att.setDataType(DataType.FLOAT);
        dataStruct.addComponent("attitude", att);
        
        // text encoding
        dataEncoding = new TextEncodingImpl(",", "\n");
    }
    
    
    protected void handleMessage(long msgTime, MAVLinkMessage m)
    {
        DataBlock dataBlock = null;
                
        // process different message types
        if (m instanceof msg_attitude)
        {
            msg_attitude msg = (msg_attitude)m;
            
            // populate datablock
            dataBlock = getNewDataBlock();
            dataBlock.setDoubleValue(0, parentSensor.getUtcTimeFromBootMillis(msg.time_boot_ms));
            dataBlock.setFloatValue(1, (float)Math.toDegrees(msg.yaw));
            dataBlock.setFloatValue(2, (float)Math.toDegrees(msg.pitch));
            dataBlock.setFloatValue(3, (float)Math.toDegrees(msg.roll));
            
            updateSamplingPeriod(msgTime);//msg.time_boot_ms);
        }        
        
        if (dataBlock != null)
            sendOutput(msgTime, dataBlock);
    }
}
