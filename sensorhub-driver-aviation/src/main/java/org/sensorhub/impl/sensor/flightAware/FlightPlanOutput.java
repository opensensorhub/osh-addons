/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockFloat;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockParallel;
import org.vast.data.DataBlockString;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;


/**
 * 
 * @author Tony Cook
 * 
 * 
 * TODO- add zulu time output somewhere
 *
 */
public class FlightPlanOutput extends AbstractSensorOutput<FlightAwareSensor> implements IMultiSourceDataInterface  
{
	private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toMillis(15); 

	DataRecord recordStruct;
	DataEncoding encoding;	
	Map<String, Long> latestUpdateTimes;
	Map<String, DataBlock> latestRecords = new LinkedHashMap<String, DataBlock>();


	public FlightPlanOutput(FlightAwareSensor parentSensor) 
	{
		super(parentSensor);
		latestUpdateTimes = new HashMap<String, Long>();
	}


	@Override
	public String getName()
	{
		return "FlightPlan data";
	}

	protected void init()
	{
		SWEHelper fac = new SWEHelper();
		GeoPosHelper geoHelper = new GeoPosHelper();

		//  Add top level structure for flight plan
		//	 time, flightId, numWaypoints, String[] icaoCode, String [] type, lat[], lon[]
		//	 time, flightId, numWaypoints, Waypt [] 

		// SWE Common data structure
		recordStruct = fac.newDataRecord(4);
		recordStruct.setName(getName());
		recordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/flightPlan"); // ??

		recordStruct.addComponent("time", fac.newTimeStampIsoGPS());

		// flightIds
		recordStruct.addComponent("flightId", fac.newText("http://earthcastwx.com/ont/swe/property/flightId", "flightId", "Internally generated flight desc (flightNum_DestAirport"));

		//  num of points
		Count numPoints = fac.newCount(DataType.INT);
		numPoints.setDefinition("http://sensorml.com/ont/swe/property/NumberOfSamples"); 
		numPoints.setId("NUM_POINTS");
		recordStruct.addComponent("numPoints",numPoints);


		DataComponent waypt = fac.newDataRecord();
		Text code = fac.newText("http://sensorml.com/ont/swe/property/code", "ICAO Code", "Typically, ICAO airline code plus IATA/ticketing flight number");
		waypt.addComponent("code", code);
		Text type = fac.newText("http://sensorml.com/ont/swe/property/type", "Type", "Type (Waypoint/Navaid/etc.)" );
		waypt.addComponent("type", type);
		Quantity latQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Latitude", "Geodetic Latitude", null, "deg", DataType.FLOAT);
		waypt.addComponent("lat", latQuant);
		Quantity lonQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Longitude", "Longitude", null, "deg", DataType.FLOAT);
		waypt.addComponent("lon", lonQuant);

		DataArray ptArr = fac.newDataArray();
		ptArr.setElementType("point", waypt);
		ptArr.setElementCount(numPoints);
		recordStruct.addComponent("points", ptArr);

		// default encoding is text
		encoding = fac.newTextEncoding(",", "\n");
	}

	public void start() throws SensorHubException {
		// Nothing to do 
	}

	public void sendFlightPlan(FlightPlan plan)
	{
		int numPts = plan.waypoints.size();

		DataArray ptArr = (DataArray)recordStruct.getComponent(3);
		ptArr.updateSize(numPts);
		DataBlock dataBlock = recordStruct.createDataBlock();

		dataBlock.setDoubleValue(0, plan.time);
		dataBlock.setStringValue(1, plan.oshFlightId);
		dataBlock.setIntValue(2, numPts);
		DataBlock arr = ((DataBlockMixed)dataBlock).getUnderlyingObject()[3];
		DataBlockParallel parr = (DataBlockParallel)arr;
		DataBlockString names = (DataBlockString)parr.getUnderlyingObject()[0];
		DataBlockString types = (DataBlockString)parr.getUnderlyingObject()[1];
		DataBlockFloat lats = (DataBlockFloat)parr.getUnderlyingObject()[2];
		DataBlockFloat  lons = (DataBlockFloat)parr.getUnderlyingObject()[3];
		names.setUnderlyingObject(plan.getNames());
		types.setUnderlyingObject(plan.getTypes());
		lats.setUnderlyingObject(plan.getLats());
		lons.setUnderlyingObject(plan.getLons());
		
//		((DataBlockMixed)dataBlock).getUnderlyingObject()[3].setUnderlyingObject();

		// update latest record and send event
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		String flightUid = FlightAwareSensor.FLIGHT_PLAN_UID_PREFIX + plan.oshFlightId;
		latestUpdateTimes.put(flightUid, plan.time);
		latestRecords.put(flightUid, latestRecord);   
//		DataBlock b = latestRecords.get("DAL2152_KSLC");
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, FlightPlanOutput.this, dataBlock));
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
	public DataBlock getLatestRecord(String entityID) {
		//		for(Map.Entry<String, DataBlock> dbe: latestRecords.entrySet()) {
		//			String key = dbe.getKey();
		//			DataBlock val = dbe.getValue();
		//			System.err.println(key + " : " + val);
		//		}
		DataBlock b = latestRecords.get(entityID);
		return b;
	}


}