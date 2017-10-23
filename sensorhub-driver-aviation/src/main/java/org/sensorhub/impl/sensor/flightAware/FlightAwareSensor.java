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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
 * TODO:   
 * java.util.ConcurrentModificationException: null
        at java.util.LinkedHashMap$LinkedHashIterator.nextNode(LinkedHashMap.java:719) ~[na:1.8.0_131]
        at java.util.LinkedHashMap$LinkedValueIterator.next(LinkedHashMap.java:747) ~[na:1.8.0_131]
        at java.util.Collections$UnmodifiableCollection$1.next(Collections.java:1042) ~[na:1.8.0_131]
        at org.sensorhub.impl.service.sos.FoiUtils.updateFois(FoiUtils.java:51) ~[s
 *
 */
public class FlightAwareSensor extends AbstractSensorModule<FlightAwareConfig> implements IMultiSourceDataProducer, // FlightObjectListener //, FlightPlanListener
	FlightPlanListener, PositionListener
{
	FlightPlanOutput flightPlanOutput;
	FlightPositionOutput flightPositionOutput;
	TurbulenceOutput turbulenceOutput;
	Thread watcherThread;
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
		// IDs
		this.uniqueID = "urn:osh:sensor:earthcast:flightAware";
		this.xmlID = "EarthcastFltaware";

		// Initialize Outputs
		// FlightPlan
		this.flightPlanOutput = new FlightPlanOutput(this);

		addOutput(flightPlanOutput, false);

		flightPlanOutput.init();

		//		FlightPosition
		this.flightPositionOutput = new FlightPositionOutput(this);
		flightPositionOutput.init();
		addOutput(flightPositionOutput, false);

		// Turbulence
		this.turbulenceOutput= new TurbulenceOutput(this);

		addOutput(turbulenceOutput, false);
		turbulenceOutput.init();
	}

	@Override
	public void start() throws SensorHubException
	{
		// Configure Firehose feed- 
		String machineName = "firehose.flightaware.com";
		client = new FlightAwareClient(machineName, config.userName, config.password);
		client.messageTypes.add("flightplan");
		client.messageTypes.add("position");
		
		//  And message Converter
		FlightAwareConverter converter = new FlightAwareConverter() ;
		client.addListener(converter);
		converter.addPlanListener(this);
		converter.addPositionListener(this);
		
		// Start firehose feed
		Thread thread = new Thread(client);
		thread.start();

		//  start Turbulence output, which will start FileListener for GTGTURB files
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
		foi.setName(flightId + " FlightPlan");
		foi.setDescription(description);

		foiIDs.add(uid);
		flightAwareFois.put(uid, foi);

		// send event
		long now = System.currentTimeMillis();
		eventHandler.publishEvent(new FoiEvent(now, flightId, this, foi, recordTime));

		log.debug("New FlightPlan added as FOI: {} ; aircraftFois.size = {}", uid, flightAwareFois.size());
	}

	protected void addPositionFoi(String flightId, long recordTime) {
		AbstractFeature posFoi = flightAwareFois.get(FLIGHT_POSITION_UID_PREFIX + flightId);
		if(posFoi != null)
			return ; // This position already has an FOI
		String uid = FLIGHT_POSITION_UID_PREFIX + flightId;
		String description = "Delta FlightPosition data for: " + flightId;

		// generate small SensorML for FOI
		PhysicalSystem foi = smlFac.newPhysicalSystem();
		foi.setId(flightId);
		foi.setUniqueIdentifier(uid);
		foi.setName(flightId + " Position");
		foi.setDescription(description);

		foiIDs.add(uid);
		flightAwareFois.put(uid, foi);

		// send event
		long now = System.currentTimeMillis();
		//  Should be good to send positionTime as startTime of FoiEvent since...
	    // @param startTime time at which observation of the FoI started (julian time in seconds, base 1970)
		eventHandler.publishEvent(new FoiEvent(now, flightId, this, foi, recordTime));

		log.debug("New Position added as FOI: {} ; flightAwareFois.size = {}", uid, flightAwareFois.size());
	}
	
	private void addTurbulenceFoi(String flightId, long recordTime) {
		String uid = TURBULENCE_UID_PREFIX + flightId;
		String description = "Delta Turbulence data for: " + flightId;

		// generate small SensorML for FOI
		PhysicalSystem foi = smlFac.newPhysicalSystem();
		foi.setId(flightId);
		foi.setUniqueIdentifier(uid);
		foi.setName(flightId + " Turbulence");
		foi.setDescription(description);

		foiIDs.add(uid);
		flightAwareFois.put(uid, foi);

		// send event
		long now = System.currentTimeMillis();
		eventHandler.publishEvent(new FoiEvent(now, flightId, this, foi, recordTime));

		log.debug("New TurbulenceProfile added as FOI: {} ; flightAwareFois.size = {}", uid, flightAwareFois.size());
	}

	@Override
	public void newPosition(FlightObject pos) {
		//  Should never send null plan
		if(pos == null) {
			return;
		}
		addPositionFoi(pos.getOshFlightId(), pos.getClock());
		flightPositionOutput.sendPosition(pos, pos.getOshFlightId());
	}

	@Override
	public void newFlightPlan(FlightPlan plan) {
		//  Should never send null plan
		if(plan == null) {
			return;
		}
		// Add new FlightPlan FOI if new
		String oshFlightId = plan.getOshFlightId();
		AbstractFeature fpFoi = flightAwareFois.get(FLIGHT_PLAN_UID_PREFIX + oshFlightId);
		if(fpFoi == null) 
			addFlightPlanFoi(oshFlightId, System.currentTimeMillis()/1000);

		//  And Turbulence FOI if new
		AbstractFeature turbFoi = flightAwareFois.get(TURBULENCE_UID_PREFIX + oshFlightId);
		if(turbFoi == null)
			addTurbulenceFoi(oshFlightId, System.currentTimeMillis()/1000) ;

		// send new data to outputs
		flightPlanOutput.sendFlightPlan(plan);
		turbulenceOutput.addFlightPlan(TURBULENCE_UID_PREFIX + plan.oshFlightId, plan);
	}
	
//	@Override
//	public void processMessage(FlightObject obj) {
//		// call api and get flightplan
//		if(!obj.type.equals("flightplan") && !obj.type.equals("position") ) {
//			log.warn("FlightAwareSensor does not yet support: " + obj.type);
//			return;
//		}
//
//		switch(obj.type) {
//		case "flightplan":
//			processFlightPlan(obj);
//			break;
//		case "position":
//			processPosition(obj);
//			break;
//		default:
//			log.error("Unknown message slipped through somehow: " + obj.type);
//		}
//	}

	// One issue here is FOI gets added even if Processing plan fails
	private void processFlightPlan(FlightObject obj) {
		// Check to see if existing FlightPlan entry with this flightId
		String oshFlightId = obj.getOshFlightId();
		AbstractFeature fpFoi = flightAwareFois.get(FLIGHT_PLAN_UID_PREFIX + oshFlightId);
		// add flightPlan FOI if new
		if(fpFoi == null) {
			addFlightPlanFoi(oshFlightId, System.currentTimeMillis()/1000);
		}

		//  And Turbulence FOI if new
		AbstractFeature turbFoi = flightAwareFois.get(TURBULENCE_UID_PREFIX + oshFlightId);
		if(turbFoi == null)
			addTurbulenceFoi(oshFlightId, System.currentTimeMillis()/1000) ;

		// kick off processing thread
		Thread thread = new Thread(new ProcessPlanThreadFAS(obj, flightPlanOutput, turbulenceOutput, TURBULENCE_UID_PREFIX ));
		thread.start();
	}

	private void processPosition(FlightObject obj) {
		// Just kick off the thread- it takes care of adding FOI if new position
		Thread thread = new Thread(new ProcessPositionThreadFAS(obj, flightPositionOutput, FLIGHT_POSITION_UID_PREFIX ));
		thread.start();
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
		// TODO
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
