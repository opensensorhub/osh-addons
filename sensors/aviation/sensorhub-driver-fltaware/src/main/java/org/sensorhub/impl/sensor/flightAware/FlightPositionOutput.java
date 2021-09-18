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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Vector;


public class FlightPositionOutput extends AbstractSensorOutput<FlightAwareDriver>
{
    static final String DEF_FLIGHTPOS_REC = SWEHelper.getPropertyUri("aero/FlightPosition");
    static final String DEF_VERTICAL_RATE = SWEHelper.getPropertyUri("areo/VerticalRate");
    static final String DEF_GROUND_SPEED = SWEHelper.getPropertyUri("GroundSpeed");
    static final String DEF_HEADING = SWEHelper.getPropertyUri("TrueHeading");
    private static final int AVERAGE_SAMPLING_PERIOD = 30;
    private static final String INVALID_ALT_MSG = ": Invalid altitude detected.";

	DataRecord recordStruct;
	DataEncoding encoding;	

	Map<String, Long> latestUpdateTimes = new ConcurrentHashMap<>();
	Map<String, DataBlock> latestRecords = new ConcurrentHashMap<>();  // key is position uid

	
	public FlightPositionOutput(FlightAwareDriver parentSensor) 
	{
		super("flightPos", parentSensor);
	}

	protected void init()
	{
		SWEHelper fac = new SWEHelper();
		GeoPosHelper geoHelper = new GeoPosHelper();

		// SWE Common data structure
		recordStruct = fac.newDataRecord(7);
		recordStruct.setName(getName());
		recordStruct.setDefinition(DEF_FLIGHTPOS_REC);

		recordStruct.addComponent("time", fac.newTimeStampIsoGPS());

		// oshFlightId
		recordStruct.addField("flightId", fac.newText(FlightPlanOutput.DEF_FLIGH_ID, "Flight ID", null));

		//  location
		Vector locVector = geoHelper.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
		locVector.setLabel("Location");
		locVector.setDescription("Location measured by GPS device");
		recordStruct.addComponent("location", locVector);

		//  heading
		recordStruct.addField("heading", fac.newQuantity(DEF_HEADING, "True Heading", null, "deg"));

		// airspeed
		recordStruct.addField("groundSpeed", fac.newQuantity(DEF_GROUND_SPEED, "Ground Speed", null, "[kn_i]"));
		
		// vertical rate
        recordStruct.addField("verticalRate", fac.newQuantity(DEF_VERTICAL_RATE, "Vertical Rate", null, "[ft_i]/min"));

		// default encoding is text
		encoding = fac.newTextEncoding(",", "\n");
	}

	public void sendPosition(String oshFlightId, FlightObject fltPos)
	{                
		int i = 0;
		
		// build data block from FlightObject Record
		DataBlock dataBlk = recordStruct.createDataBlock();
		dataBlk.setDoubleValue(i++, fltPos.getClock());
		dataBlk.setStringValue(i++, oshFlightId);
		dataBlk.setDoubleValue(i++, fltPos.getValue(fltPos.lat));
		dataBlk.setDoubleValue(i++, fltPos.getValue(fltPos.lon));		
		
		// fix altitude if 0
        double alt = fltPos.getValue(fltPos.alt);
        if (alt <= 0)
        {
            DataBlock lastData = latestRecords.get(oshFlightId);
            if (lastData != null)
            {
                double lastAlt = lastData.getDoubleValue(i);
                parentSensor.getLogger().debug("{}{} Using last value = {}", oshFlightId, INVALID_ALT_MSG, lastAlt);
                alt = lastAlt;
            }
            else
                parentSensor.getLogger().debug("{}{} No previous value available", oshFlightId, INVALID_ALT_MSG);
        }
		dataBlk.setDoubleValue(i++, alt);
		
		dataBlk.setDoubleValue(i++, fltPos.getValue(fltPos.heading));
		dataBlk.setDoubleValue(i++, fltPos.getValue(fltPos.gs));
        dataBlk.setDoubleValue(i++, fltPos.verticalChange);
        
		// update latest record and send event
		latestRecord = dataBlk;
		latestRecords.put(oshFlightId, dataBlk);
		latestRecordTime = System.currentTimeMillis();
        latestUpdateTimes.put(oshFlightId, fltPos.getClock());
		eventHandler.publish(new DataEvent(
		    latestRecordTime, this,
		    FlightAwareDriver.FLIGHT_UID_PREFIX + oshFlightId,
		    dataBlk));
	}

	public double getAverageSamplingPeriod()
	{
		return AVERAGE_SAMPLING_PERIOD;
	}


	@Override 
	public DataComponent getRecordDescription()
	{
		return recordStruct;
	}


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return encoding;
	}
}
