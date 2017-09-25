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

package org.sensorhub.impl.sensor.mesh;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;

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
	private MeshReader reader;
	
	public MeshOutput(MeshSensor parentSensor) throws IOException
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
		//	 time, alt, lat[],  lon[], mesh[][]

		// SWE Common data structure
		meshRecordStruct = fac.newDataRecord(6);
		meshRecordStruct.setName(getName());
		meshRecordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/mesh"); // ??

		// time 
		meshRecordStruct.addField("time", fac.newTimeStampIsoUTC());
		//  array sizes
		meshRecordStruct.addComponent("lonSize", 
				fac.newQuantity("http://sensorml.com/ont/swe/property/NumberOfSamples", "lonSize", "nummber of longitude points", "", DataType.INT));
		meshRecordStruct.addComponent("latSize", 
				fac.newQuantity("http://sensorml.com/ont/swe/property/NumberOfSamples", "latSize", "nummber of latitude points", "", DataType.INT));

		//  lon array 
		Quantity lonQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Longitude", "Longitude", null, "deg", DataType.FLOAT);
		DataArray lonArr = fac.newDataArray(X_SIZE);
		lonArr.setElementType("Longitude", lonQuant);
		meshRecordStruct.addComponent("LongitudeArray", lonArr);
		
		Quantity latQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Latitude", "Latitude", null, "deg", DataType.FLOAT);
		DataArray latArr = fac.newDataArray(Y_SIZE);
		latArr.setElementType("Latitude", latQuant);
		meshRecordStruct.addComponent("LatitudeArray", latArr);

		Quantity meshQuant = fac.newQuantity("http://earthcastwx.com/ont/swe/property/mesh", "Maximum Estimated Hail Size", null, "mm", DataType.FLOAT);
		DataArray meshArr = fac.newDataArray(NUM_POINTS);
		meshArr.setElementType("MESH", meshQuant);
		meshRecordStruct.addComponent("meshArray", meshArr);
		
		// default encoding is text
		meshEncoding = fac.newTextEncoding(",", "\n");
	}

	public void start() throws SensorHubException {
		// Nothing to do 
	}
	
	public void sendMeasurement(MeshRecord rec)
	{                
		// build data block from Mesh Record
		DataBlock dataBlock = meshRecordStruct.createDataBlock();
		dataBlock.setDoubleValue(0, rec.timeUtc);
		dataBlock.setIntValue(1, X_SIZE);
		dataBlock.setIntValue(2, Y_SIZE);
		
		float [] mesh = new float[NUM_POINTS];
		for(int j=0; j<Y_SIZE; j++) {
			for(int i=0, idx=0; i<X_SIZE; i++) {
				mesh[idx++] = rec.mesh[j][i];
			}			
		}
		Object  obj  = dataBlock.getUnderlyingObject();
		((DataBlockMixed)dataBlock).getUnderlyingObject()[3].setUnderlyingObject(rec.lon);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[4].setUnderlyingObject(rec.lat);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[5].setUnderlyingObject(mesh);

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
