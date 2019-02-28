/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.nldn;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.util.regex.Pattern;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.utils.grid.DirectoryWatcher;
import org.sensorhub.impl.utils.grid.FileListener;


/**
 * 
 * @author tcook
 * @since Sep 13, 2017
 * 
 * TODO:  Need a thread that monitors the watcherThread and restarts it if it dies
 *
 */
public class NldnSensor extends AbstractSensorModule<NldnConfig> implements FileListener
{
    static final Pattern DATA_FILE_REGEX = Pattern.compile(".*NLDN.*grb2");
    
    NldnOutput meshInterface;
	Thread watcherThread;
	DirectoryWatcher watcher;
	

	@Override
	public void init() throws SensorHubException
	{
		super.init();

		this.uniqueID = "urn:osh:sensor:earthcast:nldn";
		this.xmlID = "ECT_NLDN";

		// initialize outputs
        this.meshInterface = new NldnOutput(this);
		addOutput(meshInterface, false);
		meshInterface.init();
	}
	

	@Override
	public void start() throws SensorHubException
	{
	    startDirectoryWatcher();
	    readLatestDataFile();
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


    private void startDirectoryWatcher() throws SensorHubException
    {
        try
        {
            watcher = new DirectoryWatcher(Paths.get(config.dataPath), StandardWatchEventKinds.ENTRY_CREATE);
            watcherThread = new Thread(watcher);
            watcher.addListener(this);
            watcherThread.start();
            getLogger().info("Watching directory {} for data updates", config.dataPath);
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error creating directory watcher on " + config.dataPath, e);
        }
    }
    
    
    private void readLatestDataFile() throws SensorHubException
    {
        // list all available NLDN data files
        File dir = new File(config.dataPath);
        File[] turbFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return DATA_FILE_REGEX.matcher(name).matches();
            }
        });        
        
        // skip if nothing is available
        if (turbFiles.length == 0)
        {
            getLogger().warn("No NLDN data file available");
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


	/*
     * called whenever we get a new NLDN file
     */
    @Override
    public void newFile(Path p)
    {
        // only continue when it's a new turbulence GRIB file
        if (!DATA_FILE_REGEX.matcher(p.getFileName().toString()).matches())
            return;
        
        // try to read file with NldnReader
        try
        {
            getLogger().info("Loading new NLDN data file: {}", p);
            
            NldnReader reader = new NldnReader(p.toString());
            NldnRecord rec = reader.readNldn();
            if (rec == null)
                throw new IOException("NldnReader returned null record");
            
            meshInterface.sendMeasurement(rec);
        }
        catch (Exception e)
        {
            getLogger().error("Error reading NLDN data file: {}", p, e);
        }
    }
}
