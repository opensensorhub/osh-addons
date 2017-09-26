/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.nldn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.mesh.DirectoryWatcher;
import org.sensorhub.impl.sensor.mesh.FileListener;

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
	NldnOutput meshInterface;
	Thread watcherThread;
	private DirectoryWatcher watcher;

	@Override
	public void init() throws SensorHubException
	{
		super.init();

		System.err.println("dataPath: " + config.dataPath);
		// IDs
		this.uniqueID = "urn:osh:sensor:earthcast:nldn";
		this.xmlID = "EarthcastNLDN";

		// Initialize interface
		try {
			this.meshInterface = new NldnOutput(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SensorHubException("Cannot instantiate NldnOutput", e);
		}        
		addOutput(meshInterface, false);
		meshInterface.init();
	}

	@Override
	public void start() throws SensorHubException
	{
		// Start listening for new files
		meshInterface.start();
		
		try {
			watcher = new DirectoryWatcher(Paths.get(config.dataPath), StandardWatchEventKinds.ENTRY_CREATE);
			watcherThread = new Thread(watcher);
			watcher.addListener(this);
			watcherThread.start();
			System.err.println("****** past run");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SensorHubException("NldnSensor could not create DirectoryWatcher...", e);
		}
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
	public void newFile(Path p) throws IOException {
		try {
			String fn = p.getFileName().toString().toLowerCase();
			if(!fn.contains("nldn") || !fn.endsWith(".grb2")) {
				return;
			}
			File sourceFile = p.toFile();
//			while(!sourceFile.renameTo(sourceFile)) {
//				// Cannot read from file, windows still working on it.
//				try {
//					Thread.sleep(10);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
			System.err.println("*** Reading: " + fn);
			NldnReader reader = new NldnReader(p.toString());
			NldnRecord rec = reader.readNldn();
			if(rec == null) {
				throw new IOException("MeshReader returned null mesh record");
			}
			System.err.println("*** Send: " + rec.timeUtc);
			meshInterface.sendMeasurement(rec);
		} catch (Exception e) {
			//  Catch any errors so the watcher thread stays alive-
			
			e.printStackTrace(System.err);
		}
	}
}
