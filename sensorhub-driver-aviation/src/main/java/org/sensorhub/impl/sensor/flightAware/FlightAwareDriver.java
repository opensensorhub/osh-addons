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

import java.io.File;
import java.io.FilenameFilter;
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
import java.util.TimerTask;
import java.util.regex.Pattern;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.mesh.DirectoryWatcher;
import org.sensorhub.impl.sensor.mesh.FileListener;
import org.sensorhub.impl.sensor.navDb.LufthansaParser;
import org.sensorhub.impl.sensor.navDb.NavDbEntry;
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
 * @since Oct 1, 2017
 * 
 * TODO:   
 *		Separate FlightPlan and Turbulence- leaving most of the code in for now until 
 *      I get a chance to clean it up
 */
public class FlightAwareDriver extends AbstractSensorModule<FlightAwareConfig> implements IMultiSourceDataProducer,
	FlightPlanListener, PositionListener, FileListener
{
    static final Pattern DATA_FILE_REGEX = Pattern.compile(".*GTGTURB.*grb2");
    
    FlightPlanOutput flightPlanOutput;
	FlightPositionOutput flightPositionOutput;
//	TurbulenceOutput turbulenceOutput;
	LawBoxOutput lawBoxOutput;
	FlightAwareClient client;
	FlightAwareClientMonitor clientMonitor;

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

	// Listen for and ingest new Turbulence files so we can populate LawBoxOutput 
	private TurbulenceReader turbReader;
	Thread watcherThread;
	private DirectoryWatcher watcher;

	// Adding Airports- needed for computing LawBox when position is close to an airport 
	// Should pull these from storage.  
	Map<String, NavDbEntry> airportMap;
	
	static final Logger log = LoggerFactory.getLogger(FlightAwareDriver.class);

	public FlightAwareDriver() {
		this.foiIDs = new LinkedHashSet<String>();
		this.flightAwareFois = Collections.synchronizedMap(new LinkedHashMap<String, AbstractFeature>());
		this.aircraftDesc = Collections.synchronizedMap(new LinkedHashMap<String, AbstractFeature>());
		this.flightPositions = Collections.synchronizedMap(new LinkedHashMap<String, FlightObject>());
		this.airportMap = Collections.synchronizedMap(new LinkedHashMap<String, NavDbEntry>());
	}

	private void loadAirports() {
		 try {
			List<NavDbEntry> airports = LufthansaParser.getDeltaAirports(config.navDbPath, config.deltaAirportsPath);
			for(NavDbEntry a: airports) {
				airportMap.put(a.icao, a);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.debug(e.getMessage());
		}
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
		for(String mt: config.messageTypes)
			client.messageTypes.add(mt);

		//  And message Converter
		FlightAwareConverter converter = new FlightAwareConverter() ;
		client.addListener(converter);
		converter.addPlanListener(this);
		converter.addPositionListener(this);

		//  Load airportsMap so we can look up airport locations for LawBox
		loadAirports();

		// Start firehose feed
		Thread thread = new Thread(client);
		thread.start();
		
		clientMonitor = new FlightAwareClientMonitor(client);

		startDirectoryWatcher();
		readLatestDataFile();
	}
    
    private void readLatestDataFile() throws SensorHubException
    {
        // list all available GTGTURB data files
        File dir = new File(config.turbulencePath);
        File[] turbFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return DATA_FILE_REGEX.matcher(name).matches();
            }
        });        
        
        // skip if nothing is available
        if (turbFiles.length == 0)
        {
            getLogger().warn("No turbulence data file available");
            return;
        }
        
        // get the one with latest time stamp
        File latestFile = turbFiles[0];
        for (File f: turbFiles)
        {
            if (f.lastModified() > latestFile.lastModified())
                latestFile = f;
        }
        
        // trigger reader
        newFile(latestFile.toPath());
    }

    private void startDirectoryWatcher() throws SensorHubException
    {
        try
        {
            watcher = new DirectoryWatcher(Paths.get(config.turbulencePath), StandardWatchEventKinds.ENTRY_CREATE);
            watcherThread = new Thread(watcher);
            watcher.addListener(this);
            watcherThread.start();
            getLogger().info("Watching directory {} for data updates", config.turbulencePath);
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error creating directory watcher on " + config.turbulencePath, e);
        }
    }

	@Override
	public void stop() throws SensorHubException
	{
		if(clientMonitor != null)
			clientMonitor.cancel();
		
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
		// TODO:  Check all recordTime and make sure in seconds and NOT ms!!!!
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

		log.trace("New TurbulenceProfile added as FOI: {} ; flightAwareFois.size = {}", uid, flightAwareFois.size());
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

		log.trace("New LawBox added as FOI: {} ; flightAwareFois.size = {}", uid, flightAwareFois.size());
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
//		turbulenceOutput.addFlightPlan(TURBULENCE_UID_PREFIX + plan.oshFlightId, plan);
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
		NavDbEntry orig = null;
		NavDbEntry dest = null;
		if(pos.orig != null)
			orig = airportMap.get(pos.orig);
		if(pos.dest != null)
			dest = airportMap.get(pos.dest);
		
		// Probably a better way to handle this- orig and dest airports have to be 
		//  passed down a couple of classes to do the computation. 
		LawBox lawBox = turbReader.getLawBox(pos, orig, dest);
		return lawBox;
	}

    /*
     * called whenever we get a new turb file
     */
    @Override
    public void newFile(Path p)
    {
        // only continue when it's a new turbulence GRIB file
        if (!DATA_FILE_REGEX.matcher(p.getFileName().toString()).matches())
            return;
        
        //  adding short delay for all platforms now- windows was consistently 
        //  trying to open the file after creation but before it was fully copied.
        try
        {
            Thread.sleep(1000L);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        
        // try to read file with TurbReader
        try
        {
            getLogger().info("Loading new turbulence data file: {}", p);
            
            // load the whole thing into memory for now
            turbReader = new TurbulenceReader(p.toString());
        }
        catch (Exception e)
        {
            getLogger().error("Error reading turbulence data file: {}", p, e);
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
