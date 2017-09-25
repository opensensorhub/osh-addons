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
public class FltawareOutput extends AbstractSensorOutput<FltawareSensor>  
{
	private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toSeconds(5);

	DataRecord recordStruct;
	DataEncoding encoding;	
	
	public FltawareOutput(FltawareSensor parentSensor) 
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

		//  Add top level structure
		//	 time, alt, lat[],  lon[], mesh[][]

		// SWE Common data structure
		recordStruct = fac.newDataRecord(6);
		
		// default encoding is text
		encoding = fac.newTextEncoding(",", "\n");
	}

	public void start() throws SensorHubException {
		// Nothing to do 
	}
	
	public void sendMeasurement()
	{                
		// update latest record and send event
//		latestRecord = dataBlock;
//		latestRecordTime = System.currentTimeMillis();
//		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, FltawareOutput.this, dataBlock));        
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

}
