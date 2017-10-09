package org.sensorhub.impl.sensor.flightAware;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockMixed;
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
import ucar.ma2.InvalidRangeException;

public class FlightPositionOutput extends AbstractSensorOutput<FlightAwareSensor> implements IMultiSourceDataInterface  
{
	private static final int AVERAGE_SAMPLING_PERIOD = 1; //(int)TimeUnit.SECONDS.toSeconds(5);

	DataRecord recordStruct;
	DataEncoding encoding;	

	Map<String, Long> latestUpdateTimes;
	Map<String, DataBlock> latestRecords = new LinkedHashMap<>();  // key is position uid

	public FlightPositionOutput(FlightAwareSensor parentSensor) 
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "FlightPosition Data";
	}

	protected void init()
	{
		SWEHelper fac = new SWEHelper();
		GeoPosHelper geoHelper = new GeoPosHelper();

		//  Add top level structure for flight plan
		//	 time, flightId, faFlightId, locationVec, heading, airspeed?

		// SWE Common data structure
		recordStruct = fac.newDataRecord(8);
		recordStruct.setName(getName());
		recordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/flightPosition"); // ??

		recordStruct.addComponent("time", fac.newTimeStampIsoGPS());

		// flightIds
		recordStruct.addField("flightId", fac.newText("", "flightId", "Internally generated flight desc (flightNum_DestAirport"));
		recordStruct.addField("faFlightId", fac.newText("", "flightId", "FlightAware flightId- not sure we will need this"));

		//  location
		Vector locVector = geoHelper.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
		locVector.setLabel("Location");
		locVector.setDescription("Location measured by GPS device");
		recordStruct.addComponent("location", locVector);

		//  heading
		recordStruct.addField("heading", fac.newQuantity("http://sensorml.com/ont/swe/property/Heading", "Heading", null, "deg"));

		// airspeed
		recordStruct.addField("airspeed", fac.newQuantity("http://sensorml.com/ont/swe/property/AirSpeed", "AirSpeed", null, "kts"));

		// default encoding is text
		encoding = fac.newTextEncoding(",", "\n");
	}

	public void start() throws SensorHubException {
		// Nothing to do 
	}

	public void sendPosition(FlightObject obj, String oshFlightId)
	{                
		// build data block from FlightObject Record
		DataBlock dataBlock = recordStruct.createDataBlock();
		dataBlock.setDoubleValue(0, obj.getClock());
		dataBlock.setStringValue(1, obj.getOshFlightId());
		dataBlock.setStringValue(2, obj.id);

		dataBlock.setDoubleValue(3, obj.getValue(obj.lat));
		dataBlock.setDoubleValue(4, obj.getValue(obj.lon));
		dataBlock.setDoubleValue(5, obj.getValue(obj.alt));
		dataBlock.setDoubleValue(6, obj.getValue(obj.heading));
		dataBlock.setDoubleValue(7, obj.getValue(obj.speed));

		// update latest record and send event
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		String flightUid = FlightAwareSensor.FLIGHT_POSITION_UID_PREFIX + oshFlightId;
		latestUpdateTimes.put(flightUid, obj.getClock());
		latestRecords.put(flightUid, latestRecord);   
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, FlightPositionOutput.this, dataBlock));        	
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
		return latestRecords.get(entityID);
	}

}
