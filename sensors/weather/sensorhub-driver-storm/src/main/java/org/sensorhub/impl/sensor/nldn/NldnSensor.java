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
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.StandardWatchEventKinds;
import java.util.List;
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
    
    NldnOutput nldnInterface;
	Thread watcherThread;
	DirectoryWatcher watcher;
	

	@Override
	public void init() throws SensorHubException
	{
		super.init();

		this.uniqueID = "urn:osh:sensor:mrms:nldn";
		this.xmlID = "MRMS_NLDN";

		// initialize outputs
        this.nldnInterface = new NldnOutput(this);
		addOutput(nldnInterface, false);
		nldnInterface.init();
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
            watcher = new DirectoryWatcher(Paths.get(config.dataPath), StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
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

    private void readLatestDataFile() throws SensorHubException {
        Path path = Paths.get(config.dataPath);
        try (Stream<Path> stream = Files.walk(path)) {
            List<File> turbFiles = stream.filter(Files::isRegularFile)
                    .filter(p -> DATA_FILE_REGEX.matcher(p.getFileName().toString()).matches())
                    .map(Path::toFile)
                    .sorted(Comparator.comparingLong(File::lastModified).reversed()) // Sort by modification time
                    .collect(Collectors.toList()); // Store into a list

            // skip if nothing is available
            if (turbFiles.size() == 0) {
                getLogger().warn("No NLDN data file available");
                return;
            }

            // trigger reader on the latest
            newFile(turbFiles.get(0).toPath());
        } catch (IOException e) {
            getLogger().error("Error while reading directory: " + e.getMessage());
        }
    }

    private Path getValidFile(Path path) {
        if (Files.isDirectory(path)) {
            // scan directory for grib file
            try (Stream<Path> stream = Files.list(path)) {
                List<Path> matchingFiles = (stream.filter(p -> Files.isRegularFile(p) &&
                                DATA_FILE_REGEX.matcher(p.getFileName().toString()).matches())
                        .collect(Collectors.toList()));
                if (matchingFiles.size() != 1) {
                    return null;
                }
                return matchingFiles.get(0);
            } catch (IOException e) {
                getLogger().error("Cannot read directory : " + e.getMessage());
            }
        } else// only continue when it's a new turbulence GRIB file
            if (DATA_FILE_REGEX.matcher(path.getFileName().toString()).matches()) {
                return path;
            }
        return null;
    }

    private void processFile(Path p) {
        // try to read file with NldnReader
        try {
            getLogger().info("Loading new NLDN data file: {}", p);

            NldnReader reader = new NldnReader(p.toString());
            NldnRecord rec = reader.readNldn();
            if (rec == null)
                throw new IOException("NldnReader returned null record");

            reportStatus("Loaded data file \"" + p.getFileName() + "\"");
            nldnInterface.sendMeasurement(rec);
        } catch (Exception e) {
            reportError("Error reading data file \"" + p + "\"", e);
        }
    }

	/*
     * called whenever we get a new NLDN file
     */
    @Override
    public void newFile(Path path) {
        Path p = this.getValidFile(path);
        if(p != null) {
            this.processFile(p);
        }
    }
}
