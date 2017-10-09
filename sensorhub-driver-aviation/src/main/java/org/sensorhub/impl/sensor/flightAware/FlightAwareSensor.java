/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;

/**
 * 
 * @author tcook
 * @since oCT 1, 2017
 * 
 *
 */
public class FlightAwareSensor extends AbstractSensorModule<FlightAwareConfig> implements IMultiSourceDataProducer, FlightObjectListener //, FlightPlanListener
{
	FlightPlanOutput flightPlanOutput;
	FlightPositionOutput flightPositionOutput;
	TurbulenceOutput turbulenceOutput;
	Thread watcherThread;
	private FlightAwareApi api;
	FlightAwareClient client;

	// Helpers
	SMLHelper smlFac = new SMLHelper();
	GMLFactory gmlFac = new GMLFactory(true);

	// Dynamically created FOIs
	Set<String> foiIDs;
	Map<String, AbstractFeature> flightAwareFois;
	Map<String, AbstractFeature> aircraftDesc;
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:aviation:";
	static final String FLIGHT_PLAN_UID_PREFIX = SENSOR_UID_PREFIX + "flightPlan:";
	static final String FLIGHT_POSITION_UID_PREFIX = SENSOR_UID_PREFIX + "flightPosition:";
	static final String TURBULENCE_UID_PREFIX = SENSOR_UID_PREFIX + "turbulence:";

	static final Logger log = LoggerFactory.getLogger(FlightAwareSensor.class);
	private BufferedWriter writer;

	public FlightAwareSensor() {
		this.foiIDs = new LinkedHashSet<String>();
		this.flightAwareFois = new LinkedHashMap<String, AbstractFeature>();
		this.aircraftDesc = new LinkedHashMap<String, AbstractFeature>();
	}

	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("Delta FlightPlan network");
		}
	}

	@Override
	public void init() throws SensorHubException
	{
		super.init();

		//		api = new FltawareApi(config.userName, config.password);
		api = new FlightAwareApi("drgregswilson", "2809b6196a2cfafeb89db0a00b117ac67e876220");

		// IDs
		this.uniqueID = "urn:osh:sensor:earthcast:flightAware";
		this.xmlID = "EarthcastFltaware";

		// Initialize Outputs
		// FlightPlan
		this.flightPlanOutput = new FlightPlanOutput(this);
		addOutput(flightPlanOutput, false);
		flightPlanOutput.init();

		//  FlightPosition
		//		this.flightPositionOutput = new FlightPositionOutput(this);
		//		flightPositionOutput.init();
		//		addOutput(flightPositionOutput, false);

		// Turbulence
		this.turbulenceOutput= new TurbulenceOutput(this);
		addOutput(turbulenceOutput, false);
		turbulenceOutput.init();

		//  Temp so I can collect flightIds
		try {
			writer = new BufferedWriter(new FileWriter("C:/Data/sensorhub/delta/flights.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void start() throws SensorHubException
	{
		// Start Firehose feed- 
		String machineName = "firehose.flightaware.com";
		// config me!
		String userName = "drgregswilson";
		String password = "2809b6196a2cfafeb89db0a00b117ac67e876220";
		client = new FlightAwareClient(machineName, userName, password);
		client.messageTypes.add("flightplan");
		client.addListener(this);
		Thread thread = new Thread(client);
		thread.start();

		//  start Turbulence output, which will start FileListener
		turbulenceOutput.start(config.turbulencePath);
	}

	@Override
	public void stop() throws SensorHubException
	{
		if (client != null)
			client.stop();
		client = null;
	}

	@Override
	public boolean isConnected() {
		return false;
	}

	private void addFlightPlanFoi(String flightId, long recordTime) {
		String uid = FLIGHT_PLAN_UID_PREFIX + flightId;
		String description = "Delta Flight Plan for: " + flightId;

		// generate small SensorML for FOI (in this case the system is the FOI)
		PhysicalSystem foi = smlFac.newPhysicalSystem();
		foi.setId(flightId);
		foi.setUniqueIdentifier(uid);
		//		System.err.println("MetarSensor station/uid: " + station + "/" + uid);
		foi.setName(flightId + " FlightPlan");
		foi.setDescription(description);

		foiIDs.add(uid);
		flightAwareFois.put(uid, foi);

		// send event
		long now = System.currentTimeMillis();
		eventHandler.publishEvent(new FoiEvent(now, flightId, this, foi, recordTime));

//		log.debug("New FlightPlan added as FOI: {} ; aircraftFois.size = {}", uid, flightAwareFois.size());
		try {
			writer.write(flightId + "\n");
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void addPositionFoi(String flightId, long recordTime) {
		String uid = FLIGHT_POSITION_UID_PREFIX + flightId;
		String description = "Delta FlightPosition data for: " + flightId;

		// generate small SensorML for FOI (in this case the system is the FOI)
		PhysicalSystem foi = smlFac.newPhysicalSystem();
		foi.setId(flightId);
		foi.setUniqueIdentifier(uid);
		//		System.err.println("MetarSensor station/uid: " + station + "/" + uid);
		foi.setName(flightId + " Position");
		foi.setDescription(description);

		foiIDs.add(uid);
		flightAwareFois.put(uid, foi);

		// send event
		long now = System.currentTimeMillis();
		eventHandler.publishEvent(new FoiEvent(now, flightId, this, foi, recordTime));

		log.debug("New Position added as FOI: {} ; flightAwareFois.size = {}", uid, flightAwareFois.size());
	}

	private void addTurbulenceFoi(String flightId, long recordTime) {
		String uid = TURBULENCE_UID_PREFIX + flightId;
		String description = "Delta Turbulence data for: " + flightId;

		// generate small SensorML for FOI (in this case the system is the FOI)
		PhysicalSystem foi = smlFac.newPhysicalSystem();
		foi.setId(flightId);
		foi.setUniqueIdentifier(uid);
		//		System.err.println("MetarSensor station/uid: " + station + "/" + uid);
		foi.setName(flightId + " Turbulence");
		foi.setDescription(description);

		foiIDs.add(uid);
		flightAwareFois.put(uid, foi);

		// send event
		long now = System.currentTimeMillis();
		eventHandler.publishEvent(new FoiEvent(now, flightId, this, foi, recordTime));

//		log.debug("New TurbulenceProfile added as FOI: {} ; flightAwareFois.size = {}", uid, flightAwareFois.size());
	}

	@Override
	public void processMessage(FlightObject obj) {
		// call api and get flightplan
		// obj.id
		if(!obj.type.equals("flightplan") && !obj.type.equals("position") ) {
			System.err.println("FlightAwareSensor does not yet support: " + obj.type);
			return;
		}

		switch(obj.type) {
		case "flightplan":
			processFlightPlan(obj);
			break;
		case "position":
			processPosition(obj);
			break;
		default:
			logger.error("Unknown message slipped through somehow: " + obj.type);
		}
	}

	private void processFlightPlan(FlightObject obj) {
		try {
			FlightPlan plan = api.getFlightPlan(obj.id);
			if(plan == null) {
				return;
			}
			//			plan.time = obj.getClock();  // Use pitr?
			plan.time = System.currentTimeMillis() / 1000;
			//			System.err.println(plan);
			if(plan != null) {
				// Check to see if existing FlightPlan entry with this flightId
				AbstractFeature fpFoi = flightAwareFois.get(FLIGHT_PLAN_UID_PREFIX + plan.oshFlightId);
				// should we replace if it is already there?  Shouldn't matter as long as data is changing
				if(fpFoi == null)
					addFlightPlanFoi(plan.oshFlightId, plan.time);
				flightPlanOutput.sendFlightPlan(plan);

				//  And Turbulence- only adding FOI
				AbstractFeature turbFoi = flightAwareFois.get(TURBULENCE_UID_PREFIX + plan.oshFlightId);
				if(turbFoi == null)
					addTurbulenceFoi(TURBULENCE_UID_PREFIX + plan.oshFlightId, plan.time);
				turbulenceOutput.addFlightPlan(TURBULENCE_UID_PREFIX + plan.oshFlightId, plan);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 	
	}

	private void processPosition(FlightObject obj) {
		// Check to see if existing FlightPlan entry with this flightId
		if(obj.ident == null || obj.dest == null || obj.ident.length() == 0 || obj.dest.length() == 0) {
			logger.error("Cannot construct oshFlightId. Missing ident or dest in FlightObject");
			return;
		}

		String oshFlightId = obj.getOshFlightId();
		AbstractFeature posFoi = flightAwareFois.get(FLIGHT_POSITION_UID_PREFIX + oshFlightId);
		// should we replace if it is already there?  Shouldn't matter as long as data is changing
		if(posFoi == null)
			addPositionFoi(oshFlightId, obj.getClock());
		flightPositionOutput.sendPosition(obj, oshFlightId);
	}

	@Override
	public Collection<String> getEntityIDs()
	{
		return Collections.unmodifiableCollection(flightAwareFois.keySet());
	}


	@Override
	public AbstractFeature getCurrentFeatureOfInterest()
	{
		return null;
	}


	@Override
	public AbstractProcess getCurrentDescription(String entityID)
	{
		//		return aircraftDesc.get(entityID);
		return null;
	}


	@Override
	public double getLastDescriptionUpdate(String entityID)
	{
		return 0;
	}


	@Override
	public AbstractFeature getCurrentFeatureOfInterest(String entityID)
	{
		return flightAwareFois.get(entityID);
	}


	@Override
	public Collection<? extends AbstractFeature> getFeaturesOfInterest()
	{
		return Collections.unmodifiableCollection(flightAwareFois.values());
	}


	@Override
	public Collection<String> getFeaturesOfInterestIDs()
	{
		return Collections.unmodifiableCollection(foiIDs);
	}

	@Override
	public Collection<String> getEntitiesWithFoi(String foiID)
	{
		return Arrays.asList(foiID);
	}
}
