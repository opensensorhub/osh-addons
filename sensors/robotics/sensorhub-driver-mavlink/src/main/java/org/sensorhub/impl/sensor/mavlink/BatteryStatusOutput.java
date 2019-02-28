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
import org.sensorhub.api.sensor.SensorException;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_battery_status;


/**
 * <p>
 * Output for MAVLink BATTERY_STATUS messages
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jul 4, 2016
 */
public class BatteryStatusOutput extends MavlinkOutput
{
    
    
    public BatteryStatusOutput(MavlinkDriver parentSensor)
    {
        super(parentSensor);
        this.samplingPeriod = 0.1; // default to 10Hz on startup        
    }
    
    
    @Override
    public String getName()
    {
        return "batteryStatus";
    }
    
    
    protected void init() throws SensorException
    {
        GeoPosHelper fac = new GeoPosHelper();
        
        // create output structure
        dataStruct = fac.newDataRecord(2);
        dataStruct.setName(getName());
        
        // UTC time stamp
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());        
        dataStruct.addComponent("level", fac.newQuantity(SWEHelper.getPropertyUri("BatteryLevel"), "Battery Level", null, "%"));
        
        // text encoding
        dataEncoding = new TextEncodingImpl(",", "\n");
    }
    
    
    protected void handleMessage(long msgTime, MAVLinkMessage m)
    {
        DataBlock dataBlock = null;
                
        // process different message types
        if (m instanceof msg_battery_status)
        {
            msg_battery_status msg = (msg_battery_status)m;

            // populate datablock
            dataBlock = getNewDataBlock();
            dataBlock.setDoubleValue(0, msgTime/1000.);
            dataBlock.setFloatValue(1, msg.battery_remaining);
            
            updateSamplingPeriod(msgTime);//msg.time_boot_ms);
        }        
        
        if (dataBlock != null)
            sendOutput(msgTime, dataBlock);
    }
}
