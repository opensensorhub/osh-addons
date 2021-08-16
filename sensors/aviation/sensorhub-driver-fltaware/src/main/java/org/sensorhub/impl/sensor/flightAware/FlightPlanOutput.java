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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.flightAware.DecodeFlightRouteResponse.Waypoint;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;


/**
 * <p>
 * Data producer output for flight plan data 
 * </p>
 *
 * @author Tony Cook
 * @since Sep 5, 2017
 */
public class FlightPlanOutput extends AbstractSensorOutput<FlightAwareDriver> implements IMultiSourceDataInterface  
{
    static final String DEF_FLIGHTPLAN_REC = SWEHelper.getPropertyUri("aero/FlightPlan");
    static final String DEF_AIRCRAFT_TYPE = SWEHelper.getPropertyUri("aero/AircraftType/ICAO");
    static final String DEF_AIRPORT_CODE = SWEHelper.getPropertyUri("aero/AirportCode/ICAO");
    static final String DEF_WAYPOINT = SWEHelper.getPropertyUri("aero/Waypoint");
    static final String DEF_WAYPOINT_TYPE = SWEHelper.getPropertyUri("aero/WaypointType");
    static final String DEF_WAYPOINT_CODE = SWEHelper.getPropertyUri("aero/WaypointCode/ICAO");static final String DEF_FLIGHT_NUM = SWEHelper.getPropertyUri("aero/FlightNumber");
    static final String DEF_FLIGHT_LEVEL = SWEHelper.getPropertyUri("aero/FlightLevel");

    private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toMillis(15); 

	DataComponent dataStruct;
    DataArray waypointArray;
	DataEncoding encoding;	
	Map<String, Long> latestUpdateTimes = new ConcurrentHashMap<>();
	Map<String, DataBlock> latestRecords = new ConcurrentHashMap<>();


	public FlightPlanOutput(FlightAwareDriver parentSensor) 
	{
		super("flightPlan", parentSensor);
	}

	protected void init()
	{
		GeoPosHelper fac = new GeoPosHelper();

		// SWE Common data structure
		this.dataStruct = fac.newDataRecord();
		dataStruct.setName(getName());
		dataStruct.setDefinition(DEF_FLIGHTPLAN_REC);
        dataStruct.addComponent("time", fac.newTimeIsoUTC(SWEConstants.DEF_SAMPLING_TIME, "Issue Time", null));
        dataStruct.addComponent("flightId", fac.newText(ENTITY_ID_URI, "Flight ID", null));
        dataStruct.addComponent("flightNumber", fac.newText(DEF_FLIGHT_NUM, "Flight Number", null));
        dataStruct.addComponent("aircraftType", fac.newCategory(DEF_AIRCRAFT_TYPE, "Aircraft Type", "Model of aircraft operated on this flight", null));
        dataStruct.addComponent("srcAirport", fac.newText(DEF_AIRPORT_CODE, "Departure Airport", "ICAO identification code of departure airport"));
        dataStruct.addComponent("destAirport", fac.newText(DEF_AIRPORT_CODE, "Arrival Airport", "ICAO identification code of arrival airport"));
        dataStruct.addComponent("altAirports", fac.newText(DEF_AIRPORT_CODE, "Alternate Airports", "ICAO identification codes of alternate airports"));
        dataStruct.addComponent("departTime", fac.newTimeIsoUTC(SWEConstants.DEF_FORECAST_TIME, "Departure Time", "Scheduled departure time"));

        // array of waypoints
        Count numPoints = fac.newCount(SWEConstants.DEF_NUM_POINTS, "Number of Waypoints", null);
        numPoints.setId("NUM_POINTS");
        dataStruct.addComponent("numPoints", numPoints);

        DataComponent waypt = fac.newDataRecord();
        waypt.setDefinition(DEF_WAYPOINT);
        waypt.addComponent("code", fac.newText(DEF_WAYPOINT_CODE, "Waypoint Code", "Waypoint ICAO identification code"));
        waypt.addComponent("type", fac.newText(DEF_WAYPOINT_TYPE, "Waypoint Type", "Type of navigation point (airport, waypoint, VOR, VORTAC, DME, etc.)"));
        waypt.addComponent("time", fac.newTimeIsoUTC(SWEConstants.DEF_FORECAST_TIME, "Estimated Time", "Estimated time over waypoint"));
        waypt.addComponent("lat", fac.newQuantity(SWEHelper.getPropertyUri("GeodeticLatitude"), "Latitude", null, "deg"));
        waypt.addComponent("lon", fac.newQuantity(SWEHelper.getPropertyUri("Longitude"), "Longitude", null, "deg"));
        waypt.addComponent("alt", fac.newQuantity(DEF_FLIGHT_LEVEL, "Flight Level", null, "[ft_i]", DataType.DOUBLE));
        
        waypointArray = fac.newDataArray();
        waypointArray.setElementType("waypoint", waypt);
        waypointArray.setElementCount(numPoints);
        dataStruct.addComponent("waypoints", waypointArray);

		// default encoding is text
		encoding = fac.newTextEncoding(",", "\n");
	}

	public synchronized void sendFlightPlan(String oshFlightId, FlightObject fltPlan)
	{
        long msgTime = System.currentTimeMillis();
        
        // renew datablock
        int numWpts = fltPlan.decodedRoute.size();
        waypointArray.updateSize(numWpts);
        DataBlock dataBlk = dataStruct.createDataBlock();
        
        // set datablock values
        int i = 0;        
        dataBlk.setDoubleValue(i++, fltPlan.getMessageTime());
        dataBlk.setStringValue(i++, oshFlightId);
        dataBlk.setStringValue(i++, fltPlan.ident);
        dataBlk.setStringValue(i++, fltPlan.aircrafttype);
        dataBlk.setStringValue(i++, fltPlan.orig);
        dataBlk.setStringValue(i++, fltPlan.dest);
        dataBlk.setStringValue(i++, null);
        dataBlk.setDoubleValue(i++, fltPlan.getDepartureTime());
        dataBlk.setIntValue(i++, numWpts);
        AbstractDataBlock waypointData = ((DataBlockMixed)dataBlk).getUnderlyingObject()[i];
        i = 0;
        for (Waypoint waypt: fltPlan.decodedRoute)
        {
            waypointData.setStringValue(i++, waypt.name); 
            waypointData.setStringValue(i++, waypt.type);
            waypointData.setDoubleValue(i++, Double.NaN);
            waypointData.setDoubleValue(i++, waypt.latitude);
            waypointData.setDoubleValue(i++, waypt.longitude);
            waypointData.setDoubleValue(i++, waypt.altitude);
        }
        
        // skip if same as last record for a given foi
        if (isDuplicate(oshFlightId, dataBlk))
            return;
        
        // update latest record and send event
        latestRecord = dataBlk;
        latestRecordTime = msgTime;
        latestRecords.put(oshFlightId, dataBlk);
        eventHandler.publish(new DataEvent(latestRecordTime, oshFlightId, this, dataBlk));
	}
	
	
	protected boolean isDuplicate(String flightId, DataBlock newRec)
	{
	    DataBlock oldRec = latestRecords.get(flightId);
	    
	    // we're sure it's not duplicate if we never received anything
	    // or if the data blocks have different sizes
	    if (oldRec == null || oldRec.getAtomCount() != newRec.getAtomCount())
	        return false;
	    
	    // compare all fields except the first (issue time)
	    // because it's always set to current time
	    for (int i=1; i<newRec.getAtomCount(); i++)
	    {
	        String oldVal = oldRec.getStringValue(i);
	        String newVal = newRec.getStringValue(i);	        
	        if (oldVal != null && !oldVal.equals(newVal))
	            return false;	        
	    }
	    
	    parentSensor.getLogger().debug("Duplicate flight plan received for flight {}", flightId);
	    return true;
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


	@Override
	public Collection<String> getEntityIDs()
	{
		return parentSensor.getEntityIDs();
	}


	@Override
	public Map<String, DataBlock> getLatestRecords()
	{
		return Collections.unmodifiableMap(latestRecords);
	}


	@Override
	public DataBlock getLatestRecord(String entityId) {
		//		for(Map.Entry<String, DataBlock> dbe: latestRecords.entrySet()) {
		//			String key = dbe.getKey();
		//			DataBlock val = dbe.getValue();
		//			System.err.println(key + " : " + val);
		//		}
		int lastColonIdx = entityId.lastIndexOf(':');
		if(lastColonIdx == -1) {
			return null;
		}
		String flightId = entityId.substring(lastColonIdx + 1);
		return latestRecords.get(flightId);
	}


}