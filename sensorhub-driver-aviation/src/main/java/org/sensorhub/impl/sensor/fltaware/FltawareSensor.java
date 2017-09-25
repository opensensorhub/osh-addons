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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.mesh.DirectoryWatcher;
import org.sensorhub.impl.sensor.mesh.MeshReader;
import org.sensorhub.impl.sensor.mesh.MeshRecord;

/**
 * 
 * @author tcook
 * @since Sep 13, 2017
 * 
 * TODO:  Need a thread that monitors the watcherThread and restarts it if it dies
 *
 */
public class FltawareSensor extends AbstractSensorModule<FltawareConfig> 
{
	FltawareOutput fltawareInterface;
	Thread watcherThread;

	@Override
	public void init() throws SensorHubException
	{
		super.init();

		System.err.println("dataPath: " + config.dataPath);
		// IDs
		this.uniqueID = "urn:osh:sensor:earthcast:fltaware";
		this.xmlID = "EarthcastFltaware";

		// Initialize interface
		this.fltawareInterface = new FltawareOutput(this);

		addOutput(fltawareInterface, false);
		fltawareInterface.init();
	}

	@Override
	public void start() throws SensorHubException
	{
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
}
