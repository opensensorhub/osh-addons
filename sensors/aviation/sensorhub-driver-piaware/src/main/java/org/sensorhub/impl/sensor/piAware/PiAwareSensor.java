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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.feature.FoiAddedEvent;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.piAware.AircraftJson.Aircraft;
import org.sensorhub.impl.sensor.piAware.AircraftReader.ReaderTask;
import org.vast.ogc.gml.IFeature;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEHelper;

import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.PhysicalSystem;


/**
 * @author tcook
 * @since Oct 1, 2017
 * 
 * TODO - Catch Exceptions on no signal (i.e. antenna disconnect)and try restarting threads
 */
public class PiAwareSensor extends AbstractSensorModule<PiAwareConfig>
{
	// Helpers
	SMLHelper smlFac = new SMLHelper();
	GMLFactory gmlFac = new GMLFactory(true);

	// Dynamically created FOIs
	static final String SENSOR_UID = "urn:osh:sensor:aviation:piaware";
	static final String FLIGHT_UID_PREFIX = "urn:osh:aviation:flight:";
    static final String DEF_FLIGHT_ID = SWEHelper.getPropertyUri("aero/FlightID");
    static final String DEF_HEX_ID = SWEHelper.getPropertyUri("aero/HexID");

    // Outputs
	LocationOutput locationOutput;
	TrackOutput trackOutput;
	
	// Threads/TimerTasks
	SbsParser sbsParser;
	SbsParserThread sbsParserThread;
	Socket socket;
	
	AircraftReader aircraftReader;
	
	SocketChecker socketChecker;
	Timer socketTimer;
	
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
		if(config.deviceIp == null) {
			throw new SensorHubException("deviceIp is null. Must be set in config for driver to start");
		}
		
		// IDs
		this.uniqueID = SENSOR_UID;
		this.xmlID = "PiAware";

		// init outputs
		this.locationOutput = new LocationOutput(this);
		addOutput(locationOutput, false);
		locationOutput.init();
		this.trackOutput = new TrackOutput(this);
		addOutput(trackOutput, false);
		trackOutput.init();
		
		supportedMessageTypes = new ArrayList<>();
		supportedMessageTypes.add(1);
		supportedMessageTypes.add(2); // Not seeing messageType = 2
		supportedMessageTypes.add(3);
		supportedMessageTypes.add(4);
		
		// Create socket here and keep track so we can reopen if it gets closed (i.e. power/network outage)
		try {
			socket  = new Socket(config.deviceIp, config.sbsOutboundPort);
		} catch (IOException e) {
			throw new SensorHubException(e.getMessage(), e);
		}
		
	}

	class SocketChecker extends TimerTask {
		public void run() {
			if(socket.isClosed()) {
				logger.info("Socket connection to piAware closed. Attempting restart.");
				
				sbsParserThread.running = false;
				try {
					socket  = new Socket(config.deviceIp, config.sbsOutboundPort);
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
				
				sbsParserThread = new SbsParserThread();
				Thread thread = new Thread(sbsParserThread);
				thread.start();

			}
		}
	}
	
	class SbsParserThread implements Runnable {
		volatile boolean running;  
		
		@Override
		public void run() {
			logger.info("Start listening on port " + config.deviceIp);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
				sbsParser = new SbsParser(getLogger());
				running = true;
				String line = null;
				do  {
					try {
						line = in.readLine();
						if(line == null || line.trim().length() == 0)
							continue;
						SbsPojo rec = sbsParser.parse(line.trim());
						
						if(!supportedMessageTypes.contains(rec.transmissionType))
							continue;
						
						logger.trace("calling ensureFlightId for {}", rec.hexIdent);
						String uid = ensureFlightFoi(rec.hexIdent);
						
						// check reader map for flightID corresponding to this hexId
						Aircraft aircraft = aircraftReader.getAircraft(rec.hexIdent);
						if(aircraft != null) {
							rec.flightID = aircraft.flight;
							rec.category = aircraft.category;
						}
						
						
						rec.hexIdent = uid; //PiAwareSensor.SENSOR_UID + rec.hexIdent;
						switch(rec.transmissionType) {
						case 3:
							if(rec.latitude == null || rec.longitude == null || rec.altitude == null)
								break;
							locationOutput.publishRecord(rec, uid);
							break;
						case 4:
							trackOutput.publishRecord(rec, uid);
							break;
						default:
							logger.trace("TransmissionType not supported: {} ", rec.transmissionType);
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} while (line != null && running);
			} catch (Exception e) {
				logger.debug("Exception is SBSParserThread", e);
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

		try {
			String jsonUrl = "http://" + config.deviceIp + ":" + config.dataPort + "/" +  
						config.dataPath + "/" + config.aircraftJsonFile;
			aircraftReader = new AircraftReader(jsonUrl);
			aircraftReader.startReaderTask();
		} catch (MalformedURLException e) {
			throw new SensorHubException(e.getMessage(), e);
		}
		
		socketChecker= new SocketChecker();
		socketTimer = new Timer();
		socketTimer.scheduleAtFixedRate(socketChecker, 0, 5000L);
	}

	@Override
	public void doStop()
	{
		if(sbsParserThread != null)
			sbsParserThread.running = false;
		
		if(aircraftReader != null)
			aircraftReader.stopReaderTask();

		if(socketChecker != null) 
			socketChecker.cancel();
		
		if(socketTimer != null)
			socketTimer.cancel();
	}


	private String ensureFlightFoi(String flightId)
	{						
		String uid = FLIGHT_UID_PREFIX + flightId;

		// skip if FOI already exists
		IFeature flightFoi = getCurrentFeaturesOfInterest().get(uid);
		if (flightFoi != null) 
			return uid;

		// generate small SensorML for FOI (in this case the system is the FOI)
		PhysicalSystem foi = smlFac.createPhysicalSystem().build();
		foi.setId(flightId);
		foi.setUniqueIdentifier(uid);
		foi.setName(flightId + " Flight");
		addFoi(foi);

		// send event - don't need to do this anymore?
		long now = System.currentTimeMillis();
		eventHandler.publish(new FoiAddedEvent(now, SENSOR_UID, uid, Instant.now() ));

		logger.trace("{}: New FOI added: {}; Num FOIs = {}", flightId, uid, foiMap.size());
		return uid;
	}


	@Override
	public boolean isConnected()
	{
		return false;
	}
}
