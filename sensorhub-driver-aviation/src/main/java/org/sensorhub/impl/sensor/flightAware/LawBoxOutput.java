package org.sensorhub.impl.sensor.flightAware;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

public class LawBoxOutput extends AbstractSensorOutput<FlightAwareSensor> implements IMultiSourceDataInterface  
{
	private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toSeconds(15); 

	DataRecord recordStruct;
	DataEncoding encoding;	

	Map<String, Long> latestUpdateTimes = new LinkedHashMap<>();
	Map<String, DataBlock> latestRecords = new LinkedHashMap<>();  //

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

		//  Output structure for flight plan
		//	 time, flightId, lawBox corners (array of 4 latLon pairs), lawBoxLowerAlt, llawBoxUpperAlt, turbHazard location (single LatLonAlt for now), maxHazardVal, incr/decr flag

		// SWE Common data structure
		recordStruct = fac.newDataRecord(8);
		recordStruct.setName(getName());
		recordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/LawBox"); // ??

		recordStruct.addComponent("time", fac.newTimeStampIsoGPS());

		// oshFlightId
		recordStruct.addField("flightId", fac.newText("http://earthcastwx.com/ont/swe/property/flightId", "flightId", "Internally generated flight desc (flightNum_DestAirport"));

		//  LaxBox corners
		Quantity latQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Latitude", "Latitude", null, "deg", DataType.DOUBLE);
		Quantity lonQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Longitude", "Longitude", null, "deg", DataType.DOUBLE);
		DataRecord cornerRec = fac.newDataRecord(2);
		cornerRec.setName("LawBoxCorner");
		cornerRec.addComponent("latitude", latQuant);
		cornerRec.addComponent("longitude", lonQuant);
		
//		recordStruct.addComponent("NumberOfCornerPoints", numCorners);
		DataArray cornerArr = fac.newDataArray(4);
		cornerArr.setDescription("Corners computed based on plane location and criteria set by Delta (encoded in LaxBoxGeometry class)");
		cornerArr.setElementType("BoxCorner", cornerRec);
//		cornerArr.setElementCount(numCorners);
		recordStruct.addComponent("LawBoxCorners", cornerArr);

		// Vertical lower and upper altitude of LawBox
		recordStruct.addField("LawBoxLowerAltitude", fac.newQuantity("http://earthcastwx.com/ont/swe/property/altitude", "LawBoxLowerCorner", null, "feet"));
		recordStruct.addField("LawBoxUpperAltitude", fac.newQuantity("http://earthcastwx.com/ont/swe/property/altitude", "LawBoxLowerCorner", null, "feet"));


		// Single Location of max Turb value in LawBox
		Vector turbVector = geoHelper.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
		turbVector.setLabel("Turbulence Hazard Location");
		turbVector.setDescription("Location of max turbulence value exceeding threshold");
		recordStruct.addComponent("MaxTurbulenceLocation", turbVector);

		// Turbulence max value
		Quantity maxTurbValue = fac.newQuantity("http://earthcastwx.com/ont/swe/property/TurbulenceMaxHazardValue", "Turbulence", null, "");
		recordStruct.addField("MaxTurbulenceValue", maxTurbValue);

		// Turbulence increasing/decreasing
		//  +1 = increasing, -1 = decreasing, 0 = no change
		recordStruct.addField("turbulenceChangeFlag", fac.newQuantity("http://earthcastwx.com/ont/swe/property/TurbulenceChangeFlag", "TurbulenceChangeFlag", null, ""));
		
		encoding = fac.newTextEncoding(",", "\n");

	}

	public void start() throws SensorHubException {
		// Nothing to do 
	}

	public DataBlock sendLawBox(LawBox lawBox)
	{                
		//		// build data block from FlightObject Record
		//	 time, flightId, lawBox corners (array of 4 latLonALt Vectors), lawBoxLowerAlt, lawBoxUpperAlt 
		//      turbHazard loacation (single LatLonAlt for now), maxHazardVal, incr/decr flag
		DataBlock dataBlock = recordStruct.createDataBlock();
		dataBlock.setDoubleValue(0, lawBox.position.getClock());
		dataBlock.setStringValue(1, lawBox.position.getOshFlightId());

		((DataBlockMixed)dataBlock).getUnderlyingObject()[2].setUnderlyingObject(lawBox.getBoundary());

		dataBlock.setDoubleValue(10, lawBox.brBottomLla.alt);
		dataBlock.setDoubleValue(11, lawBox.brTopLla.alt);
		double [] maxLla = new double [] {
			lawBox.maxCoordLla.lat,
			lawBox.maxCoordLla.lon,				
			lawBox.maxCoordLla.alt				
		};
		((DataBlockMixed)dataBlock).getUnderlyingObject()[5].setUnderlyingObject(maxLla);
//		dataBlock.setDoubleValue(5, lawBox.maxCoordLla.lat);
//		dataBlock.setDoubleValue(6, lawBox.maxCoordLla.lon);
//		dataBlock.setDoubleValue(7, lawBox.maxCoordLla.alt);
		dataBlock.setDoubleValue(15, lawBox.maxTurb);
		dataBlock.setDoubleValue(16, lawBox.changeFlag);
		//
		//		// update latest record and send event
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		String flightUid = FlightAwareSensor.LAWBOX_UID_PREFIX + lawBox.position.getOshFlightId();
		latestUpdateTimes.put(flightUid, lawBox.position.getClock());
		latestRecords.put(flightUid, latestRecord);   
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, LawBoxOutput.this, dataBlock));

		return dataBlock;
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
		DataBlock b = latestRecords.get(entityID);
	
		LawBox lawBox = null;
		try {
			int lastColon = entityID.lastIndexOf(':');
			if(lastColon == -1) {
				log.error("Malformed entityID in getLatestRecord.");
				return null;
			}
			String oshFlighId = entityID.substring(lastColon + 1);
			lawBox = parentSensor.getLawBox(oshFlighId);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(lawBox == null) {
			log.info("LawBoxOutput.getLatest():  Error Reading lawBox.");
			return null;
		}
		if(b != null) {
			//  If requests are spaced out, this way of computing will not be reliable
			//  Needs to be done for every active flight every time pos updates
			Double prevTurb = b.getDoubleValue(15);
			int prevMag = (int)(prevTurb * 10.0); 
			int newMag = (int)(lawBox.maxTurb * 10.0);
			if (prevMag == newMag) {
				lawBox.changeFlag = 0;
			} else {
				lawBox.changeFlag = (newMag > prevMag) ? 1 : -1;				
			}
		}

		DataBlock latestBlock = sendLawBox(lawBox);
		return latestBlock;
	}
}
