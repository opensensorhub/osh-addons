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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
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
 * @since Oct 1, 2017
 */
public class FlightAwareDriver extends AbstractSensorModule<FlightAwareConfig> implements IMultiSourceDataProducer,
	FlightPlanListener, PositionListener
{
    FlightPlanOutput flightPlanOutput;
	FlightPositionOutput flightPositionOutput;
	FlightAwareClient client;

	// Helpers
	SMLHelper smlFac = new SMLHelper();
	GMLFactory gmlFac = new GMLFactory(true);

	// Dynamically created FOIs
	Map<String, AbstractFeature> flightFois;
	Map<String, AbstractFeature> aircraftDesc;
	Map<String, FlightObject> flightPositions;
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:aviation:";
	static final String FLIGHT_UID_PREFIX = "urn:osh:aviation:flight:";
	
	static final Logger log = LoggerFactory.getLogger(FlightAwareDriver.class);

	public FlightAwareDriver() {
		this.flightFois = new ConcurrentSkipListMap<>();
		this.aircraftDesc = new ConcurrentHashMap<>();
		this.flightPositions = new ConcurrentHashMap<>();
	}
	
	
	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("Delta FlightAware feed");
		}
	}

	@Override
	public void init() throws SensorHubException
	{
		// IDs
		this.uniqueID = "urn:osh:sensor:earthcast:flightAware";
		this.xmlID = "Earthcast";

		// Initialize Outputs
		// FlightPlan
		this.flightPlanOutput = new FlightPlanOutput(this);
		addOutput(flightPlanOutput, false);
		flightPlanOutput.init();

		// FlightPosition
		this.flightPositionOutput = new FlightPositionOutput(this);
		addOutput(flightPositionOutput, false);
		flightPositionOutput.init();
	}

	@Override
	public void start() throws SensorHubException
	{
		// Configure Firehose feed- 
		String machineName = "firehose.flightaware.com";
		client = new FlightAwareClient(machineName, config.userName, config.password);
		for(String mt: config.messageTypes)
			client.addMessageType(mt);
		for(String airline: config.airlines)
			client.addAirline(airline);

		//  And message Converter
		FlightAwareConverter converter = new FlightAwareConverter() ;
		client.addListener(converter);
		converter.addPlanListener(this);
		converter.addPositionListener(this);

		// Start firehose feed
		Thread thread = new Thread(client);
		thread.start();
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

	private void ensureFlightFoi(String flightId, long recordTime) {
						
	    String uid = FLIGHT_UID_PREFIX + flightId;
	    
	    // skip if FOI already exists
		AbstractFeature fpFoi = flightFois.get(uid);
        if (fpFoi != null) 
            return;
        
		// generate small SensorML for FOI (in this case the system is the FOI)
		PhysicalSystem foi = smlFac.newPhysicalSystem();
		foi.setId(flightId);
		foi.setUniqueIdentifier(uid);
		foi.setName(flightId + " Flight");
		flightFois.put(uid, foi);

		// send event
		long now = System.currentTimeMillis();
		// TODO:  Check all recordTime and make sure in seconds and NOT ms!!!!
		eventHandler.publishEvent(new FoiEvent(now, flightId, this, foi, recordTime));

		log.trace("New FOI added: {}; Num FOIs = {}", uid, flightFois.size());
	}

	@Override
	public void newPosition(FlightObject pos) {
		//  Should never send null pos, but check it anyway
		if(pos == null) {
			return;
		}
		// Check for and add Pos and LawBox FOIs if they aren't already in cache
		String oshFlightId = pos.getOshFlightId();
		ensureFlightFoi(oshFlightId, pos.getClock());
		FlightObject prevPos = flightPositions.get(oshFlightId);
		if(prevPos != null) {
			// Calc vert change in ft/minute
			Long prevTime = prevPos.getClock() ;
			Long newTime = pos.getClock() ;
			Double prevAlt = prevPos.getAltitude();
			Double newAlt = pos.getAltitude();
//			System.err.println(" ??? " + oshFlightId + ":" + prevAlt + "," + newAlt + "," + prevTime + "," + newTime);
			if(prevAlt != null && newAlt != null && prevTime != null && newTime != null && (!prevTime.equals(newTime)) ) {
				// check math here!!!
				pos.verticalChange = (newAlt - prevAlt)/( (newTime - prevTime)/60.);
//				System.err.println(" ***  " + oshFlightId + ":" + prevAlt + "," + newAlt + "," + prevTime + "," + newTime + " ==> " + pos.verticalChange);
			}
		}
		flightPositions.put(oshFlightId, pos);
		flightPositionOutput.sendPosition(pos, oshFlightId);
	}

	@Override
	public void newFlightPlan(FlightPlan plan) {
		//  Should never send null plan
		if(plan == null) {
			return;
		}
		// Add new FlightPlan FOI if new
		String oshFlightId = plan.getOshFlightId();
		ensureFlightFoi(oshFlightId, System.currentTimeMillis()/1000);

		// send new data to outputs
		flightPlanOutput.sendFlightPlan(plan);
	}
	
	@Override
	public Collection<String> getEntityIDs()
	{
		return Collections.unmodifiableCollection(flightFois.keySet());
	}


	@Override
	public AbstractFeature getCurrentFeatureOfInterest()
	{
		return null;
	}


	@Override
	public AbstractProcess getCurrentDescription(String entityID)
	{
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
		return flightFois.get(entityID);
	}


	@Override
	public Collection<? extends AbstractFeature> getFeaturesOfInterest()
	{
		return Collections.unmodifiableCollection(flightFois.values());
	}


	@Override
	public Collection<String> getFeaturesOfInterestIDs()
	{
		return Collections.unmodifiableCollection(flightFois.keySet());
	}
	

	@Override
	public Collection<String> getEntitiesWithFoi(String foiID)
	{
	    String entityID = foiID.substring(foiID.lastIndexOf(':')+1);
        return Arrays.asList(entityID);
	}
}
