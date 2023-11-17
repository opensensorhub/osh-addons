/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.nexrad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.nexrad.aws.NexradSqsService;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.ChunkQueueManager;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.RealtimeRadialProvider;
import org.sensorhub.impl.sensor.nexrad.ucar.ArchiveRadialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEHelper;

import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.PhysicalSystem;


/**
 * <p>NexradSensor - Sensor Driver for pulling and serving data from AWS S3 Nexrad bucket
 * </p>
 *
 * @author Tony Cook <tony.coook@opensensorhub.org>
 * 
 * TODO
 * 		Add FOIs for all sites
 * 		Support dynamic addition/removal of individual sites
 * 			
 */
public class NexradSensor extends AbstractSensorModule<NexradConfig>
{
	static final Logger logger = LoggerFactory.getLogger(NexradSensor.class);
	static final String SENSOR_UID = "urn:osh:sensor:weather:nexrad";
	static final String SITE_UID_PREFIX = "urn:osh:sensor:weather:nexrad:";
	NexradOutput nexradOutput;
	RadialProvider radialProvider;  // either Realtime or archive AWS source
	boolean isRealtime;

	NexradSqsService nexradSqs;
	ChunkQueueManager chunkQueueManager;
	
	List<NexradSite> enabledSites = new ArrayList<>();
	
	public NexradSensor() throws SensorHubException
	{
	}
	

	public void setQueueActive() throws SensorHubException
	{
		if(!isRealtime) 
			return;
		try {
			nexradSqs.activateQueue(config.purgeExistingMessages);
			nexradSqs.setNumThreads(config.numThreads);
		} catch (IOException e) {
			throw new SensorHubException("Could not activate aws queue", e);
		}
		//		nexradSqs.setChunkQueue(chunkQueue);  // 
		//		chunkQueue.setS3client(nexradSqs.getS3client());  //
		//		nexradSqs.start();
	}
	

	public void setQueueIdle()
	{
		if(isRealtime)
			nexradSqs.setQueueIdle();
	}
	

	@Override
    protected void doInit() throws SensorHubException
	{
		super.doInit();

		// generate IDs
		this.uniqueID = SENSOR_UID;
		this.xmlID = "NEXRAD_NETWORK";

		if(config.archiveStartTime != null && config.archiveStopTime != null) {
			isRealtime = false;
			radialProvider = new ArchiveRadialProvider(config);
		} else {
			try {
				isRealtime = true;
				
				// add FOIS before initializing ChunkQueueManager
				try {
					addFois();
				} catch (IOException e) {
					throw new SensorHubException("Could not instantiate NexradTable. ", e);
				}
				
				nexradSqs = new NexradSqsService(config.queueName, config.siteIds, config.purgeExistingMessages);
				nexradSqs.setQueueIdleTimeMillis(TimeUnit.MINUTES.toMillis(config.queueIdleTimeMinutes));
				chunkQueueManager = new ChunkQueueManager(this);
				//  DECOUPLE ME!!!
				nexradSqs.setChunkQueueManager(chunkQueueManager);
				chunkQueueManager.setS3Client(nexradSqs.getS3client());
				radialProvider = new RealtimeRadialProvider(this, chunkQueueManager);
				setQueueActive();
			} catch (Exception e) {
				throw new SensorHubException("Could not instantiate NexradSqsService", e);
			}
		}
		nexradOutput = new NexradOutput(this);
		addOutput(nexradOutput, false);
		nexradOutput.init();	
	}

	private void addFois() throws IOException {
		// Add FOIs- one per site
		SMLHelper smlFac = new SMLHelper();
		GMLFactory gmlFac = new GMLFactory(true);

		// generate station FOIs for all stations
		NexradTable nexradTable = NexradTable.getInstance();
		Collection<NexradSite> sites = nexradTable.getAllSites();
		for (NexradSite site: sites)
		{
//			if(!site.id.equals("KLBB"))  continue;
			String siteUID = SITE_UID_PREFIX + site.id;
			String name = site.id;
			String description = "Nexrad site " + site.id;

			// generate small SensorML for FOI (in this case the system is the FOI)
			PhysicalSystem foi = smlFac.createPhysicalSystem().build();
			foi.setId(site.id);
			foi.setUniqueIdentifier(siteUID);
			foi.setName(name);
			foi.setDescription(description);
			Point stationLoc = gmlFac.newPoint();
			stationLoc.setSrsName(SWEHelper.getEpsgUri(4326));
			stationLoc.setPos(new double [] {site.lat, site.lon, site.elevation});
			foi.setLocation(stationLoc);
			addFoi(foi);

			logger.debug("SENSOR_UID: {}, siteUID: {}, numSites: {}", SENSOR_UID, siteUID, foiMap.size());
		}
		
		//  Put all config.siteIds into enableSites list
		for(String siteId: config.siteIds) {
			NexradSite site = nexradTable.getSite(siteId); 
			if(site != null) {
				logger.debug("enabling site based on config: {}", siteId);
				enabledSites.add(site);
			} else {
				logger.error("Unkonwn siteId in NexradConfig.siteIds: {}", siteId);
			}
		}
	}

	// NOTE: may not need enabledSites here- just let queueManager deal with it
	public void enableSite(NexradSite site) {
		enabledSites.add(site);
		chunkQueueManager.enableSite(site.id);
	}
	
	// NOTE: may not need enabledSites here- just let queueManager deal with it
	public void disableSite(NexradSite site) {
		enabledSites.remove(site);
		chunkQueueManager.disableSite(site.id);
	}
	
	public List<NexradSite> getEnabledSites() {
		return enabledSites;
	}
	
	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescription)
		{
			super.updateSensorDescription();
			
			sensorDescription.setId("NEXRAD_SENSOR");
			sensorDescription.setUniqueIdentifier(SENSOR_UID);  // does this need to be done here and in doInit()
			sensorDescription.setDescription("Sensor supporting Level II Nexrad data");
		}
	}

	@Override
	protected void doStart() throws SensorHubException
	{
		setQueueActive();
		nexradOutput.start(radialProvider); 
	}


	@Override
	protected void doStop() throws SensorHubException
	{
		nexradOutput.stop();
		if(isRealtime) {
			nexradSqs.stop();
		}
	}


	@Override
	public void cleanup() throws SensorHubException
	{

	}


	@Override
	public boolean isConnected()
	{
		return true;
	}
}
