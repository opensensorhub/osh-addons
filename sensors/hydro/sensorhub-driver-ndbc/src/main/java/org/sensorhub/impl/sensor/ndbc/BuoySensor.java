/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.ndbc;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.feature.FoiAddedEvent;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.ogc.gml.IFeature;
import org.vast.sensorML.SMLHelper;

import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.PhysicalSystem;


/**
 * @author tcook
 * @since Oct 1, 2017
 * 
 */
public class BuoySensor extends AbstractSensorModule<BuoyConfig>
{
	// Helpers
	SMLHelper smlFac = new SMLHelper();
	GMLFactory gmlFac = new GMLFactory(true);

	// Dynamically created FOIs
	static final String BUOY_UID = "urn:osh:sensor:hydro:buoy";
	static final String BUOY_UID_PREFIX = "urn:osh:hydro:buoy:";

    // Outputs
	BuoyOutput buoyOutput;
	
	// Threads/TimerTasks
	BuoyPollerThread pollerThread;
	Timer pollerTimer;
	
	BuoyDataReader buoyReader;
	
	//  Map of latest buoyId,updateTime
	Map<String, Long> buoyObs = new HashMap<>();
	
	public BuoySensor()
	{
	}


	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("National Data Buoy Center");
		}
	}


	@Override
	public void doInit() throws SensorHubException
	{
//		// IDs
		this.uniqueID = BUOY_UID;
		this.xmlID = "NDBC_Buoy";
//
//		// init outputs
		this.buoyOutput = new BuoyOutput(this);
		addOutput(buoyOutput, false);
		
		buoyOutput.init();
	}

	class BuoyPollerThread extends TimerTask {
		@Override
		public void run() {
			try {
				List<BuoyDataRecord> recs = BuoyDataReader.read(config.realtimeUrl);
				// Loop here and call ensureFoi and publishRecord
				for(BuoyDataRecord rec: recs) {
					// Check to see if FOI is new. If so, add it, publish record, and continue
					if(!buoyObs.containsKey(rec.id)) {
						ensureBuoyFoi(rec.id);  // need to set location
						buoyObs.put(rec.id, rec.timeMs);
						buoyOutput.publishRecord(rec);
						continue;
					}
					// FOI exists, if already pushed to bus, skip it
					Long  latestTime  = buoyObs.get(rec.id);
					if(rec.timeMs == latestTime) {
						logger.debug("Record already published... id, time: {} , {}" , rec.id, Instant.ofEpochMilli(rec.timeMs));
						continue;
					}
					// new record for existing FOI - publish and add to map
					buoyOutput.publishRecord(rec);
					buoyObs.put(rec.id, rec.timeMs);
				}
			} catch (Exception e) {
				logger.error("Error polling NDBC duoy CSV: ", e);
			}
		}
	}
	
	@Override
	public void doStart() throws SensorHubException
	{
		pollerThread =  new BuoyPollerThread();
		pollerTimer = new Timer();
		pollerTimer.scheduleAtFixedRate(pollerThread, 0, config.pollingPeriod);
	}

	@Override
	public void doStop()
	{
		if(pollerTimer != null)
			pollerTimer.cancel();
	}


	private String ensureBuoyFoi(String buoyId)
	{						
		String uid = BUOY_UID_PREFIX + buoyId;

//		// skip if FOI already exists
		IFeature buoyFoi = getCurrentFeaturesOfInterest().get(uid);
		if (buoyFoi != null) 
			return uid;
//
//		// generate small SensorML for FOI (in this case the system is the FOI)
		PhysicalSystem foi = smlFac.createPhysicalSystem().build();
		foi.setId(buoyId);
		foi.setUniqueIdentifier(uid);
		foi.setName("Buoy " + buoyId);
//		foi.setLocation();
		addFoi(foi);

		/// send for added event
		long now = System.currentTimeMillis();
		eventHandler.publish(new FoiAddedEvent(now, BUOY_UID, uid, Instant.now() ));

		logger.trace("{}: New FOI added: {}; Num FOIs = {}", buoyId, uid, foiMap.size());
		return uid;
	}


	@Override
	public boolean isConnected()
	{
		return false;
	}
}
