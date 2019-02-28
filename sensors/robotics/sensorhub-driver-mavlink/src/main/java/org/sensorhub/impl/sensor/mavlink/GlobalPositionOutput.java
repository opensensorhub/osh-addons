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
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.sensor.SensorException;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_global_position_int;


/**
 * <p>
 * Output for MAVLink GLOBAL_POSITION_INT messages
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 12, 2015
 */
public class GlobalPositionOutput extends MavlinkOutput
{        
    
    public GlobalPositionOutput(MavlinkDriver parentSensor)
    {
        super(parentSensor);
        this.samplingPeriod = 1.0; // default to 1Hz on startup        
    }
    
    
    @Override
    public String getName()
    {
        return "platformLoc";
    }
    
    
    protected void init() throws SensorException
    {
        GeoPosHelper fac = new GeoPosHelper();
        
        // create output structure
        dataStruct = fac.newDataRecord(2);
        dataStruct.setName(getName());
        
        // UTC time stamp
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        
        // lat/lon/alt location 
        Vector att = fac.newLocationVectorLLA(SWEConstants.DEF_PLATFORM_LOC);
        att.setLocalFrame("#" + MavlinkDriver.BODY_FRAME);
        att.getCoordinate("alt").setReferenceFrame(SWEConstants.VERT_DATUM_EGM96_MSL);
        dataStruct.addComponent("loc", att);
        
        // text encoding
        dataEncoding = new TextEncodingImpl(",", "\n");
    }
    
    
    protected void handleMessage(long msgTime, MAVLinkMessage m)
    {
        DataBlock dataBlock = null;
                
        // process different message types
        if (m instanceof msg_global_position_int)
        {
            msg_global_position_int msg = (msg_global_position_int)m;
            
            // populate datablock
            dataBlock = getNewDataBlock();
            dataBlock.setDoubleValue(0, parentSensor.getUtcTimeFromBootMillis(msg.time_boot_ms));
            dataBlock.setDoubleValue(1, ((double)msg.lat) / 1e7);
            dataBlock.setDoubleValue(2, ((double)msg.lon) / 1e7);
            dataBlock.setDoubleValue(3, ((double)msg.alt) / 1e3);
            
            updateSamplingPeriod(msgTime);//msg.time_boot_ms);
        }        
        
        if (dataBlock != null)
            sendOutput(msgTime, dataBlock);
    }
}
