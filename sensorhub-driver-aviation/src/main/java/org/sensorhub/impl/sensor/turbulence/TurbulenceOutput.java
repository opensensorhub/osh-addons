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

package org.sensorhub.impl.sensor.turbulence;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockList;
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


/**
 * 
 * @author Tony Cook
 * 
 * 
 * TODO- add zulu time output somewhere
 *
 */
public class TurbulenceOutput extends AbstractSensorOutput<TurbulenceSensor> //implements IMultiSourceDataInterface  
{
	private static final int AVERAGE_SAMPLING_PERIOD = 1; //(int)TimeUnit.SECONDS.toSeconds(5);

	DataRecord recordStruct;
	DataEncoding encoding;	
	Map<String, Long> latestUpdateTimes;
	Map<String, DataBlock> latestRecords = new LinkedHashMap<String, DataBlock>();

	private DataRecord profileStruct;

	
	public TurbulenceOutput(TurbulenceSensor parentSensor) 
	{
		super(parentSensor);
		latestUpdateTimes = new HashMap<String, Long>();
	}


	@Override
	public String getName()
	{
		return "Turbulence profile data";
	}

	protected void initNumPointsPlusArray()
	{
		SWEHelper fac = new SWEHelper();
		
		//  Add top level structure for flight plan
		//	 numPoints?   array of time, lat, lon, numPoints, float[] turbVal
		recordStruct = fac.newDataRecord(2);  // 1 big array
		recordStruct.setName(getName());
		recordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/turbulenceProfile"); // ??

		// SWE Common data structure
		//  num of points- can client infer this or do we have to iunclude?
		Count numPoints = fac.newCount(DataType.INT);
		numPoints.setId("NUM_POINTS");
		recordStruct.addComponent("numPoints",numPoints);

		// profiles == time, lat, lon, numPoints, float[]
		DataComponent profiles = fac.newDataRecord();
		
		//  turbARr = [] of profiles
		DataArray turbArr = fac.newDataArray();
		turbArr.setElementType("Turbulence Profiles", profiles);
		turbArr.setElementCount(numPoints);
		recordStruct.addComponent("Turbulence Profile", turbArr);
		
		profiles.addComponent("time", fac.newTimeStampIsoGPS());
		Quantity latQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Latitude", "Latitude", null, "deg", DataType.DOUBLE);
		Quantity lonQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Longitde", "Longitde", null, "deg", DataType.DOUBLE);
		profiles.addComponent("Latitude", latQuant);
		profiles.addComponent("Longitude", lonQuant);

		//  Vertical Profile
		Count turbulencePoints = fac.newCount(DataType.INT);
		turbulencePoints.setDefinition("http://sensorml.com/ont/swe/property/NumberOfSamples"); 
		turbulencePoints.setId("PROFILE_POINTS");
		profiles.addComponent("numTurbulencePoints",turbulencePoints);
		
		//  Turb values
		Quantity valuesQuant = fac.newQuantity("http://earthcastwx.com/ont/swe/property/TurbulenceVales", "Turbulence Profile Values", null, "", DataType.FLOAT);
		DataArray valuesArr = fac.newDataArray();
		valuesArr.setElementType("Turbulence", valuesQuant);
		valuesArr.setElementCount(turbulencePoints);
		profiles.addComponent("Turbulence Profile", valuesArr);

		encoding = fac.newTextEncoding(",", "\n");
	}

	protected void init()
	{
		SWEHelper fac = new SWEHelper();
		GeoPosHelper geoHelper = new GeoPosHelper();
		
		//  wrap in Array with no size? is this legal
		recordStruct = fac.newDataRecord(2); 
		recordStruct.setName(getName());
		recordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/turbulenceProfile"); // ??
		
		Count numProfiles = fac.newCount(DataType.INT);
		numProfiles.setId("NUM_PROFILES");
		recordStruct.addComponent("NumProfiles",numProfiles);
		
		profileStruct = fac.newDataRecord(5); 
		profileStruct.setName(getName());
		profileStruct.setDefinition("http://earthcastwx.com/ont/swe/property/turbulenceProfile"); // ??

		//  
		DataArray profileArr = fac.newDataArray();
		profileArr.setElementType("Turbulence", profileStruct);
		profileArr.setElementCount(numProfiles);
		recordStruct.addComponent("Profiles", profileArr);
		
		//	 time, lat, lon, numPoints, float[] turbVal
		profileStruct.addComponent("time", fac.newTimeStampIsoGPS());
		Quantity latQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Latitude", "Latitude", null, "deg", DataType.FLOAT);
		Quantity lonQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Longitde", "Longitde", null, "deg", DataType.FLOAT);
		profileStruct.addComponent("Latitude", latQuant);
		profileStruct.addComponent("Longitude", lonQuant);

		//  Vertical Profile
		Count numPoints = fac.newCount(DataType.INT);
		numPoints.setDefinition("http://sensorml.com/ont/swe/property/NumberOfSamples"); 
		numPoints.setId("PROFILE_POINTS");
		profileStruct.addComponent("numTurbulencePoints",numPoints);
		
		//  Turb values
		Quantity valuesQuant = fac.newQuantity("http://earthcastwx.com/ont/swe/property/TurbulenceVales", "Turbulence Profile Values", null, "", DataType.FLOAT);
		DataArray valuesArr = fac.newDataArray();
		valuesArr.setElementType("Turbulence", valuesQuant);
		//  DataArrayImpl.579
		//             elementCount.setValue((Count)sizeComponent);  //  (Count)sizeComponent == 0 
		numPoints.setValue(52);
		valuesArr.setElementCount(numPoints);
		profileStruct.addComponent("Turbulence Profile", valuesArr);

		encoding = fac.newTextEncoding(",", "\n");
	}

	public void start() throws SensorHubException {
		//  
	}
	
	public void sendProfiles(List<TurbulenceRecord> recs)
	{
		DataArray profileArr = (DataArray)recordStruct.getComponent(1);
		profileArr.updateSize(recs.size());
		DataBlock bigBlock = recordStruct.createDataBlock();
		bigBlock.setIntValue(0, recs.size());
		DataBlock profileBlock = ((DataBlockMixed)bigBlock).getUnderlyingObject()[1];
		
		// build data block from Mesh Record
		int idx = 0;
		for(TurbulenceRecord rec: recs) {
			DataArray turbArr = (DataArray)profileStruct.getComponent(4);
			turbArr.updateSize(rec.turbulence.length);
			DataBlock dataBlock = profileStruct.createDataBlock();

			dataBlock.setDoubleValue(0, rec.time);
			dataBlock.setDoubleValue(1, rec.lat);
			dataBlock.setDoubleValue(2, rec.lon);
			dataBlock.setIntValue(3 , rec.turbulence.length);
			((DataBlockMixed)dataBlock).getUnderlyingObject()[4].setUnderlyingObject(rec.turbulence);
			Object o = ((DataBlockList)profileBlock).getUnderlyingObject();
			LinkedList<DataBlock> ll = (LinkedList<DataBlock>) o;
			ll.set(idx++, dataBlock);
		}
//		DataBlock dbObj = ((DataBlockMixed)bigBlock).getUnderlyingObject()[1];
//		dbObj.setUnderlyingObject(obj);
		
		// update latest record and send event
		latestRecord = bigBlock;
		latestRecordTime = System.currentTimeMillis();
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, TurbulenceOutput.this, bigBlock)); 	
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

//
//	@Override
//	public Collection<String> getEntityIDs()
//	{
////		return parentSensor.getEntityIDs();
//		return null;
//	}
//
//
//	@Override
//	public Map<String, DataBlock> getLatestRecords()
//	{
//		return Collections.unmodifiableMap(latestRecords);
//	}
//
//
//	@Override
//	public DataBlock getLatestRecord(String entityID) {
////		for(Map.Entry<String, DataBlock> dbe: latestRecords.entrySet()) {
////			String key = dbe.getKey();
////			DataBlock val = dbe.getValue();
////			System.err.println(key + " : " + val);
////		}
//		DataBlock b = latestRecords.get(entityID);
//		return b;
//	}
	

}
