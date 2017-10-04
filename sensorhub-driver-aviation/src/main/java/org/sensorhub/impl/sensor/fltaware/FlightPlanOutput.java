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

package org.sensorhub.impl.sensor.fltaware;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockMixed;
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
public class FlightPlanOutput extends AbstractSensorOutput<FltawareSensor> implements IMultiSourceDataInterface  
{
	private static final int AVERAGE_SAMPLING_PERIOD = 1; //(int)TimeUnit.SECONDS.toSeconds(5);

	DataRecord recordStruct;
	DataEncoding encoding;	
	Map<String, Long> latestUpdateTimes;
	Map<String, DataBlock> latestRecords = new LinkedHashMap<String, DataBlock>();

	
	public FlightPlanOutput(FltawareSensor parentSensor) 
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

		// SWE Common data structure
		recordStruct = fac.newDataRecord(7);
		recordStruct.setName(getName());
		recordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/flightPlan"); // ??
		
        recordStruct.addComponent("time", fac.newTimeStampIsoGPS());

        // flightIds
		recordStruct.addField("flightId", fac.newText("", "flightId", "Internally generated flight desc (flightNum_DestAirport"));
        
		//  num of points
		Count numPoints = fac.newCount(DataType.INT);
		numPoints.setDefinition("http://sensorml.com/ont/swe/property/NumberOfSamples"); 
		numPoints.setId("NUM_POINTS");
		recordStruct.addComponent("numPoints",numPoints);
		
		//  icaoCodes
		Text code = fac.newText("http://sensorml.com/ont/swe/property/code", "icaoCode", "Typically, ICAO airline code plus IATA/ticketing flight number");
		DataArray codeArr = fac.newDataArray();
		codeArr.setElementType("icaoCode", code);
		codeArr.setElementCount(numPoints);
		recordStruct.addComponent("CodeArray", codeArr);

		//  icaoCodes
		Text type = fac.newText("http://sensorml.com/ont/swe/property/type", "icaoCode", "Type (Waypoint/Navaid/etc.)" );
		DataArray typeArr = fac.newDataArray();
		typeArr.setElementType("type", type);
		typeArr.setElementCount(numPoints);
		recordStruct.addComponent("TypeArray", typeArr);
		
		
		Quantity latQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Latitude", "Latitude", null, "deg", DataType.FLOAT);
		DataArray latArr = fac.newDataArray();
		latArr.setElementType("Latitude", latQuant);
		latArr.setElementCount(numPoints);
		recordStruct.addComponent("LatitudeArray", latArr);

		Quantity lonQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Longitude", "Longitude", null, "deg", DataType.FLOAT);
		DataArray lonArr = fac.newDataArray();
		lonArr.setElementType("Longitude", lonQuant);
		lonArr.setElementCount(numPoints);
		recordStruct.addComponent("LongitudeArray", lonArr);
		
        
		// default encoding is text
		encoding = fac.newTextEncoding(",", "\n");
	}

	public void start() throws SensorHubException {
		// Nothing to do 
	}
	
	public void sendFlightPlan(FlightPlan plan)
	{                
		//  sequential arrays
		float [] lats = plan.getLats();
		float [] lons = plan.getLons();
		String [] names = plan.getNames();
		String [] types = plan.getTypes();
		
		int numPts = lats.length;
		// update array sizes
		DataArray nameArr = (DataArray)recordStruct.getComponent(3);
		DataArray typeArr = (DataArray)recordStruct.getComponent(4);
		DataArray latArr = (DataArray)recordStruct.getComponent(5);
		DataArray lonArr = (DataArray)recordStruct.getComponent(6);
		nameArr.updateSize(numPts);
		typeArr.updateSize(numPts);
		latArr.updateSize(numPts);
		lonArr.updateSize(numPts);

		// build data block from Mesh Record
		DataBlock dataBlock = recordStruct.createDataBlock();
		dataBlock.setDoubleValue(0, plan.time);
		dataBlock.setStringValue(1, plan.flightId);
		
		dataBlock.setIntValue(2, numPts);
		
		((DataBlockMixed)dataBlock).getUnderlyingObject()[3].setUnderlyingObject(names);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[4].setUnderlyingObject(types);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[5].setUnderlyingObject(lats);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[6].setUnderlyingObject(lons);

		// update latest record and send event
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		String flightUid = FltawareSensor.AIRCRAFT_UID_PREFIX + plan.flightId;
		latestUpdateTimes.put(flightUid, plan.time);
		latestRecords.put(flightUid, latestRecord);   
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
