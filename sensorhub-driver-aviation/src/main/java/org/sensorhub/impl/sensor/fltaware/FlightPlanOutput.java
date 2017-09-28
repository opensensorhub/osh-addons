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

package org.sensorhub.impl.sensor.fltaware;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
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
 */
public class FlightPlanOutput extends AbstractSensorOutput<FltawareSensor>  
{
	private static final int AVERAGE_SAMPLING_PERIOD = 1; //(int)TimeUnit.SECONDS.toSeconds(5);

	DataRecord recordStruct;
	DataEncoding encoding;	
	
	public FlightPlanOutput(FltawareSensor parentSensor) 
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "FltAware Sensor";
	}

	protected void init()
	{
		SWEHelper fac = new SWEHelper();
		GeoPosHelper geoHelper = new GeoPosHelper();
		
		//  Add top level structure for flight plan
		//	 time, flightId, faFlightId, numWaypoints, lat[], lon[]

		// SWE Common data structure
		recordStruct = fac.newDataRecord(6);
		recordStruct.setName(getName());
		recordStruct.setDefinition("http://earthcastwx.com/ont/swe/property/flightPlan"); // ??
		
        recordStruct.addComponent("time", fac.newTimeStampIsoGPS());

        // flightIds
		recordStruct.addField("flightId", fac.newText("", "flightId", "Internally generated flight desc (flightNum_DestAirport"));
		recordStruct.addField("faFlightId", fac.newText("", "flightId", "FlightAware flightId- not sure we will need this"));
        
		//  num of points
		Count numPoints = fac.newCount(DataType.INT);
		numPoints.setDefinition("http://sensorml.com/ont/swe/property/NumberOfSamples"); 
		numPoints.setId("NUM_POINTS");
		recordStruct.addComponent("numPoints",numPoints);
		
		Quantity latQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Latitude", "Latitude", null, "deg", DataType.FLOAT);
		DataArray latArr = fac.newDataArray();
		latArr.setElementType("Latitude", latQuant);
		latArr.setElementCount(numPoints);
		recordStruct.addComponent("LatitudeArray", latArr);

		Quantity lonQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Longitude", "Longitude", null, "deg", DataType.FLOAT);
		DataArray lonArr = fac.newDataArray();
		lonArr.setElementType("Longitude", lonQuant);
		lonArr.setElementCount(numPoints);
		recordStruct.addComponent("LongitudeArray", lonArr);
		
        
		// default encoding is text
		encoding = fac.newTextEncoding(",", "\n");
	}

	public void start() throws SensorHubException {
		// Nothing to do 
	}
	
	public void sendMeasurement(FlightObject obj)
	{                
		//  sequential arrays
		float [] lats = obj.getLats();
		float [] lons = obj.getLons();
		
		int numPts = lats.length;
		// update array sizes
		DataArray latArr = (DataArray)recordStruct.getComponent(4);
		DataArray lonArr = (DataArray)recordStruct.getComponent(5);
		latArr.updateSize(numPts);
		lonArr.updateSize(numPts);

		// build data block from Mesh Record
		DataBlock dataBlock = recordStruct.createDataBlock();
		dataBlock.setDoubleValue(0, obj.getClock());
		dataBlock.setStringValue(1, obj.getInternalId());
		dataBlock.setStringValue(2, obj.id);
		
		dataBlock.setIntValue(3, numPts);
		
		((DataBlockMixed)dataBlock).getUnderlyingObject()[4].setUnderlyingObject(lats);
		((DataBlockMixed)dataBlock).getUnderlyingObject()[5].setUnderlyingObject(lons);

		// update latest record and send event
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, FlightPlanOutput.this, dataBlock));        	}

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

}
