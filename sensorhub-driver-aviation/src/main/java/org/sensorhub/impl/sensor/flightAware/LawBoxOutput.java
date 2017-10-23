package org.sensorhub.impl.sensor.flightAware;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEConstants;
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
import net.opengis.swe.v20.Vector;

public class LawBoxOutput extends AbstractSensorOutput<FlightAwareSensor> implements IMultiSourceDataInterface  
{
	private static final int AVERAGE_SAMPLING_PERIOD = 1; //(int)TimeUnit.SECONDS.toSeconds(5);

	DataRecord recordStruct;
	DataEncoding encoding;	

	Map<String, Long> latestUpdateTimes = new LinkedHashMap<>();
	Map<String, DataBlock> latestRecords = new LinkedHashMap<>();  // key is position uid

	public LawBoxOutput(FlightAwareSensor parentSensor) 
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "LawBox data";
	}

	protected void init()
	{
		SWEHelper fac = new SWEHelper();
		GeoPosHelper geoHelper = new GeoPosHelper();

		//  Add top level structure for flight plan
		//	 time, flightId, <planeLoc>, lawBox corners (array of lla), turbHazard loacations array of [lla], maxHazard, incr/decr

		// SWE Common data structure
		recordStruct = fac.newDataRecord(7);
		recordStruct.setName(getName());
		recordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/LawBox"); // ??

		recordStruct.addComponent("time", fac.newTimeStampIsoGPS());

		// oshFlightId
		recordStruct.addField("flightId", fac.newText("", "flightId", "Internally generated flight desc (flightNum_DestAirport"));

		//  LaxBox corners
		Vector boxCorners = geoHelper.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
		boxCorners.setLabel("LawBoxCorners");
		boxCorners.setDescription("Corners computed based on plane location and criteria set by Delta (encoded in LaxBoxGeometry class)");
		Count numCorners = fac.newCount(DataType.INT);
		numCorners.setValue(8);
		recordStruct.addComponent("NumberOfCornerPoints", numCorners);
		DataArray cornerArr = fac.newDataArray();
		cornerArr.setElementType("BoxCorner", boxCorners);
		cornerArr.setElementCount(numCorners);
		recordStruct.addComponent("LawBoxCOrners", cornerArr);
		

		// Array of Turbulence Locations where turb exceeds thersh
		Vector turbVector = geoHelper.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
		turbVector.setLabel("Turbulence Hazard Location");
		turbVector.setDescription("Location where turbulence exceeds threshold");

		Count numTurbValues = fac.newCount(DataType.INT);
		numTurbValues.setId("NUM_TURB_VALUES");
		recordStruct.addComponent("numPoints",numTurbValues);
		
		DataArray turbArr = fac.newDataArray();
		turbArr.setElementType("LocationVector", turbVector);
		turbArr.setElementCount(numTurbValues);
		recordStruct.addComponent("TurbulenceHazardArray", turbArr);		
		
		// Turbulence MAX location and value
		Vector turbMaxVector = geoHelper.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
		turbVector.setLabel("Turbulence Maximum Hazard Location");
		turbVector.setDescription("Location of Maximum turbulence");
		recordStruct.addField("turbulenceHazardValue", fac.newQuantity("http://earthcastwx.com/ont/swe/property/TurbulenceMaxHazardValue", "Turbulence", null, ""));

		// Turbulence increasing/decreasing
		//  +1 = increasing, -1 = decreasing, 0 = no change
		recordStruct.addField("turbulenceChangeFlag", fac.newQuantity("http://earthcastwx.com/ont/swe/property/TurbulenceChangeFlag", "AirSpeed", null, ""));
	}

	public void start() throws SensorHubException {
		// Nothing to do 
	}

	public void sendLawBox()
	{                
//		// build data block from FlightObject Record
//		DataBlock dataBlock = recordStruct.createDataBlock();
//		dataBlock.setDoubleValue(0, obj.getClock());
//		dataBlock.setStringValue(1, obj.getOshFlightId());
//
//		dataBlock.setDoubleValue(2, obj.getValue(obj.lat));
//		dataBlock.setDoubleValue(3, obj.getValue(obj.lon));
//		dataBlock.setDoubleValue(4, obj.getValue(obj.alt));
//		dataBlock.setDoubleValue(5, obj.getValue(obj.heading));
//		dataBlock.setDoubleValue(6, obj.getValue(obj.speed));
//
//		// update latest record and send event
//		latestRecord = dataBlock;
//		latestRecordTime = System.currentTimeMillis();
//		String flightUid = FlightAwareSensor.FLIGHT_POSITION_UID_PREFIX + oshFlightId;
//		latestUpdateTimes.put(flightUid, obj.getClock());
//		latestRecords.put(flightUid, latestRecord);   
//		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, LawBoxOutput.this, dataBlock));        	
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
		//  Can't really generate this one
		DataBlock b =  latestRecords.get(entityID);
		return b;
	}

}
