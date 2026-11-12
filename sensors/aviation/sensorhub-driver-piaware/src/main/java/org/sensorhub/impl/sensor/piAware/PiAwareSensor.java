/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.piAware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.feature.FoiAddedEvent;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.piAware.AircraftJson.Aircraft;
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
    static final int SOCKET_CONNECT_TIMEOUT_MS = 15000;
    static final long SOCKET_CHECKER_PERIOD = SOCKET_CONNECT_TIMEOUT_MS + 30000L;

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
	}
	
	class SocketChecker extends TimerTask {
		public void run() {
			try {
				logger.debug("SocketChecker isConnected = {}", socket.isConnected());
				logger.debug("SocketChecker isClosed = {}", socket.isClosed());
				if(socket == null || socket.isClosed() || !socket.isConnected()) {
					logger.info("No connection to PiAware socket. Attempting start.");
					
					if(sbsParserThread != null)
						sbsParserThread.running = false;
					try {
						socket = new Socket();
						socket.setSoTimeout(30000);
						SocketAddress socketAddress = new InetSocketAddress(config.deviceIp, config.sbsOutboundPort);
						socket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT_MS);
					} catch (IOException e) {
						logger.error("IOException connecting to socket", e);
						return;
					}
					
					// Start SbsParserThread
					sbsParserThread = new SbsParserThread();
					Thread thread = new Thread(sbsParserThread);
					thread.start();
					
					// Start or restart AircraftReader Timer task
					if(aircraftReader == null) {
						try {
							String jsonUrl = "http://" + config.deviceIp + ":" + config.dataPort + "/" +  
										config.dataPath + "/" + config.aircraftJsonFile;
							aircraftReader = new AircraftReader(jsonUrl);
						} catch (MalformedURLException e) {
							logger.warn("aircraftReader malformed url", e);
						}
					} else {
						aircraftReader.stopReaderTask();
					}
					aircraftReader.startReaderTask();
				}
			} catch (Throwable t) {
				logger.error("SocketChecker failed with exception ", t);
			}
		}
	}
	
	class SbsParserThread implements Runnable {
		volatile boolean running;  
		
		@Override
		public void run() {
			logger.info("Start listening on ip:port {}", config.deviceIp + ":" + config.sbsOutboundPort);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
				sbsParser = new SbsParser(getLogger());
				running = true;
				String line = null;
				do  {
					line = in.readLine();
					try {
						if(line == null || line.trim().length() == 0)
							continue;
						SbsPojo rec = sbsParser.parse(line.trim());
						
						logger.trace("calling ensureFlightId for {}", rec.hexIdent);
						String uid = ensureFlightFoi(rec.hexIdent);
						
						// check reader map for flightID corresponding to this hexId
						Aircraft aircraft = aircraftReader.getAircraft(rec.hexIdent);
						if(aircraft != null) {
//							rec.flightID = aircraft.callSign;
							rec.category = aircraft.category;
							rec.callsign = aircraft.flight;
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
						logger.error("Exception in SBSParserThread", e);
					}
				} while (line != null && running);
			} catch (Throwable t) {
				logger.error("Exception in SBSParserThread", t);
				// Likely disconnected from PiAware receiver
				// Close socket and socketChecker thread should attempt to reconnect at configured check period
				try {
					socket.close();
				} catch (IOException e) {
					logger.error("Exception trying to close socket", e);
				}
			}
			
		}
	}
	
	@Override
	public void doStart() throws SensorHubException
	{
		// Create socket here and keep track so we can reopen if it gets closed (i.e. power/network outage)
			socket = new Socket();
			socketChecker= new SocketChecker();
			socketTimer = new Timer();
			socketTimer.scheduleAtFixedRate(socketChecker, 0, SOCKET_CHECKER_PERIOD);
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

		// send event
		long now = System.currentTimeMillis();
		eventHandler.publish(new FoiAddedEvent(now, SENSOR_UID, uid, Instant.now() ));

		logger.debug("{}: New FOI added: {}; Num FOIs = {}", flightId, uid, foiMap.size());
		return uid;
	}


	@Override
	public boolean isConnected()
	{
		return socket.isConnected();
	}
}