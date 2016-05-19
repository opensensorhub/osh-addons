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
import net.opengis.swe.v20.DataType;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Output providing GNSS receiver quality and DOP information.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 27, 2015
 */
public class GPSQualityOutput extends NMEAGpsOutput
{
        
    public GPSQualityOutput(NMEAGpsSensor parentSensor)
    {
        super(parentSensor);
        this.samplingPeriod = 1.0; // default to 1Hz on startup
    }
    
    
    @Override
    public String getName()
    {
        return "gpsQuality";
    }

    
    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        
        // SWE Common data structure
        dataStruct = fac.newDataRecord(3);
        dataStruct.setName(getName());
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        dataStruct.addComponent("numSats", fac.newCount(SWEHelper.getPropertyUri("GNSS/FixNumSats"), "Number of Satellites", "Number of satellites used in the position fix"));
        dataStruct.addComponent("hdop", fac.newQuantity(SWEHelper.getPropertyUri("GNSS/HDOP"), "HDOP", null, "1", DataType.FLOAT));
        dataStruct.addComponent("vdop", fac.newQuantity(SWEHelper.getPropertyUri("GNSS/VDOP"), "VDOP", null, "1", DataType.FLOAT));
        dataStruct.addComponent("herr", fac.newQuantity(SWEHelper.getPropertyUri("GNSS/HPrecision"), "Horizontal Precision", null, "m", DataType.FLOAT));
        dataStruct.addComponent("verr", fac.newQuantity(SWEHelper.getPropertyUri("GNSS/VPrecision"), "Vertical Precision", null, "m", DataType.FLOAT));
        
        dataEncoding = fac.newTextEncoding(",", "\n");
    }
    
    
    protected void handleMessage(long msgTime, String msgID, String msg)
    {
        DataBlock dataBlock = null;
                
        // process different message types
        if (msgID.equals(NMEAGpsSensor.GSA_MSG))
        {
            String[] tokens = msg.split(NMEA_SEP_REGEX);
                        
            // skip if position fix not available
            if (tokens[2].charAt(0) == '1' || Double.isNaN(parentSensor.lastFixUtcTime))
            {
                log.debug("GSA: No position fix");
                return;
            }
            
            // count number of satellites used
            int numSats = 0;
            for (int i = 3; i < 15; i++)
            {
                if (tokens[i].trim().length() > 0)
                    numSats++;
            }
            
            // populate datablock
            dataBlock = getNewDataBlock();
            dataBlock.setDoubleValue(0, parentSensor.lastFixUtcTime);
            dataBlock.setIntValue(1, numSats);
            dataBlock.setFloatValue(2, Float.parseFloat(tokens[16]));
            dataBlock.setFloatValue(3, Float.parseFloat(tokens[17]));
            dataBlock.setFloatValue(4, Float.parseFloat(tokens[16]) * 5.0f);
            dataBlock.setFloatValue(5, Float.parseFloat(tokens[17]) * 5.0f);         
        }        
        
        if (dataBlock != null)
            sendOutput(msgTime, dataBlock);
    }
    
}
