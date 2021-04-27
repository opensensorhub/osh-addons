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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.nexrad.aws.NexradSqsService;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.ChunkQueueManager;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.RealtimeRadialProvider;
import org.sensorhub.impl.sensor.nexrad.ucar.ArchiveRadialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.PhysicalSystem;


/**
 * <p>
 * </p>
 *
 * @author Tony Cook <tony.coook@opensensorhub.org>
 */
public class NexradSensor extends AbstractSensorModule<NexradConfig> implements IMultiSourceDataProducer
{
	static final Logger logger = LoggerFactory.getLogger(NexradSensor.class);
	static final String SITE_UID_PREFIX = "urn:osh:sensors:nexrad:";

	NexradOutput dataInterface;
	RadialProvider radialProvider;  // either Realtime or archive AWS source
	boolean isRealtime;

	NexradSqsService nexradSqs;
	ChunkQueueManager chunkQueueManager;
	
	
	public NexradSensor() throws SensorHubException
	{
	}
	

	public void setQueueActive() throws IOException
	{
		if(!isRealtime) 
			return;
		nexradSqs.setQueueActive();
		nexradSqs.setNumThreads(config.numThreads);
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
		this.uniqueID = SITE_UID_PREFIX + "network";
		this.xmlID = "NEXRAD_NETWORK";

		if(config.archiveStartTime != null && config.archiveStopTime != null) {
			isRealtime = false;
			radialProvider = new ArchiveRadialProvider(config);
		} else {
			try {
				isRealtime = true;
				nexradSqs = new NexradSqsService(config.queueName, config.siteIds);
				nexradSqs.setQueueIdleTimeMillis(TimeUnit.MINUTES.toMillis(config.queueIdleTimeMinutes));
				chunkQueueManager = new ChunkQueueManager(this);
				//  DECOUPLE ME!!!
				nexradSqs.setChunkQueueManager(chunkQueueManager);
				chunkQueueManager.setS3Client(nexradSqs.getS3client());
				radialProvider = new RealtimeRadialProvider(this, chunkQueueManager);
				setQueueActive();
			} catch (IOException e) {
				throw new SensorHubException("Could not instantiate NexradSqsService", e);
			}
		}
		dataInterface = new NexradOutput(this);
		addOutput(dataInterface, false);
		dataInterface.init();	
	}


	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescription)
		{
			super.updateSensorDescription();
			
			sensorDescription.setId("NEXRAD_SENSOR");
			sensorDescription.setUniqueIdentifier(SITE_UID_PREFIX);
			sensorDescription.setDescription("Sensor supporting Level II Nexrad data");
		}
	}

	@Override
	protected void doStart() throws SensorHubException
	{
		SMLHelper smlFac = new SMLHelper();
		GMLFactory gmlFac = new GMLFactory(true);

		// generate station FOIs and full descriptions
		for (String siteId: config.siteIds)
		{
			String uid = SITE_UID_PREFIX + siteId;
			String name = siteId;
			String description = "Nexrad site " + siteId;

			// generate small SensorML for FOI (in this case the system is the FOI)
			PhysicalSystem foi = smlFac.newPhysicalSystem();
			foi.setId(siteId);
			foi.setUniqueIdentifier(uid);
			foi.setName(name);
			foi.setDescription(description);
			Point stationLoc = gmlFac.newPoint();
			NexradSite site = config.getSite(siteId);
			stationLoc.setPos(new double [] {site.lat, site.lon, site.elevation});
			foi.setLocation(stationLoc);
			foiMap.put(uid, foi);
		}

		dataInterface.start(radialProvider); 
	}


	@Override
	protected void doStop() throws SensorHubException
	{
		dataInterface.stop();
		if(isRealtime)
			nexradSqs.stop();
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

	
    @Override
    public Map<String, ? extends IDataProducer> getMembers()
    {
        return Collections.emptyMap();
    }
    

    @Override
    public Collection<String> getProceduresWithFoi(String foiUID)
    {
        return Arrays.asList(getUniqueIdentifier());
    }
}
