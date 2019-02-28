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
import net.opengis.swe.v20.Quantity;
import org.slf4j.Logger;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Output providing GNSS receiver speed and heading in NED reference frame.<br/>
 * UTC time of each output sample is obtained from previous position fix.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 27, 2015
 */
public class NEDVelocityOutput extends NMEAGpsOutput
{
    private static final double KMH_TO_MS = 1000./3600.;
    
    Logger log;
    
    
    public NEDVelocityOutput(NMEAGpsSensor parentSensor)
    {
        super(parentSensor);
        this.samplingPeriod = 1.0; // default to 1Hz on startup
        this.log = parentSensor.getLogger();
    }
    
    
    @Override
    public String getName()
    {
        return "gpsTrack";
    }

    
    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        
        // SWE Common data structure
        dataStruct = fac.newDataRecord(3);
        dataStruct.setDefinition(SWEHelper.getPropertyUri("GNSS/TrackData"));
        dataStruct.setName(getName());
        
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        
        Quantity heading = fac.newQuantity(SWEHelper.getPropertyUri("TrackHeading"), "Track Heading", "Track heading relative to true north", "deg");
        heading.setReferenceFrame(SWEConstants.REF_FRAME_NED);
        heading.setAxisID("z");
        dataStruct.addComponent("heading", heading);
        
        dataStruct.addComponent("speed", fac.newQuantity(SWEHelper.getPropertyUri("GroundSpeed"), "Ground Speed", null, "m/s"));
        
        dataEncoding = fac.newTextEncoding(",", "\n");
    }
    
    
    protected void handleMessage(long msgTime, String msgID, String msg)
    {
        DataBlock dataBlock = null;
                
        // process different message types
        if (msgID.equals(NMEAGpsSensor.VTG_MSG))
        {
            String[] tokens = msg.split(NMEA_SEP_REGEX);
            
            // skip if position fix not available
            if (Double.isNaN(parentSensor.lastFixUtcTime))
            {
                log.debug("VTG: No position fix");
                return;
            }
            
            // populate datablock
            dataBlock = getNewDataBlock();
            dataBlock.setDoubleValue(0, parentSensor.lastFixUtcTime);
            dataBlock.setDoubleValue(1, Double.parseDouble(tokens[1])); // heading
            dataBlock.setDoubleValue(2, toMetersPerSecond(tokens[7])); // speed    
        }
        
        else if (msgID.equals(NMEAGpsSensor.HDT_MSG))
        {
            String[] tokens = msg.split(NMEA_SEP_REGEX);
            
            // skip if position fix not available
            if (Double.isNaN(parentSensor.lastFixUtcTime))
            {
                log.debug("HDT: No position fix");
                return;
            }
            
            // populate datablock
            dataBlock = getNewDataBlock();
            dataBlock.setDoubleValue(0, parentSensor.lastFixUtcTime);
            dataBlock.setDoubleValue(1, Double.parseDouble(tokens[1])); // heading
            dataBlock.setDoubleValue(2, Double.NaN); // speed 
        }
        
        if (dataBlock != null)
            sendOutput(msgTime, dataBlock);
    }
       
    
    protected double toMetersPerSecond(String speedKmPerHours)
    {
        return Double.parseDouble(speedKmPerHours) * KMH_TO_MS;
    }
}
