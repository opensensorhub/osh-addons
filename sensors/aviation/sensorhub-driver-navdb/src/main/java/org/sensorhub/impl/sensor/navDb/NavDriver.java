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
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.utils.grid.DirectoryWatcher;
import org.sensorhub.impl.utils.grid.FileListener;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;


/**
 * Main driver class for exposing navigation database as sensor outputs
 * 
 * @author Tony Cook
 * @since Nov, 2017
 */
public class NavDriver extends AbstractSensorModule<NavConfig> implements IMultiSourceDataProducer, FileListener
{
    static final Pattern DATA_FILE_REGEX = Pattern.compile(".+\\.dat");
    static final String SENSOR_UID_PREFIX = "urn:osh:sensor:aviation:";
    static final String AIRPORTS_UID_PREFIX = SENSOR_UID_PREFIX + "airports:";
    static final String WAYPOINTS_UID_PREFIX = SENSOR_UID_PREFIX + "waypoints:";
    static final String NAVAID_UID_PREFIX = SENSOR_UID_PREFIX + "navaids:";
    
    Thread watcherThread;
    DirectoryWatcher watcher;
    AtomicBoolean loading = new AtomicBoolean(false);
    
    AirportOutput navOutput;
    WaypointOutput wayptOutput;
    NavaidOutput navaidOutput;
    Set<String> entityIDs;

    
    public NavDriver()
    {
        this.entityIDs = new TreeSet<>();
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

    NavDatabase navDB;
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
        try
        {
            if (airports)
            {
                this.navOutput = new AirportOutput(this);
                addOutput(navOutput, false);
                navOutput.init();
            }

            if (navaids)
            {
                this.navaidOutput = new NavaidOutput(this);
                addOutput(navaidOutput, false);
                navaidOutput.init();
            }

            if (waypoints)
            {
                this.wayptOutput = new WaypointOutput(this);
                addOutput(wayptOutput, false);
                wayptOutput.init();
            }
        }
        catch (IOException e)
        {
            throw new SensorHubException("Cannot instantiate NavDB outputs", e);
        }
    }


    private void loadAirports(Set<String> newEntityIDs)
    {
        Set<String> selectedAirports = (config.airportFilterPath != null) ?
            readSelectedAirportIcaos(config.airportFilterPath) : null;
        
        // build filtered list of airports
        List<NavDbPointEntry> filteredAirports = navDB.getAirports().values().stream()
            .filter(airport -> {
                return selectedAirports == null || selectedAirports.contains(airport.id);
            })
            .peek(airport -> newEntityIDs.add(airport.id))
            .collect(Collectors.toList());
        
        
        // send entries to output
        navOutput.sendEntries(filteredAirports);
        getLogger().info("{} airports loaded", filteredAirports.size());
    }


    private void loadNavaids(Set<String> newEntityIDs)
    {
        Collection<NavDbPointEntry> navaids = navDB.getNavaids().values();
        
        // add FOIS, one per airport
        for (NavDbEntry navaid : navaids)
            newEntityIDs.add(navaid.id);

        // send entries to output
        navaidOutput.sendEntries(navaids);
        getLogger().info("{} navaids loaded", navaids.size());
    }


    private void loadWaypoints(Set<String> newEntityIDs)
    {
        Collection<NavDbPointEntry> waypts = navDB.getWaypoints().values();
        
        // add FOIS, one per airport
        for (NavDbEntry waypt : waypts)
            newEntityIDs.add(waypt.id);

        // send entries to output
        wayptOutput.sendEntries(waypts);
        getLogger().info("{} waypoints loaded", waypts.size());
    }


    public Set<String> readSelectedAirportIcaos(String filterPath)
    {
        Set<String> selectedAirports = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filterPath)))
        {
            while (true)
            {
                String l = br.readLine();
                if (l == null)
                    break;
                String icao = l.substring(0, 4);
                selectedAirports.add(icao);
            }
        }
        catch (IOException e)
        {
            getLogger().error("Cannot read airport list", e);
        }

        return selectedAirports;
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
        newFile(latestFile.toPath());
    }


    /*
     * called whenever we get a new Nav DB file
     */
    @Override
    public void newFile(Path p)
    {
        // only continue when it's a new nav DB file
        if (!DATA_FILE_REGEX.matcher(p.getFileName().toString()).matches())
            return;
        
        // skip if already loading
        if (!loading.compareAndSet(false, true))
            return;
        
        getLogger().info("Loading new Nav DB file: {}", p);
        
        try
        {
            // switch to new Nav DB atomically
            NavDatabase db = new NavDatabase();
            db.reload(p.toString());
            navDB = db;
            
            // reload in-memory entities
            // and switch to new entities atomically
            Set<String> newEntityIDs = new TreeSet<>();
            
            if (airports)
                loadAirports(newEntityIDs);
            if (navaids)
                loadNavaids(newEntityIDs);
            if (waypoints)
                loadWaypoints(newEntityIDs);
            entityIDs = newEntityIDs;
            
            reportStatus("Loaded database file \"" + p + "\"");
        }
        catch (Exception e)
        {
            reportError("Error reading database file \"" + p + "\"", e);
            return;
        }
        
        loading.set(false);
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
        return Arrays.asList(foiID.substring(foiID.lastIndexOf(':') + 1));
    }
    
    
    public NavDatabase getNavDatabase()
    {
        return navDB;
    }
}
