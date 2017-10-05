/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.turbulence;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.util.List;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.fltaware.FlightPlan;
import org.sensorhub.impl.sensor.mesh.DirectoryWatcher;
import org.sensorhub.impl.sensor.mesh.FileListener;

import ucar.ma2.InvalidRangeException;


public class TurbulenceSensor extends AbstractSensorModule<TurbulenceConfig> implements FileListener
{
	TurbulenceOutput turbOutput;
	Thread watcherThread;
	private DirectoryWatcher watcher;
	Path latestTurbPath;

	@Override
	public void init() throws SensorHubException
	{
		super.init();

		System.err.println("dataPath: " + config.dataPath);
		// IDs
		this.uniqueID = "urn:osh:sensor:earthcast:turbulence";
		this.xmlID = "EarthcastTurbulence";

		// Initialize interface
		this.turbOutput= new TurbulenceOutput(this);
		addOutput(turbOutput, false);
		turbOutput.init();
	}



	@Override
	public void start() throws SensorHubException
	{
		try {
			watcher = new DirectoryWatcher(Paths.get(config.dataPath), StandardWatchEventKinds.ENTRY_CREATE);
			watcherThread = new Thread(watcher);
			watcher.addListener(this);
			watcherThread.start();
			System.err.println("****** past run");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SensorHubException("TurbulenceSensor could not create DirectoryWatcher...", e);
		}

	}

	@Override
	public void stop() throws SensorHubException
	{

	}

	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	public void newFile(Path p) throws IOException {
		String fn = p.getFileName().toString().toLowerCase();
		if(!fn.contains("gtgturb") || !fn.endsWith(".grb2")) {
			return;
		}

		latestTurbPath = p;
		//  
		FlightPlan sample = FlightPlan.getSamplePlan();
		// Hey, I can get the FLightPLan data from the internal OSH node
		TurbulenceReader reader = new TurbulenceReader(p.toString());
		try {
			List<TurbulenceRecord> recs = reader.getTurbulence(sample.getLats(), sample.getLons());
			turbOutput.sendProfiles(recs);
		} catch (InvalidRangeException e) {
			e.printStackTrace(System.err);
		}
	}
}
