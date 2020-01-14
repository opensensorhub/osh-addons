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

package org.sensorhub.impl.sensor.intellisense;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;


/**
 * 
 * @author Tony Cook
 *
 *  	aviationTimerTas polls aviation csv Metar file at POLLING_INTERVAL and checks for new records.  
 */
public class IntellisenseOutput extends AbstractSensorOutput<IntellisenseSensor> implements IMultiSourceDataInterface
{
	private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toSeconds(20);

	DataRecord recordStruct;
	DataEncoding recordEncoding;
	Map<String, Long> latestUpdateTimes;
	Map<String, DataBlock> latestRecords = new LinkedHashMap<String, DataBlock>();

	Logger logger;
	
	public IntellisenseOutput(IntellisenseSensor parentSensor)
	{
		super(parentSensor);
		latestUpdateTimes = new HashMap<String, Long>();
		logger = parentSensor.getLogger();
		init();
	}


	@Override
	public String getName()
	{
		return "intellisenseFloodRecord";
	}


	protected void init()
	{
		SWEHelper fac = new SWEHelper();

		//  Add top level structure
		//      time
		//      deviceId
		//		Location Vector
		//		Observed Props
		
		// SWE Common data structure
		recordStruct = fac.newDataRecord(18);
		recordStruct.setName(getName());
		recordStruct.setDefinition("http://sensorml.com/ont/swe/property/intellisense/floodRecord");
		recordStruct.addField("time", fac.newTimeStampIsoUTC());
		recordStruct.addField("deviceId", fac.newText("http://sensorml.com/ont/swe/property/deviceId", "Device Id", null));
        recordStruct.addField("location", fac.newVector(null, SWEConstants.REF_FRAME_4326, new String[]{"lat","lon"}, new String[] {"Geodetic Latitude", "Longitude"}, new String[] {"deg", "deg"}, new String[] {"Lat", "Long"}));
        recordStruct.addField("FlashFloodIndicator", fac.newBoolean());
		recordStruct.addField("waterDepth", fac.newQuantity("http://sensorml.com/ont/swe/property/WaterDepth", "Water Depth", null, "in_i"));
		recordStruct.addField("navD88O", fac.newQuantity("http://sensorml.com/ont/swe/property/WaterDepth", "Regular Depth Offset", null, "ft_i"));
		recordStruct.addField("navD88D1", fac.newQuantity("http://sensorml.com/ont/swe/property/WaterDepth", "Current Depth Offset", null, "ft_i"));
		recordStruct.addField("dropSDI", fac.newCount());
		recordStruct.addField("soilSDI", fac.newCount());
		recordStruct.addField("sample", fac.newQuantity());
		recordStruct.addField("mode", fac.newQuantity());
		recordStruct.addField("lowerPressureOutput", fac.newQuantity("http://sensorml.com/ont/swe/property/Pressure", "Lower Pressure Sensor calibration Offset", null, "mb"));
		recordStruct.addField("airTemperature", fac.newQuantity("http://sensorml.com/ont/swe/property/Temperature", "Air Temperature", null, "degF"));
		recordStruct.addField("waterTemperature", fac.newQuantity("http://sensorml.com/ont/swe/property/Temperature", "Water Temperature", null, "degF"));
		recordStruct.addField("barometer", fac.newQuantity("http://sensorml.com/ont/swe/property/Pressure", "Upper board barometric pressure", null, "mb"));
		recordStruct.addField("signalStrength", fac.newQuantity("http://sensorml.com/ont/swe/property/SignalStrength", "Signal Strength", null, "%"));
		recordStruct.addField("batteryLevel", fac.newQuantity("http://sensorml.com/ont/swe/property/BatteryLevel", "Battery Level", null, "volts"));

		// mark component providing entity ID
		recordStruct.getFieldList().getProperty(1).setRole(ENTITY_ID_URI);

		// default encoding is text
		recordEncoding = fac.newTextEncoding(",", "\n");
	}


	private DataBlock recordToDataBlock(FloodRecord rec)
	{
		DataBlock dataBlock = recordStruct.createDataBlock();

		int index = 0;
		dataBlock.setDoubleValue(index++, rec.timeMs/1000.);
		dataBlock.setStringValue(index++, rec.deviceId); // Change to deviceID
		dataBlock.setDoubleValue(index++, rec.lat[0].value);
		dataBlock.setDoubleValue(index++, rec.lon[0].value);
//		setDoubleValue(dataBlock, index++, rec.elev);
        dataBlock.setBooleanValue(index++, rec.ffi1[0].value == 1);  // check
		setDoubleValue(dataBlock, index++, rec.depth1[0].value);
		setDoubleValue(dataBlock, index++, rec.NAVD88O[0].value);
		setDoubleValue(dataBlock, index++, rec.NAVD88D1[0].value);
		setIntValue(dataBlock, index++, rec.dropSDI[0].value);
		setDoubleValue(dataBlock, index++, rec.soilSDI[0].value);
		setIntValue(dataBlock, index++, rec.samp[0].value);
		setIntValue(dataBlock, index++, rec.mode[0].value);
		setDoubleValue(dataBlock, index++, rec.oPressure[0].value);
		setDoubleValue(dataBlock, index++, rec.airTemp[0].value);
		setDoubleValue(dataBlock, index++, rec.h2oTemp[0].value);
		setDoubleValue(dataBlock, index++, rec.baro[0].value);
		setIntValue(dataBlock, index++, rec.rssi[0].value);
		setDoubleValue(dataBlock, index++, rec.battery[0].value);

		return dataBlock;
	}
	
	public void addRecord(FloodRecord record) {
		try {
			latestUpdateTimes.put(record.deviceId, record.timeMs);
			latestRecordTime = System.currentTimeMillis();
			String deviceUID = IntellisenseSensor.DEVICE_UID_PREFIX + record.deviceId;
			latestRecord = recordToDataBlock(record);
			latestRecords.put(deviceUID, latestRecord);  
    		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime,deviceUID, IntellisenseOutput.this, latestRecord ));
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private void setDoubleValue(DataBlock block, int index, Double value) {
		if(value != null)
			block.setDoubleValue(index, value);
		index++;
	}
	
	private void setIntValue(DataBlock block, int index, Integer value) {
		if(value != null)
			block.setIntValue(index, value);
		index++;
	}
	
	protected void stop()
	{
//		if (timer != null)
//		{
//			timer.cancel();
//			timer = null;
//		}
	}


	@Override
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
		return recordEncoding;
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
		//    	DataBlock db = latestRecords.get(entityID);
//		for(Map.Entry<String, DataBlock> dbe: latestRecords.entrySet()) {
//			String key = dbe.getKey();
//			DataBlock val = dbe.getValue();
//		}
		return latestRecords.get(entityID);
	}
	
}
