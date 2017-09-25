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

package org.sensorhub.impl.sensor.nldn;

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
public class NldnOutput extends AbstractSensorOutput<NldnSensor>  
{
	private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toSeconds(5);
	//  Are these dims always fixed?  Should make this settable, even if fixed now
	private static final int X_SIZE = 902;
	private static final int Y_SIZE = 674;
	private static final int NUM_POINTS = X_SIZE * Y_SIZE;

	DataRecord nldnRecordStruct;
	DataEncoding nldnEncoding;	
	private NldnReader reader;
	
	public NldnOutput(NldnSensor parentSensor) throws IOException
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "NldnSensor";
	}

	protected void init()
	{
		SWEHelper fac = new SWEHelper();

		// SWE Common data structure
		nldnRecordStruct = fac.newDataRecord(6);
		nldnRecordStruct.setName(getName());
		nldnRecordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/nldn"); // ??

		// time 
		nldnRecordStruct.addField("time", fac.newTimeStampIsoUTC());
		//  Array sizes
		nldnRecordStruct.addComponent("lonSize", 
				fac.newQuantity("http://sensorml.com/ont/swe/property/NumberOfSamples", "lonSize", "nummber of longitude points", "", DataType.INT));
		nldnRecordStruct.addComponent("latSize", 
				fac.newQuantity("http://sensorml.com/ont/swe/property/NumberOfSamples", "latSize", "nummber of latitude points", "", DataType.INT));

		//  lon array 
		Quantity lonQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Longitude", "Longitude", null, "deg", DataType.FLOAT);
		DataArray lonArr = fac.newDataArray(X_SIZE);
		lonArr.setElementType("Longitude", lonQuant);
		nldnRecordStruct.addComponent("LongitudeArray", lonArr);
		
		Quantity latQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Latitude", "Latitude", null, "deg", DataType.FLOAT);
		DataArray latArr = fac.newDataArray(Y_SIZE);
		latArr.setElementType("Latitude", latQuant);
		nldnRecordStruct.addComponent("LatitudeArray", latArr);

		Quantity nldnQuant = fac.newQuantity("http://earthcastwx.com/ont/swe/property/mesh", "Maximum Estimated Hail Size", null, "mm", DataType.FLOAT);
		DataArray nldnArr = fac.newDataArray(NUM_POINTS);
		nldnArr.setElementType("NLDN", nldnQuant);
		nldnRecordStruct.addComponent("nldnArray", nldnArr);
		
		// default encoding is text
		nldnEncoding = fac.newTextEncoding(",", "\n");
	}

	public void start() throws SensorHubException {
		// Nothing to do 
	}
	
	public void sendMeasurement(NldnRecord rec)
	{                
		// build data block from Mesh Record
		DataBlock dataBlock = nldnRecordStruct.createDataBlock();
		dataBlock.setDoubleValue(0, rec.timeUtc);
		dataBlock.setIntValue(1, X_SIZE);
		dataBlock.setIntValue(2, Y_SIZE);

		float [] nldn = new float[NUM_POINTS];
		for(int j=0; j<Y_SIZE; j++) {
			for(int i=0, idx=0; i<X_SIZE; i++) {
				nldn[idx++] = rec.nldn[j][i];
			}			
		}
		Object  obj  = dataBlock.getUnderlyingObject();
		((DataBlockMixed)dataBlock).getUnderlyingObject()[3].setUnderlyingObject(rec.lon);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[4].setUnderlyingObject(rec.lat);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[5].setUnderlyingObject(nldn);

		// update latest record and send event
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, NldnOutput.this, dataBlock));        
	}

	public double getAverageSamplingPeriod()
	{
		return AVERAGE_SAMPLING_PERIOD;
	}


	@Override 
	public DataComponent getRecordDescription()
	{
		return nldnRecordStruct;
	}


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return nldnEncoding;
	}

}
