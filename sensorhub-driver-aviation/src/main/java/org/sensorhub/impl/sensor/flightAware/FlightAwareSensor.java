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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.mesh.DirectoryWatcher;
import org.sensorhub.impl.sensor.mesh.FileListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;
import ucar.ma2.InvalidRangeException;

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
public class FlightAwareSensor extends AbstractSensorModule<FlightAwareConfig> implements IMultiSourceDataProducer,
	FlightPlanListener, PositionListener, FileListener
{
	FlightPlanOutput flightPlanOutput;
	FlightPositionOutput flightPositionOutput;
	TurbulenceOutput turbulenceOutput;
	LawBoxOutput lawBoxOutput;
	FlightAwareClient client;

	// Helpers
	SMLHelper smlFac = new SMLHelper();
	GMLFactory gmlFac = new GMLFactory(true);

	// Dynamically created FOIs
	Set<String> foiIDs;
	Map<String, AbstractFeature> flightAwareFois;
	Map<String, AbstractFeature> aircraftDesc;
	Map<String, FlightObject> flightPositions;
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:aviation:";
	static final String FLIGHT_PLAN_UID_PREFIX = SENSOR_UID_PREFIX + "flightPlan:";
	static final String FLIGHT_POSITION_UID_PREFIX = SENSOR_UID_PREFIX + "flightPosition:";
	static final String TURBULENCE_UID_PREFIX = SENSOR_UID_PREFIX + "turbulence:";
	static final String LAWBOX_UID_PREFIX = SENSOR_UID_PREFIX + "lawBox:";

	// Listen for and ingest new Turbulence files so we can populate both 
	// TurbulenceOutput and LawBoxOutput 
	private TurbulenceReader turbReader;
	Thread watcherThread;
	private DirectoryWatcher watcher;

	static final Logger log = LoggerFactory.getLogger(FlightAwareSensor.class);

	public FlightAwareSensor() {
		this.foiIDs = new LinkedHashSet<String>();
		this.flightAwareFois = new LinkedHashMap<String, AbstractFeature>();
		this.aircraftDesc = new LinkedHashMap<String, AbstractFeature>();
		this.flightPositions = new LinkedHashMap<String, FlightObject>();
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

		// FlightPosition
		this.flightPositionOutput = new FlightPositionOutput(this);
		addOutput(flightPositionOutput, false);
		flightPositionOutput.init();

		// Turbulence
		this.turbulenceOutput= new TurbulenceOutput(this);
		addOutput(turbulenceOutput, false);
		turbulenceOutput.init();

		// LawBox
		this.lawBoxOutput= new LawBoxOutput(this);
		addOutput(lawBoxOutput, false);
		lawBoxOutput.init();
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
		startTurbulenceListener();
	}

	private void startTurbulenceListener() throws SensorHubException {
		try {
			watcher = new DirectoryWatcher(Paths.get(config.turbulencePath), StandardWatchEventKinds.ENTRY_CREATE);
			watcherThread = new Thread(watcher);
			watcher.addListener(this);
			watcherThread.start();
		} catch (IOException e) {
			e.printStackTrace();
			throw new SensorHubException("TurbulenceSensor could not create DirectoryWatcher...", e);
		}
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

	private void addLawBoxFoi(String flightId, long recordTime) {
		AbstractFeature lawboxFoi = flightAwareFois.get(LAWBOX_UID_PREFIX + flightId);
		if(lawboxFoi != null)
			return ; // This position already has an FOI
		String uid = LAWBOX_UID_PREFIX + flightId;
		String description = "Delta LawBox data for: " + flightId;

		// generate small SensorML for FOI
		PhysicalSystem foi = smlFac.newPhysicalSystem();
		foi.setId(flightId);
		foi.setUniqueIdentifier(uid);
		foi.setName(flightId + " LawBox");
		foi.setDescription(description);

		foiIDs.add(uid);
		flightAwareFois.put(uid, foi);

		// send event
		long now = System.currentTimeMillis();
		eventHandler.publishEvent(new FoiEvent(now, flightId, this, foi, recordTime));

		log.debug("New LawBox added as FOI: {} ; flightAwareFois.size = {}", uid, flightAwareFois.size());
	}

	@Override
	public void newPosition(FlightObject pos) {
		//  Should never send null pos, but check it anyway
		if(pos == null) {
			return;
		}
		// Check for and add Pos and LawBox FOIs if they aren't already in cache
		String oshFlightId = pos.getOshFlightId();
		addPositionFoi(oshFlightId, pos.getClock());
		addLawBoxFoi(oshFlightId, pos.getClock());
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

	public List<TurbulenceRecord> getTurbulence(FlightPlan plan) throws IOException, InvalidRangeException {
		if(turbReader == null) {
			log.debug("FlightAwareSensor turbulenceReader is null.  No data yet.");
			return new ArrayList<TurbulenceRecord>();
		}
		return turbReader.getTurbulence(plan);
	}
	
	public LawBox getLawBox(String flightUid) throws IOException {
		// Need FlightPos
		FlightObject pos = flightPositions.get(flightUid);
		if(pos == null) {
			log.debug("FlightPosition not available in getLawBox() for: {}", flightUid);
			return null;
		}
		LawBox lawBox = turbReader.getLawBox(pos);
		return lawBox;
	}
		
	/**
	 * Wheenever we get a new turb file, load the whole thing into memory for now
	 */
	@Override
	public void newFile(Path p) throws IOException {
		String fn = p.getFileName().toString().toLowerCase();
		if(!fn.contains("gtgturb") || !fn.endsWith(".grb2")) {
			return;
		}

		//  adding short delay for all platforms now- windows was consistently 
		//  trying to open the file after creation but before it was fully copied.
		try {
			Thread.sleep(200L);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		// Account for failure of new TurbReader
		try {
			turbReader = new TurbulenceReader(p.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
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
