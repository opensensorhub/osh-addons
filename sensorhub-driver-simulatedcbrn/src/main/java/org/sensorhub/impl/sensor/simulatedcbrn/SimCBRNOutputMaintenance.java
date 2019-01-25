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

import net.opengis.swe.v20.*;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class SimCBRNOutputMaintenance extends AbstractSensorOutput<SimCBRNSensor>
{
	DataComponent cbrnMaintData;
	DataEncoding cbrnEncoding;
	Timer timer;
	Random rand = new Random();

	// Reference values used as a basis for building randomized vals
	double tempRef = 20.0;

	// "Sensor" variables (what gets output as the sensor data
	double temp = tempRef;
	//String eventStatus = "NONE";
	String fault = "";
	boolean lowSieve = false;
	int usageHours = 0;


	public SimCBRNOutputMaintenance(SimCBRNSensor parentSensor)
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "MAINT";
	}


	protected void init() {
		SWEHelper fac = new SWEHelper();

		// Build SWE Common record structure
		cbrnMaintData = fac.newDataRecord(5);
		cbrnMaintData.setName(getName());
//		cbrnMaintData.setDefinition("http://sensorml.com/ont/swe/property/ToxicAgent");
//		cbrnMaintData.setDescription("CBRN measurements");

		// Add fields
		cbrnMaintData.addComponent("time", fac.newTimeStampIsoUTC());

		Category maint_faults = fac.newCategory("http://sensorml.com/ont/swe/property/MaintenanceFaults", null, null, null);
		AllowedTokens faultList = fac.newAllowedTokens();
		faultList.addValue("Change_Sieve");
		faultList.addValue("Pressure/Temperature_Range_Error");
		faultList.addValue("Corona_Fault");
		faultList.addValue("Fan_Current_Above_Limit");
		faultList.addValue("Init_Self_Test_Failure");
		faultList.addValue("Health_Check_Failure");
		faultList.addValue("Code_Checksum_Error");
		faultList.addValue("HT_Outside_Limits");
		faultList.addValue("Fan_Life_Warning");
		faultList.addValue("Configuration_Not_Valid");
		faultList.addValue("None");
		maint_faults.setConstraint(faultList);
		cbrnMaintData.addComponent("faults", maint_faults);

		cbrnMaintData.addComponent("low_sieve", fac.newBoolean("http://sensorml.com/ont/swe/property/LowSieveWarning", null, null));

		Quantity maint_Usage = fac.newQuantity("http://sensorml.com/ont/swe/property/UsageHours", null, null, null);
		AllowedValues usageInterval = fac.newAllowedValues();
		usageInterval.addInterval(new double[]{0,99999});
		maint_Usage.setConstraint(usageInterval);
		cbrnMaintData.addComponent("usage", maint_Usage);

		cbrnEncoding = fac.newTextEncoding(",", "\n");

	}
	// will need to do some of the simulation here save for later
	private void sendMeasurement()
	{
		double time = System.currentTimeMillis()/1000;

		// Temperature sim (copied from FakeWeatherOutput)
		//temp += variation(temp, tempRef, 0.001, 0.1);

		// Build DataBlock
		DataBlock dataBlock = cbrnMaintData.createDataBlock();
		dataBlock.setDoubleValue(0, time);
		dataBlock.setStringValue(1, fault);
		dataBlock.setBooleanValue(2, lowSieve);
		dataBlock.setIntValue(3, usageHours);


		//this method call is required to push data
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, SimCBRNOutputMaintenance.this, dataBlock));
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
		return cbrnMaintData;
	}


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return cbrnEncoding;
	}
}
