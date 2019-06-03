/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.navDb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.navDb.NavDbEntry.Type;
import org.sensorhub.impl.utils.grid.DirectoryWatcher;
import org.sensorhub.impl.utils.grid.FileListener;
import org.vast.sensorML.SMLHelper;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;

/**
 * 
 * @author tcook
 * @since Nov, 2017
 * 
 * TODO: clean up and remove redundant code
 * 
 */
public class NavDriver extends AbstractSensorModule<NavConfig>  implements IMultiSourceDataProducer, FileListener  
{
    static final Pattern DATA_FILE_REGEX = Pattern.compile(".+\\.dat");
    
    Thread watcherThread;
    DirectoryWatcher watcher;
    AirportOutput navOutput;
	WaypointOutput wyptOutput;
	NavaidOutput navaidOutput;
	Set<String> entityIDs;
	//Multimap<String, AbstractFeature> navEntryFois;
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:aviation:";
	static final String AIRPORTS_UID_PREFIX = SENSOR_UID_PREFIX + "airports:";
	static final String WAYPOINTS_UID_PREFIX = SENSOR_UID_PREFIX + "waypoints:";
	static final String NAVAID_UID_PREFIX = SENSOR_UID_PREFIX + "navaids:";
	AtomicBoolean loading = new AtomicBoolean(false);
	

	public NavDriver() {
		this.entityIDs = new TreeSet<>();
		//this.navEntryFois = LinkedListMultimap.create();
	}

	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("NavDB offerings");
		}
	}

	boolean airports = true;
	boolean waypoints = true;
	boolean navaids = true;

	@Override
	public void init() throws SensorHubException
	{
		super.init();
		
		// IDs
		this.uniqueID = SENSOR_UID_PREFIX + "navDb";
		this.xmlID = "NAVDB";

		// Initialize Outputs
		try {
            if (airports) {
                this.navOutput = new AirportOutput(this);
                addOutput(navOutput, false);
                navOutput.init();
            }

            if (navaids) {
                this.navaidOutput = new NavaidOutput(this);
                addOutput(navaidOutput, false);
                navaidOutput.init();
            }
            
			if (waypoints) {
				this.wyptOutput = new WaypointOutput(this);
				addOutput(wyptOutput, false);
				wyptOutput.init();
			}			
		} catch (IOException e) {
			throw new SensorHubException("Cannot instantiate NavDB outputs", e);
		}        

	}

	@Override
	public void start() throws SensorHubException
	{
	    loading.set(false);
	    startDirectoryWatcher();
        readLatestDataFile();
	}


    private void startDirectoryWatcher() throws SensorHubException
    {
        try
        {
            watcher = new DirectoryWatcher(Paths.get(config.navDbPath), StandardWatchEventKinds.ENTRY_CREATE);
            watcherThread = new Thread(watcher);
            watcher.addListener(this);
            watcherThread.start();
            getLogger().info("Watching directory {} for data updates", config.navDbPath);
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error creating directory watcher on " + config.navDbPath, e);
        }
    }
    
    
    private void readLatestDataFile() throws SensorHubException
    {
        // list all available nav DB data files
        File dir = new File(config.navDbPath);
        File[] navDbFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return DATA_FILE_REGEX.matcher(name).matches();
            }
        });        
        
        // skip if nothing is available
        if (navDbFiles.length == 0)
        {
            getLogger().warn("No Nav DB file available");
            return;
        }
        
        // get the one with latest time stamp
        File latestFile = navDbFiles[0];
        for (File f: navDbFiles)
        {
            if (f.lastModified() > latestFile.lastModified())
                latestFile = f;
        }
        
        // trigger reader
        loadAllData(latestFile.toPath());
    }


    /*
     * called whenever we get a new Nav DB file
     */
    @Override
    public void newFile(Path p)
    {
        try
        {
            loadAllData(p);
        }
        catch (Exception e)
        {
            getLogger().error("Error loading new data", e);
        }
    }
    
    private void loadAllData(Path navDbFilePath) throws SensorHubException
    {
        // only continue when it's a new nav DB file
        if (!DATA_FILE_REGEX.matcher(navDbFilePath.getFileName().toString()).matches())
            return;
        
        // skip if already loading
        if (!loading.compareAndSet(false, true))
            return;
                
        getLogger().info("Loading new Nav DB file: {}", navDbFilePath);
        
        try
        {
            List<NavDbEntry> allEntries = LufthansaParser.getNavDbEntries(navDbFilePath);
            Set<String> newEntityIDs = new TreeSet<>();
            
            if (airports)
                loadAirports(allEntries, newEntityIDs);
            if (navaids)
                loadNavaids(allEntries, newEntityIDs);
            if (waypoints)
                loadWaypoints(allEntries, newEntityIDs);
            
            // switch to new entity IDs set atomically
            entityIDs = newEntityIDs;
        }
        catch (IOException e)
        {
            getLogger().error("Cannot read Nav DB File", e);
        }
        
        loading.set(false);
    }
    

	private void loadAirports(List<NavDbEntry> navDbEntries, Set<String> newEntityIDs)
	{
	    List<NavDbEntry> airports;
	    if (config.airportFilterPath != null)
	        airports = getSelectedAirports(navDbEntries, config.airportFilterPath);
	    else
	        airports = LufthansaParser.filterEntries(navDbEntries, Type.AIRPORT);
	    
        //SMLHelper smlFac = new SMLHelper();
        //GMLFactory gmlFac = new GMLFactory(true);
        
	    // add FOIS, one per airport
        for (NavDbEntry airport: airports) {
            /*String uid = AIRPORTS_UID_PREFIX + airport.icao;
            AbstractFeature airportFoi = smlFac.newPhysicalSystem();
            airportFoi.setId(airport.icao);             
            airportFoi.setUniqueIdentifier(uid);
            airportFoi.setName(airport.name);
            airportFoi.setDescription("Airport Foi for " + airport.name);
            Point location = gmlFac.newPoint();
            location.setPos(new double [] {airport.lat, airport.lon});
            airportFoi.setLocation(location);
            navEntryFois.put(uid, airportFoi);*/
            newEntityIDs.add(airport.icao);
        }

        //  Send to output
        navOutput.sendEntries(airports);
        
        getLogger().info("{} airports loaded", airports.size()); 		
	}


	private void loadNavaids(List<NavDbEntry> navDbEntries, Set<String> newEntityIDs)
	{
	    List<NavDbEntry> navaids = LufthansaParser.filterEntries(navDbEntries, Type.NAVAID);
        //SMLHelper smlFac = new SMLHelper();
        //GMLFactory gmlFac = new GMLFactory(true);
        
        // add FOIS, one per airport
        for (NavDbEntry navaid: navaids) {
            /*String uid = NAVAID_UID_PREFIX + navaid.id;
            AbstractFeature navaidFoi = smlFac.newPhysicalSystem();
            navaidFoi.setId(navaid.id);
            navaidFoi.setUniqueIdentifier(uid);
            navaidFoi.setName(navaid.name);
            navaidFoi.setDescription("Navaid Foi for " + navaid.name);
            Point location = gmlFac.newPoint();
            location.setPos(new double [] {navaid.lat, navaid.lon});
            navaidFoi.setLocation(location);
            navEntryFois.put(uid, navaidFoi);*/
            newEntityIDs.add(navaid.id);                
        }

        //  Send to output
        navaidOutput.sendEntries(navaids);
        
        getLogger().info("{} navaids loaded", navaids.size());
	}
	

	private void loadWaypoints(List<NavDbEntry> navDbEntries, Set<String> newEntityIDs)
	{
	    List<NavDbEntry> waypts = LufthansaParser.filterEntries(navDbEntries, Type.WAYPOINT);
        //SMLHelper smlFac = new SMLHelper();
        //GMLFactory gmlFac = new GMLFactory(true);
        
        for (NavDbEntry waypt: waypts) {
            /*String uid = WAYPOINTS_UID_PREFIX + waypt.id;
            AbstractFeature wyptFoi = smlFac.newPhysicalSystem();
            wyptFoi.setId(waypt.id);
            wyptFoi.setUniqueIdentifier(uid);
            wyptFoi.setName(waypt.name);
            wyptFoi.setDescription("Navaid Foi for " + waypt.name);
            Point location = gmlFac.newPoint();
            location.setPos(new double [] {waypt.lat, waypt.lon});
            wyptFoi.setLocation(location);
            navEntryFois.put(uid, wyptFoi);*/
            newEntityIDs.add(waypt.id);
        }

        //  Send to output
        wyptOutput.sendEntries(waypts);
        
        getLogger().info("{} waypoints loaded", waypts.size());	
	}


    public List<String> readSelectedAirportIcaos(String filterPath)
    {
        List<String> delta = new ArrayList<>();
        
        try (BufferedReader br= new BufferedReader(new FileReader(filterPath))) {
            while (true) {
                String l = br.readLine();
                if(l==null)  break;
                String icao = l.substring(0, 4);
                delta.add(icao);
            }
        }
        catch (IOException e)
        {
            getLogger().error("Cannot read airport list", e);
        }
        
        return delta;
    }
    

    public List<NavDbEntry> getSelectedAirports(List<NavDbEntry> navDbEntries, String deltaPath)
    {
        List<String> icaos = readSelectedAirportIcaos(deltaPath);
        List<NavDbEntry> deltaAirports = new ArrayList<>();
        
        for (NavDbEntry a: navDbEntries) {
            if(icaos.contains(a.id) && a.type == Type.AIRPORT) {
                deltaAirports.add(a);
            }
        }
        
        return deltaAirports;
    }
	

	@Override
	public void stop() throws SensorHubException
	{
	    if (watcherThread != null)
            watcherThread.interrupt();
	}


	@Override
	public boolean isConnected() 
	{
		return true;
	}

	
	@Override
	public AbstractFeature getCurrentFeatureOfInterest()
	{
		return null;
	}


	@Override
	public Collection<String> getEntityIDs()
	{
		return Collections.unmodifiableCollection(entityIDs);
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
		return null;
	}


	@Override
	public Collection<? extends AbstractFeature> getFeaturesOfInterest()
	{
		//return Collections.unmodifiableCollection(navEntryFois.values());
	    return Collections.emptyList();
	}


	@Override
	public Collection<String> getFeaturesOfInterestIDs()
	{
		return Collections.unmodifiableCollection(entityIDs);
	}
	

	@Override
	public Collection<String> getEntitiesWithFoi(String foiID)
	{
		return Arrays.asList(foiID.substring(foiID.lastIndexOf(':')+1));
	}
}
