package org.sensorhub.impl.sensor.flightAware;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Vector;

public class FlightPositionOutput extends AbstractSensorOutput<FlightAwareDriver> implements IMultiSourceDataInterface  
{
    static final String DEF_FLIGHTPOS_REC = SWEHelper.getPropertyUri("aero/FlightPosition");
    static final String DEF_VERTICAL_RATE = SWEHelper.getPropertyUri("areo/VerticalRate");
    static final String DEF_GROUND_SPEED = SWEHelper.getPropertyUri("GroundSpeed");
    static final String DEF_HEADING = SWEHelper.getPropertyUri("TrueHeading");
    private static final int AVERAGE_SAMPLING_PERIOD = 30;

	DataRecord recordStruct;
	DataEncoding encoding;	

	Map<String, Long> latestUpdateTimes = new ConcurrentHashMap<>();
	Map<String, DataBlock> latestRecords = new ConcurrentHashMap<>();  // key is position uid

	public FlightPositionOutput(FlightAwareDriver parentSensor) 
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "flightPos";
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
		recordStruct.addField("flightId", fac.newText(ENTITY_ID_URI, "Flight ID", null));

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

	public void sendPosition(FlightObject obj, String oshFlightId)
	{                
		int i = 0;
		
	    // build data block from FlightObject Record
		DataBlock dataBlock = recordStruct.createDataBlock();
		dataBlock.setDoubleValue(i++, obj.getClock());
		dataBlock.setStringValue(i++, obj.getOshFlightId());
		dataBlock.setDoubleValue(i++, obj.getValue(obj.lat));
		dataBlock.setDoubleValue(i++, obj.getValue(obj.lon));
		dataBlock.setDoubleValue(i++, obj.getValue(obj.alt));
		dataBlock.setDoubleValue(i++, obj.getValue(obj.heading));
		dataBlock.setDoubleValue(i++, obj.getValue(obj.gs));
        dataBlock.setDoubleValue(i++, obj.verticalChange);

		// update latest record and send event
		latestRecord = dataBlock;
		latestRecords.put(oshFlightId, latestRecord);
		latestRecordTime = System.currentTimeMillis();
        latestUpdateTimes.put(oshFlightId, obj.getClock());
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
	public DataBlock getLatestRecord(String entityID)
	{
		return latestRecords.get(entityID);
	}

}
