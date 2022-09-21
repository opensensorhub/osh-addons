/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.piAware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.feature.FoiAddedEvent;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.ogc.gml.IFeature;
import org.vast.sensorML.SMLHelper;

import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.PhysicalSystem;


/**
 * 
 * @author tcook
 * @since Oct 1, 2017
 */
public class PiAwareSensor extends AbstractSensorModule<PiAwareConfig> // implements IMultiSourceDataProducer
{
	// Helpers
	SMLHelper smlFac = new SMLHelper();
	GMLFactory gmlFac = new GMLFactory(true);

	// Dynamically created FOIs
//	Map<String, AbstractFeature> flightFois;
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:aviation:";
	static final String FLIGHT_UID_PREFIX = "urn:osh:aviation:flight:";

    // Outputs
	SbsOutput sbsOutput;
	SbsParser sbsParser;
	SbsParserThread sbsParserThread;
	List<Integer> supportedMessageTypes;

	public PiAwareSensor()
	{
	}


	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("PiAware Feed");
		}
	}


	@Override
	public void doInit() throws SensorHubException
	{
		// IDs
		this.uniqueID = SENSOR_UID_PREFIX + "PiAware";
		this.xmlID = "PiAware";

		// init outputs
		this.sbsOutput = new SbsOutput(this);
		addOutput(sbsOutput, false);
		sbsOutput.init();
		
		// init Map
		//flightFois = new ConcurrentHashMap<String, AbstractFeature>();  // map type?
		
		//
		supportedMessageTypes = new ArrayList<>();
		supportedMessageTypes.add(2);
		supportedMessageTypes.add(3);
		supportedMessageTypes.add(4);
	}

	class SbsParserThread implements Runnable {
		volatile boolean running;  // 
		
		@Override
		public void run() {
			try (Socket socket = new Socket(config.piawareDeviceIp, config.sbsOutboundPort);
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

				sbsParser = new SbsParser(getLogger());
				running = true;
				String line = null;
				do  {
					try {
						line = in.readLine();
//						System.err.println(line);
						SbsPojo rec = sbsParser.parse(line);
						
						if(!supportedMessageTypes.contains(rec.messageSubType))
							continue;
						String uid = ensureFlightFoi(rec.hexIdent, rec.timeMessageGenerated);
						
						rec.hexIdent = PiAwareSensor.SENSOR_UID_PREFIX + rec.hexIdent;
//						System.err.println("SbsParserThread uid = " + uid);
						sbsOutput.publishRecord(rec, uid);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} while (line != null && running);
			} catch (Exception e) {
				e.printStackTrace(System.err);
				//  Check and reestablish connection if needed
			}
			
		}
	}
	
	@Override
	public void doStart() throws SensorHubException
	{
		sbsParserThread = new SbsParserThread();
		Thread thread = new Thread(sbsParserThread);
		thread.start();
	}

	@Override
	public void doStop()
	{
		if(sbsParserThread != null)
			sbsParserThread.running = false;
	}


	private String ensureFlightFoi(String flightId, long recordTime)
	{						
		String uid = FLIGHT_UID_PREFIX + flightId;

		// skip if FOI already exists
//		AbstractFeature flightFoi = flightFois.get(uid);
		IFeature flightFoi = getCurrentFeaturesOfInterest().get(uid);
		if (flightFoi != null) 
			return uid;

		// generate small SensorML for FOI (in this case the system is the FOI)
		PhysicalSystem foi = smlFac.createPhysicalSystem().build();
		foi.setId(flightId);
		foi.setUniqueIdentifier(uid);
		foi.setName(flightId + " Flight");
		//foiMap.put(uid, foi);
		addFoi(foi);
//		flightFois.put(uid, foi);

		// send event - don't need to do this anymore?
		long now = System.currentTimeMillis();
		eventHandler.publish(new FoiAddedEvent(now, SENSOR_UID_PREFIX + flightId, uid, Instant.now() ));

		getLogger().debug("{}: New FOI added: {}; Num FOIs = {}", flightId, uid, foiMap.size());
		return uid;
	}


	@Override
	public boolean isConnected()
	{
		return false;
	}


//	@Override
//	public Collection<String> getEntityIDs()
//	{
//		return Collections.unmodifiableCollection(flightFois.keySet());
//	}
//
//
//	@Override
//	public AbstractFeature getCurrentFeatureOfInterest()
//	{
//		return null;
//	}
//
//
//	@Override
//	public AbstractProcess getCurrentDescription(String entityID)
//	{
//		return null;
//	}
//
//
//	@Override
//	public double getLastDescriptionUpdate(String entityID)
//	{
//		return 0;
//	}
//
//
//	@Override
//	public AbstractFeature getCurrentFeatureOfInterest(String entityID)
//	{
//		return flightFois.get(entityID);
//	}
//
//
//	@Override
//	public Collection<? extends AbstractFeature> getFeaturesOfInterest()
//	{
//		return Collections.unmodifiableCollection(flightFois.values());
//	}
//
//
//	@Override
//	public Collection<String> getFeaturesOfInterestIDs()
//	{
//		return Collections.unmodifiableCollection(flightFois.keySet());
//	}
//
//
//	@Override
//	public Collection<String> getEntitiesWithFoi(String foiID)
//	{
//		String entityID = foiID.substring(foiID.lastIndexOf(':')+1);
//		return Arrays.asList(entityID);
//	}

}
