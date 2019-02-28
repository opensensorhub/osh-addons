/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mesh;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;

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
 */
public class MeshOutput extends AbstractSensorOutput<MeshSensor>  
{
	private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toSeconds(5);
	//  Are these dims always fixed?  Should make this settable, even if fixed now
	private static final int X_SIZE = 902;
	private static final int Y_SIZE = 674;
	private static final int NUM_POINTS = X_SIZE * Y_SIZE;

	DataRecord meshRecordStruct;
	DataEncoding meshEncoding;	
	
	
	public MeshOutput(MeshSensor parentSensor)
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "MeshSensor";
	}
	

	protected void init()
	{
		SWEHelper fac = new SWEHelper();

		//  Add top level structure
		//	 time, numPts, lat[], lon[], mesh[]

		// SWE Common data structure
		meshRecordStruct = fac.newDataRecord(5);
		meshRecordStruct.setName(getName());
		meshRecordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/mesh"); // ??

		// time 
		meshRecordStruct.addField("time", fac.newTimeStampIsoUTC());
		
		//  num of points
		Count numPoints = fac.newCount(DataType.INT);
		numPoints.setDefinition("http://sensorml.com/ont/swe/property/NumberOfSamples"); 
		numPoints.setId("NUM_POINTS");
		meshRecordStruct.addComponent("numPoints",numPoints);
		
		Quantity latQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Latitude", "Latitude", null, "deg", DataType.FLOAT);
		DataArray latArr = fac.newDataArray();
		latArr.setElementType("Latitude", latQuant);
		latArr.setElementCount(numPoints);
		meshRecordStruct.addComponent("LatitudeArray", latArr);

		Quantity lonQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Longitude", "Longitude", null, "deg", DataType.FLOAT);
		DataArray lonArr = fac.newDataArray();
		lonArr.setElementType("Longitude", lonQuant);
		lonArr.setElementCount(numPoints);
		meshRecordStruct.addComponent("LongitudeArray", lonArr);
		

		Quantity meshQuant = fac.newQuantity("http://earthcastwx.com/ont/swe/property/mesh", "Maximum Estimated Hail Size", null, "mm", DataType.FLOAT);
		DataArray meshArr = fac.newDataArray();
		meshArr.setElementType("MESH", meshQuant);
		meshArr.setElementCount(numPoints);
		meshRecordStruct.addComponent("meshArray", meshArr);
//		
//		// default encoding is text
		meshEncoding = fac.newTextEncoding(",", "\n");
	}
	
	
	public void sendMeasurement(MeshRecord rec)
	{                
		//  sequential arrays
		float [] lats = rec.getLats();
		float [] lons = rec.getLons();
		float [] vals = rec.getValues();
		
		int numPts = lats.length;
		// update array sizes
		DataArray latArr = (DataArray)meshRecordStruct.getComponent(2);
		DataArray lonArr = (DataArray)meshRecordStruct.getComponent(3);
		DataArray meshArr = (DataArray)meshRecordStruct.getComponent(4);
		latArr.updateSize(numPts);
		lonArr.updateSize(numPts);
		meshArr.updateSize(numPts);

		// build data block from Mesh Record
		DataBlock dataBlock = meshRecordStruct.createDataBlock();
		dataBlock.setDoubleValue(0, rec.timeUtc);
		dataBlock.setIntValue(1, numPts);
		
		
		((DataBlockMixed)dataBlock).getUnderlyingObject()[2].setUnderlyingObject(lats);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[3].setUnderlyingObject(lons);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[4].setUnderlyingObject(vals);

		// update latest record and send event
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, MeshOutput.this, dataBlock));        
	}
	

	public double getAverageSamplingPeriod()
	{
		return AVERAGE_SAMPLING_PERIOD;
	}


	@Override 
	public DataComponent getRecordDescription()
	{
		return meshRecordStruct;
	}


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return meshEncoding;
	}

}
