/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.time.Instant;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.utils.aero.impl.AeroUtils;
import org.vast.data.DataBlockProxy;
import org.vast.swe.SWEHelper;
import com.google.common.base.Strings;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;


public class FlightPositionOutput extends AbstractSensorOutput<FlightAwareDriver> implements PositionListener
{
    static final String DEF_FLIGHTPOS_REC = SWEHelper.getPropertyUri("aero/FlightPosition");
    static final String DEF_VERTICAL_RATE = SWEHelper.getPropertyUri("areo/VerticalRate");
    static final String DEF_GROUND_SPEED = SWEHelper.getPropertyUri("GroundSpeed");
    static final String DEF_HEADING = SWEHelper.getPropertyUri("TrueHeading");
    private static final int AVERAGE_SAMPLING_PERIOD = 30;

    DataRecord dataStruct;
    DataEncoding encoding;
    FaPositionMsg posMsg;

	
	public FlightPositionOutput(FlightAwareDriver parentSensor) 
	{
		super("flightPos", parentSensor);
	}


    protected void init()
    {
        // data structure
        this.dataStruct = FaPositionMsg.getSchema(getName());
        this.posMsg = DataBlockProxy.generate(dataStruct, FaPositionMsg.class);

        // default encoding is text
        encoding = new SWEHelper().newTextEncoding(",", "\n");
    }
    

    @Override
    public synchronized void newPosition(FlightObject fltObj)
	{                
	    long msgTime = System.currentTimeMillis();
        
        // fix altitude if 0
        double alt = toDouble(fltObj.alt);
        if (alt <= 0)
            alt = Double.NaN;
	    
        // renew datablock
        var dataBlk = latestRecord == null ?
            dataStruct.createDataBlock() : latestRecord.renew();
        posMsg.wrap(dataBlk);
        
        // set datablock values
        posMsg.setTime(toInstant(fltObj.clock));
        posMsg.setTailNumber(fltObj.reg);
        posMsg.setCallSign(fltObj.ident);
        posMsg.setLatitude(toDouble(fltObj.lat));
        posMsg.setLongitude(toDouble(fltObj.lon));
        posMsg.setUpdateType(fltObj.updateType);
        posMsg.setGroundSpeed(toDouble(fltObj.gs));
        posMsg.setTrueTrack(toDouble(fltObj.heading));
        posMsg.setGnssAltitude(toDouble(fltObj.alt_gnss));
        posMsg.setBaroAltitude(toDouble(fltObj.alt));
        posMsg.setVerticalRate(toDouble(fltObj.vertRate));
        posMsg.setTrueHeading(toDouble(fltObj.heading_true));
        posMsg.setIndicatedAirspeed(toDouble(fltObj.speed_ias));
        posMsg.setTrueAirSpeed(toDouble(fltObj.speed_tas));
        posMsg.setMach(toDouble(fltObj.mach));
        
        // create FOI if needed, use tail number as ID
        String foiUid = AeroUtils.ensureTailFoi(getParentProducer(), posMsg.getTailNumber());
        
		// update latest record and send event
		latestRecord = dataBlk;
		latestRecordTime = msgTime;
		eventHandler.publish(new DataEvent(latestRecordTime, this, foiUid, dataBlk));
	}
    
    
    protected Instant toInstant(String val)
    {
        if (Strings.isNullOrEmpty(val))
            return null;
        
        long epochSeconds = Long.parseLong(val);
        return Instant.ofEpochSecond(epochSeconds);
    }
    
    
    protected double toDouble(String val)
    {
        if (Strings.isNullOrEmpty(val))
            return Double.NaN;
        
        return Double.parseDouble(val);
    }
	

    public double getAverageSamplingPeriod()
    {
        return AVERAGE_SAMPLING_PERIOD;
    }


    @Override 
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return encoding;
	}
}
