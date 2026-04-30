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

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.utils.datafiles.DataFileWatcher;


/**
 * 
 * @author tcook
 * @since Sep 13, 2017
 * 
 * TODO:  Need a thread that monitors the watcherThread and restarts it if it dies
 *
 */
public class NldnSensor extends AbstractSensorModule<NldnConfig>
{
    static final Pattern DATA_FILE_REGEX = Pattern.compile(".*NLDN.*grb2");
    
    NldnOutput nldnInterface;
    DataFileWatcher dataFileWatcher;
	

	@Override
    protected void doInit() throws SensorHubException
	{
		super.doInit();

		this.uniqueID = "urn:osh:sensor:mrms:nldn";
		this.xmlID = "MRMS_NLDN";

		// initialize outputs
        this.nldnInterface = new NldnOutput(this);
		addOutput(nldnInterface, false);
		nldnInterface.init();
	}
	

	@Override
	protected void doStart() throws SensorHubException
	{
	    dataFileWatcher = new DataFileWatcher(
            "NLDN",
            config.dataPath,
            ".+",
            config.fileNamePattern,
            config.latestPointerFileName,
            this::newFile,
            getLogger());
        dataFileWatcher.start();
        dataFileWatcher.readLatestDataFile();
	}


    @Override
    protected void doStop() throws SensorHubException
    {
        if (dataFileWatcher != null) {
            dataFileWatcher.stop();
            dataFileWatcher = null;
        }
    }


    @Override
    public boolean isConnected()
    {
        return true;
    }


	/*
     * called whenever we get a new NLDN file
     */
    public void newFile(Path p)
    {
        // try to read file with NldnReader
        try
        {
            getLogger().info("Loading new NLDN data file: {}", p);
            
            NldnReader reader = new NldnReader(p.toString());
            NldnRecord rec = reader.readNldn();
            if (rec == null)
                throw new IOException("NldnReader returned null record");
            
            reportStatus("Loaded data file \"" + p.getFileName() + "\"");
            nldnInterface.sendMeasurement(rec);
        }
        catch (Exception e)
        {
            reportError("Error reading NLDN data file \"" + p + "\"", e);
        }
    }
}
