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
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class SimCBRNOutputID extends AbstractSensorOutput<SimCBRNSensor>
{
	DataComponent cbrnIDData;
	DataEncoding cbrnEncoding;
	Timer timer;
	Random rand = new Random();

	// Reference values used as a basis for building randomized vals
	double tempRef = 20.0;

	// "Sensor" variables (what gets output as the sensor data
	String hostID = "111-11-111-HID";
	String mp_name = "User, Test";
	String mp_org = "TestOrg";
	String mp_phone = "234-567-8901";
	String mp_email = "fake@fmail.com";
	String mp_address = "555 A Street, ATown, XX 55555";

	public SimCBRNOutputID(SimCBRNSensor parentSensor)
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "ID";
	}


	protected void init() {
		SWEHelper fac = new SWEHelper();

		// Build SWE Common record structure
		cbrnIDData = fac.newDataRecord(4);
		cbrnIDData.setName(getName());
//		cbrnIDData.setDefinition("http://sensorml.com/ont/swe/property/ToxicAgent");
//		cbrnIDData.setDescription("CBRN measurements");

		// Add fields
		cbrnIDData.addComponent("time", fac.newTimeStampIsoUTC());
		cbrnIDData.addComponent("host_id", fac.newCategory("http://sensorml.com/ont/swe/property/HostID", null, null, null));
		DataRecord poc_info = fac.newDataRecord(5);
		poc_info.setDefinition("http://sensorml.com/ont/swe/property/POC");
		poc_info.setLabel(null);
		poc_info.setDescription(null);
		poc_info.addField("poc_name", fac.newText("http://sensorml.com/ont/swe/property/Name", null, null));
		poc_info.addField("poc_org", fac.newText("http://sensorml.com/ont/swe/property/Organization", null, null));
		poc_info.addField("poc_phone", fac.newText("http://sensorml.com/ont/swe/property/Phone", null, null));
		poc_info.addField("poc_email", fac.newText("http://sensorml.com/ont/swe/property/Email", null, null));
		poc_info.addField("poc_address", fac.newText("http://sensorml.com/ont/swe/property/Address", null, null));
		cbrnIDData.addComponent("maint_poc", poc_info);

		cbrnEncoding = fac.newTextEncoding(",", "\n");

	}
	// will need to do some of the simulation here save for later
	private void sendMeasurement()
	{
		double time = System.currentTimeMillis()/1000;

		// Temperature sim (copied from FakeWeatherOutput)
		//temp += variation(temp, tempRef, 0.001, 0.1);

		// Build DataBlock
		DataBlock dataBlock = cbrnIDData.createDataBlock();
		dataBlock.setDoubleValue(0, time);
		dataBlock.setStringValue(1, hostID);
		dataBlock.setStringValue(2, mp_name);
		dataBlock.setStringValue(3, mp_org);
		dataBlock.setStringValue(4, mp_phone);
		dataBlock.setStringValue(5, mp_email);
		dataBlock.setStringValue(6, mp_address);


		//this method call is required to push data
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, SimCBRNOutputID.this, dataBlock));
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
		return cbrnIDData;
	}


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return cbrnEncoding;
	}
}
