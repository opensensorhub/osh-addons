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

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.vast.swe.SWEHelper;

public class SimCBRNOutputReadings extends AbstractSensorOutput<SimCBRNSensor>
{
	DataComponent cbrnReadingData;
	DataEncoding cbrnEncoding;
	Timer timer;
	Random rand = new Random();

	// Reference values used as a basis for building randomized vals
	double tempRef = 20.0;

	// "Sensor" variables (what gets output as the sensor data
	double temp = tempRef;
	String agentClassStatus = "G_Agent";
	String agentIDStatus = "GA";
	int numericalLevel = 0;
	String units = "BARS";
	String stringLevel = "NONE";


	public SimCBRNOutputReadings(SimCBRNSensor parentSensor)
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "READGS";
	}


	protected void init()
	{
		SWEHelper fac = new SWEHelper();

		// Build SWE Common record structure
		cbrnReadingData = fac.newDataRecord(7);
		cbrnReadingData.setName(getName());
//		cbrnReadingData.setDefinition("http://sensorml.com/ont/swe/property/ToxicAgent");
//		cbrnReadingData.setDescription("CBRN measurements");

		// Add fields
		cbrnReadingData.addComponent("time", fac.newTimeStampIsoUTC());

		// Agent Classes
		Category reading_AgentClass = fac.newCategory("http://sensorml.com/ont/swe/property/ChemicalAgentClass", null, null,null );
		AllowedTokens allowedAgentClasses = fac.newAllowedTokens();
		allowedAgentClasses.addValue("G_Agent");
		allowedAgentClasses.addValue("H_Agent");
		allowedAgentClasses.addValue("V_Agent");
		allowedAgentClasses.addValue("BloodTIC");
		reading_AgentClass.setConstraint(allowedAgentClasses);
		cbrnReadingData.addComponent("agent_class", reading_AgentClass);

		// Agent IDs (Specific Identification codes for Toxic Agents)
		Category reading_AgentID = fac.newCategory("http://sensorml.com/ont/swe/property/ChemicalAgentID", null, null, null);
		AllowedTokens allowedAgentIDs = fac.newAllowedTokens();
		allowedAgentIDs.addValue("GA");
		allowedAgentIDs.addValue("GB");
		allowedAgentIDs.addValue("GD");
		allowedAgentIDs.addValue("VX");
		allowedAgentIDs.addValue("HN");
		allowedAgentIDs.addValue("HD");
		allowedAgentIDs.addValue("L");
		reading_AgentID.setConstraint(allowedAgentIDs);
		cbrnReadingData.addComponent("agent_ID", reading_AgentID);

		// Alert Levels
		Quantity reading_Level = fac.newQuantity("http://sensorml.com/ont/swe/property/Level", null, null,"http://www.opengis.net/def/uom/0/instrument_BAR");
		AllowedValues levelValue = fac.newAllowedValues();
		levelValue.addInterval(new double[] {0,6});
		reading_Level.setConstraint(levelValue);
		cbrnReadingData.addComponent("alert_level", reading_Level);

		// Alert Units (of measurement)
		Category reading_Units = fac.newCategory("http://sensorml.com/ont/swe/property/UnitOfMeasure", null, null, null);
		AllowedTokens unitToken = fac.newAllowedTokens();
		unitToken.addValue("BARS");
		reading_Units.setConstraint(unitToken);
		cbrnReadingData.addComponent("alert_units", reading_Units);

		// Hazard Level (severity)
		Category reading_HazardLevel = fac.newCategory("http://sensorml.com/ont/swe/property/HazardLevel",null, null, null);
		AllowedTokens  hazardLevels = fac.newAllowedTokens();
		hazardLevels.addValue("None");
		hazardLevels.addValue("Medium");
		hazardLevels.addValue("High");
		reading_HazardLevel.setConstraint(hazardLevels);
		cbrnReadingData.addComponent("hazard_level", reading_HazardLevel);

		// Temperature
		cbrnReadingData.addComponent("temp", fac.newQuantity("http://sensorml.com/ont/swe/property/Temperature", null, null, "Cel"));

		cbrnEncoding = fac.newTextEncoding(",", "\n");
	}


	// will need to do some of the simulation here save for later
	private void sendMeasurement()
	{
		/*getParentModule().simData.update(getParentModule().getConfiguration());
		double time = System.currentTimeMillis()/1000;

		// Temperature sim (copied from FakeWeatherOutput)
		temp += variation(temp, tempRef, 0.001, 0.1);
		agentClassStatus = getParentModule().simData.getDetectedAgent().getAgentClass();
		agentIDStatus = getParentModule().simData.getDetectedAgent().getAgentID();
		numericalLevel = getParentModule().simData.findThreatLevel();
		stringLevel = getParentModule().simData.findThreatString();*/

		// Build DataBlock
		DataBlock dataBlock = cbrnReadingData.createDataBlock();
		dataBlock.setDoubleValue(0, time);
		dataBlock.setStringValue(1, agentClassStatus);
		dataBlock.setStringValue(2, agentIDStatus);
		dataBlock.setIntValue(3, numericalLevel);
		dataBlock.setStringValue(4, units);
		dataBlock.setStringValue(5, stringLevel);
		dataBlock.setDoubleValue(6, temp);

		//this method call is required to push data
		latestRecord = dataBlock;
		latestRecordTime = System.currentTimeMillis();
		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, SimCBRNOutputReadings.this, dataBlock));
	}


	private double variation(double val, double ref, double dampingCoef, double noiseSigma)
	{
		return -dampingCoef*(val - ref) + noiseSigma*rand.nextGaussian();
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
		return cbrnReadingData;
	}


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return cbrnEncoding;
	}
}
