/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.nldn;

import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEConstants;
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
public class NldnOutput extends AbstractSensorOutput<NldnSensor>  
{
	private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toSeconds(5);
	//  Are these dims always fixed?  Should make this settable, even if fixed now
	private static final int X_SIZE = 902;
	private static final int Y_SIZE = 674;
	private static final int NUM_POINTS = X_SIZE * Y_SIZE;

	DataRecord nldnRecordStruct;
	DataEncoding nldnEncoding;
	
	
	public NldnOutput(NldnSensor parentSensor)
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "nldnData";
	}
	

	protected void init()
	{
		SWEHelper fac = new SWEHelper();

		//  Top level structure:
		//	 time, numPts, lat[], lon[], mesh[]

		// SWE Common data structure
		nldnRecordStruct = fac.newDataRecord(5);
		nldnRecordStruct.setName(getName());
		nldnRecordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/nldn"); // ??

		// time 
		nldnRecordStruct.addField("time", fac.newTimeStampIsoUTC());
	
		//  num of points
		Count numPoints = fac.newCount(DataType.INT);
		numPoints.setDefinition(SWEConstants.DEF_NUM_POINTS); 
		numPoints.setId("NUM_POINTS");
		nldnRecordStruct.addComponent("numPoints",numPoints);
		
		Quantity latQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Latitude", "Latitude", null, "deg", DataType.FLOAT);
		DataArray latArr = fac.newDataArray();
		latArr.setElementType("Latitude", latQuant);
		latArr.setElementCount(numPoints);
		nldnRecordStruct.addComponent("LatitudeArray", latArr);

		Quantity lonQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Longitude", "Longitude", null, "deg", DataType.FLOAT);
		DataArray lonArr = fac.newDataArray();
		lonArr.setElementType("Longitude", lonQuant);
		lonArr.setElementCount(numPoints);
		nldnRecordStruct.addComponent("LongitudeArray", lonArr);
		

		Quantity nldnQuant = fac.newQuantity("http://earthcastwx.com/ont/swe/property/nldn", "Lightning Data", null, "?", DataType.FLOAT);
		DataArray nldnArr = fac.newDataArray();
		nldnArr.setElementType("NLDN", nldnQuant);
		nldnArr.setElementCount(numPoints);
		nldnRecordStruct.addComponent("nldnArray", nldnArr);
		
		// default encoding is text
		nldnEncoding = fac.newTextEncoding(",", "\n");
	}
	
	
	public void sendMeasurement(NldnRecord rec)
	{                
		//  sequential arrays
		float [] lats = rec.getLats();
		float [] lons = rec.getLons();
		float [] vals = rec.getValues();
		
		int numPts = lats.length;
		// update array sizes
		DataArray latArr = (DataArray)nldnRecordStruct.getComponent(2);
		DataArray lonArr = (DataArray)nldnRecordStruct.getComponent(3);
		DataArray nldnArr = (DataArray)nldnRecordStruct.getComponent(4);
		latArr.updateSize(numPts);
		lonArr.updateSize(numPts);
		nldnArr.updateSize(numPts);

		// build data block from Mesh Record
		DataBlock dataBlock = nldnRecordStruct.createDataBlock();
		dataBlock.setDoubleValue(0, rec.timeUtc);
		dataBlock.setIntValue(1, numPts);
		
		
		((DataBlockMixed)dataBlock).getUnderlyingObject()[2].setUnderlyingObject(lats);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[3].setUnderlyingObject(lons);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[4].setUnderlyingObject(vals);

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
