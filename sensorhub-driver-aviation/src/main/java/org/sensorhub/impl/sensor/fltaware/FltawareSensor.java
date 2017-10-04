/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fltaware;

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
public class FltawareSensor extends AbstractSensorModule<FltawareConfig> implements IMultiSourceDataProducer, FlightObjectListener
{
	FlightPlanOutput flightPlanOutput;
	FlightPositionOutput flightPositionOutput;
	Thread watcherThread;
	private FltawareApi api;

	// Helpers
	SMLHelper smlFac = new SMLHelper();
	GMLFactory gmlFac = new GMLFactory(true);
	
	// Dynamically created FOIs
	Set<String> foiIDs;
	Map<String, AbstractFeature> aircraftFois;
	Map<String, AbstractFeature> aircraftDesc;
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:aviation:";
	static final String AIRCRAFT_UID_PREFIX = SENSOR_UID_PREFIX + "flightPlan:";

	static final Logger log = LoggerFactory.getLogger(FltawareSensor.class);
	
	public FltawareSensor() {
		this.foiIDs = new LinkedHashSet<String>();
		this.aircraftFois = new LinkedHashMap<String, AbstractFeature>();
		this.aircraftDesc = new LinkedHashMap<String, AbstractFeature>();
	}
	
	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("Delta FlightPlan networK");
		}
	}
	
	@Override
	public void init() throws SensorHubException
	{
		super.init();

//		api = new FltawareApi(config.userName, config.password);
		api = new FltawareApi("drgregswilson", "2809b6196a2cfafeb89db0a00b117ac67e876220");
		
		// IDs
		this.uniqueID = "urn:osh:sensor:earthcast:flightPlan";
		this.xmlID = "EarthcastFltaware";

		// Initialize Outputs
		this.flightPlanOutput = new FlightPlanOutput(this);

		addOutput(flightPlanOutput, false);
		flightPlanOutput.init();

		this.flightPositionOutput = new FlightPositionOutput(this);
		flightPositionOutput.init();
		addOutput(flightPositionOutput, false);
	}

	@Override
	public void start() throws SensorHubException
	{
		//  Push sample flight plan
//		FlightPlan plan = FlightPlan.getSamplePlan();
//		flightPlanOutput.sendFlightPlan(plan);
		
		// Start Firehose feed- 
        String machineName = "firehose.flightaware.com";
    	String userName = "drgregswilson";
    	String password = "2809b6196a2cfafeb89db0a00b117ac67e876220";
        FlightAwareClient client = new FlightAwareClient(machineName, userName, password);
        client.messageTypes.add("flightplan");
        client.addListener(this);
        Thread thread = new Thread(client);
        thread.start();
	}

	@Override
	public void stop() throws SensorHubException
	{

	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void processMessage(FlightObject obj) {

	}	

	private void addFoi(long recordTime, String flightId) {
		String uid = AIRCRAFT_UID_PREFIX + flightId;
		String description = "Delta Flight Plan for: " + flightId;

		// generate small SensorML for FOI (in this case the system is the FOI)
		PhysicalSystem foi = smlFac.newPhysicalSystem();
		foi.setId(flightId);
		foi.setUniqueIdentifier(uid);
//		System.err.println("MetarSensor station/uid: " + station + "/" + uid);
		foi.setName(flightId);
		foi.setDescription(description);

		foiIDs.add(uid);
		aircraftFois.put(flightId, foi);

        // send event
        long now = System.currentTimeMillis();
        eventHandler.publishEvent(new FoiEvent(now, flightId, this, foi, recordTime));
        
        log.debug("New vehicle added as FOI: {} ; aircraftFois.size = {}", uid, aircraftFois.size());
	}
	
	@Override
	public void processMessage(FlightObject obj, String message) {
		// call api and get flightplan
		// obj.id
		if(!obj.type.equals("flightplan")) {
			System.err.println("FlightAwareSensor does not yet support: " + obj.type);
			return;
		}

		try {
			FlightPlan plan = api.getFlightPlan(obj.id);
			if(plan == null) {
				return;
			}
//			plan.time = obj.getClock();  // Use pitr?
			plan.time = System.currentTimeMillis() / 1000;
//			System.err.println(message);
//			System.err.println(plan);
			if(plan != null) {
				// Check to see if existing entry with this flightId
				AbstractFeature foi = aircraftFois.get(plan.flightId);
				// should we replace if it is already there?  Shouldn't matter as long as data is changing
				if(foi == null)
					addFoi(plan.time, plan.flightId);
				flightPlanOutput.sendFlightPlan(plan);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	@Override
	public Collection<String> getEntityIDs()
	{
		return Collections.unmodifiableCollection(aircraftFois.keySet());
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
		return aircraftFois.get(entityID);
	}


	@Override
	public Collection<? extends AbstractFeature> getFeaturesOfInterest()
	{
		return Collections.unmodifiableCollection(aircraftFois.values());
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
