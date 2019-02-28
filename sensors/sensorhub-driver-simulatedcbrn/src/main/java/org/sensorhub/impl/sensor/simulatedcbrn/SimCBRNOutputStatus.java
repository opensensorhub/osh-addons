/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.simulatedcbrn;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class SimCBRNOutputStatus extends AbstractSensorOutput<SimCBRNSensor>
{
	DataComponent cbrnStatusData;
	DataEncoding cbrnEncoding;
	Timer timer;
	Random rand = new Random();

	// Reference values used as a basis for building randomized vals
	double tempRef = 20.0;

	// "Sensor" variables (what gets output as the sensor data
	boolean bitFail = false;
	boolean alertStatus = false;
	boolean maintNeeded = false;
	String nakStatus = "";


	public SimCBRNOutputStatus(SimCBRNSensor parentSensor)
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "STATUS";
	}


	protected void init() {
		SWEHelper fac = new SWEHelper();

		// Build SWE Common record structure
		cbrnStatusData = fac.newDataRecord(6);
		cbrnStatusData.setName(getName());
//		cbrnStatusData.setDefinition("http://sensorml.com/ont/swe/property/ToxicAgent");
//		cbrnStatusData.setDescription("CBRN measurements");

		// Add fields
		cbrnStatusData.addComponent("time", fac.newTimeStampIsoUTC());
		cbrnStatusData.addComponent("bit_failure", fac.newBoolean("http://sensorml.com/ont/swe/property/BIT_Failure", null, null));
		cbrnStatusData.addComponent("status_alert", fac.newBoolean("http://sensorml.com/ont/swe/property/Alert", null, null));
		cbrnStatusData.addComponent("need_maintenance", fac.newBoolean("http://sensorml.com/ont/swe/property/MaintenanceNeeded", null, null) );
		cbrnStatusData.addComponent("NAK_status", fac.newText("http://sensorml.com/ont/swe/property/NAK_Details", null,null));

		cbrnEncoding = fac.newTextEncoding(",", "\n");

	}
	// will need to do some of the simulation here save for later
	private void sendMeasurement()
	{
		double time = System.currentTimeMillis()/1000;

		// Temperature sim (copied from FakeWeatherOutput)
		//temp += variation(temp, tempRef, 0.001, 0.1);

		// Build DataBlock
		DataBlock dataBlock = cbrnStatusData.createDataBlock();
		dataBlock.setDoubleValue(0, time);
		dataBlock.setBooleanValue(1, bitFail);
		dataBlock.setBooleanValue(2, alertStatus);
		dataBlock.setBooleanValue(3, maintNeeded);
		dataBlock.setStringValue(4,nakStatus);


		//this method call is required to push data
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, SimCBRNOutputStatus.this, dataBlock));
	}


	protected void start()
	{
		if (timer != null)
			return;
		timer = new Timer();

		// start main measurement generation thread
		TimerTask task = new TimerTask() {
			public void run()
			{
				sendMeasurement();
			}
		};

		timer.scheduleAtFixedRate(task, 0, (long)(getAverageSamplingPeriod()*1000));
	}


	@Override
	protected void stop()
	{
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}
	}


	@Override
	public double getAverageSamplingPeriod()
	{
		// sample every 1 second
		return 1.0;
	}


	@Override
	public DataComponent getRecordDescription()
	{
		return cbrnStatusData;
	}


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return cbrnEncoding;
	}
}
