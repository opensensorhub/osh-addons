/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.sensorhub.api.comm.IMessageQueuePush;
import org.sensorhub.api.comm.IMessageQueuePush.MessageListener;
import org.sensorhub.api.comm.MessageQueueConfig;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.flightAware.FlightAwareConfig.Mode;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Asserts;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;


/**
 * 
 * @author tcook
 * @since Oct 1, 2017
 */
public class FlightAwareDriver extends AbstractSensorModule<FlightAwareConfig> implements IMultiSourceDataProducer,
	FlightPlanListener, PositionListener
{
    FlightPlanOutput flightPlanOutput;
	FlightPositionOutput flightPositionOutput;
	
	// Helpers
	SMLHelper smlFac = new SMLHelper();
	GMLFactory gmlFac = new GMLFactory(true);

	// Dynamically created FOIs
	Map<String, AbstractFeature> flightFois;
	Map<String, AbstractFeature> aircraftDesc;
	Map<String, FlightObject> flightPositions;
	static final String SENSOR_UID_PREFIX = "urn:osh:sensor:aviation:";
	static final String FLIGHT_UID_PREFIX = "urn:osh:aviation:flight:";

    //  Warn us if messages start stacking up- note sys clock dependence
    //  Allow configuration of these values
	private static final long MESSAGE_TIMER_CHECK_PERIOD = 10000L; // in ms
    private static final long MESSAGE_RECEIVE_TIMEOUT = 20000L; // in ms
    private static final long MESSAGE_LATENCY_WARN_LIMIT = 120000L; // in ms
    
    //  Use the following to debug disconnect/reconnect code
//  private static final long MESSAGE_TIMER_CHECK_PERIOD = 20000L; // in ms
//  private static final long MESSAGE_LATENCY_WARN_LIMIT = 10000L; // in ms
//  private static final long MESSAGE_LATENCY_RESTART_LIMIT = 20000L; // in ms
    
    ScheduledExecutorService timer;
    FlightAwareClient firehoseClient;
    IMessageQueuePush msgQueue;
    MessageHandler msgHandler;
    int retriesLeft;
    boolean connected;
        
    
	class MessageTimeCheck implements Runnable
	{
        @Override
        public void run()
        {
            if (msgHandler != null)
            {
                long sysTime = System.currentTimeMillis();
                long lastMsgRecvTime = msgHandler.getLastMessageReceiveTime();
                long lastMsgTime = msgHandler.getLastMessageTime()*1000;
                long lastMsgDelta = sysTime - lastMsgRecvTime;
                long lastMsgAge = sysTime - lastMsgTime;
                
                // if not receiving anything from queue, maybe no other instance connected to firehose?
                if (firehoseClient == null && msgQueue != null && lastMsgDelta > MESSAGE_RECEIVE_TIMEOUT)
                {
                    getLogger().error("No message received from message queue");
                    stop();
                    
                    if (retriesLeft <= 0)
                    {
                        getLogger().error("Max number of retries reached");
                        return;
                    }
                    
                    retriesLeft--;
                    if (config.connectionType != Mode.PUBSUB)
                        startWithFirehose();
                    else
                        startWithPubSub();
                }
                
                // if connection to firehose didn't succeed
                else if (firehoseClient != null && !firehoseClient.isStarted())
                {
                    getLogger().error("Lost connection to Firehose");
                    stop();
                    
                    if (retriesLeft <= 0)
                    {
                        getLogger().error("Max number of retries reached");
                        return;
                    }
                    
                    retriesLeft--;
                    if (config.connectionType != Mode.FIREHOSE)
                        startWithPubSub();
                    else
                        startWithFirehose();
                }
                
                else
                {
                    retriesLeft = config.maxRetries;
                    
                    // if initially connected but no message received for some time
                    if (lastMsgDelta > MESSAGE_RECEIVE_TIMEOUT)
                    {
                        getLogger().error("No message received in the last {}s", MESSAGE_RECEIVE_TIMEOUT/1000);                        
                        if (firehoseClient != null)
                            firehoseClient.restart();
                    }
                    
                    // if messages getting old (usually when we can't keep up)
                    else if (lastMsgAge > MESSAGE_LATENCY_WARN_LIMIT)
                        getLogger().warn("Messages getting old. Last dated {}s ago", lastMsgAge/1000);
                    
                    else
                        getLogger().info("FA connection OK: Last message received {}s ago", lastMsgDelta/1000);
                }                    
            }
        }
    }
    

	public FlightAwareDriver()
	{
		this.flightFois = new ConcurrentSkipListMap<>();
		this.aircraftDesc = new ConcurrentHashMap<>();
		this.flightPositions = new ConcurrentHashMap<>();
	}
	
	
	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescLock)
		{
			super.updateSensorDescription();
			sensorDescription.setDescription("FlightAware Feed");
		}
	}
	

	@Override
	public void init() throws SensorHubException
	{
		// IDs
		this.uniqueID = SENSOR_UID_PREFIX + "flightAware";
		this.xmlID = "Earthcast";

		// init flight plan output
		this.flightPlanOutput = new FlightPlanOutput(this);
		addOutput(flightPlanOutput, false);
		flightPlanOutput.init();

		// init flight position output
		this.flightPositionOutput = new FlightPositionOutput(this);
		addOutput(flightPositionOutput, false);
		flightPositionOutput.init();
		
		this.retriesLeft = config.maxRetries;
	}
	

	@Override
	public void start() throws SensorHubException
	{
	    // if configured to use firehose only, connect to firehose now
        if (config.connectionType == Mode.FIREHOSE || config.connectionType == Mode.FIREHOSE_THEN_PUBSUB)
            startWithFirehose();
        else
            startWithPubSub();
	}
	

    private void startWithFirehose()
    {
        reportStatus("Connecting to Firehose channel...");
        timer = Executors.newSingleThreadScheduledExecutor();
        
        // connect to pub/sub channel for publishing only
        if (config.pubSubConfig != null)
            connectToPubSub(true);
        
        // create message handler
        if (msgQueue != null)
            msgHandler = new MessageHandlerWithForward(config.userName, config.password, msgQueue);
        else
            msgHandler = new MessageHandler(config.userName, config.password);
        msgHandler.addPlanListener(this);
        msgHandler.addPositionListener(this);
        
        // configure firehose feed
        firehoseClient = new FlightAwareClient(config.hostname, config.userName, config.password, new IMessageHandler() {
            @Override
            public void handle(String msg)
            {
                try {
                    msgHandler.handle(msg);
                }
                catch (IllegalStateException e) {
                    clearStatus();
                    reportError("Cannot connect to Firehose", e);
                    return;
                }
                
                if (!connected)
                {
                    reportStatus("Connected to Firehose channel");
                    connected = true;
                }
            }            
        });
        for(String mt: config.messageTypes)
            firehoseClient.addMessageType(mt);
        for(String airline: config.airlines)
            firehoseClient.addAirline(airline);
        
        // start firehose feed
        connected = false;
        firehoseClient.start();
        
        // start watchdog thread
        long randomDelay = (long)(Math.random()*10000.);
        timer.scheduleWithFixedDelay(new MessageTimeCheck(),
                MESSAGE_RECEIVE_TIMEOUT+randomDelay,
                MESSAGE_TIMER_CHECK_PERIOD+randomDelay,
                TimeUnit.MILLISECONDS);
	}
    
    
    private void startWithPubSub()
    {
        Asserts.checkNotNull(config.pubSubConfig, "PubSubConfig");
        
        reportStatus("Connecting to Pub/Sub channel...");
        timer = Executors.newSingleThreadScheduledExecutor();
        
        // create message handler
        msgHandler = new MessageHandler(config.userName, config.password);
        msgHandler.addPlanListener(this);
        msgHandler.addPositionListener(this);
        
        // start pub/sub receiver
        connectToPubSub(false);
        
        // start watchdog thread
        long randomDelay = (long)(Math.random()*10000.);
        timer.scheduleWithFixedDelay(new MessageTimeCheck(),
                MESSAGE_RECEIVE_TIMEOUT+randomDelay,
                MESSAGE_TIMER_CHECK_PERIOD+randomDelay,
                TimeUnit.MILLISECONDS);
    }
    
	
	private void connectToPubSub(boolean publishOnly)
	{
	    try
	    {
	        // generate full subscription name
	        MessageQueueConfig pubSubConfig = (MessageQueueConfig)config.pubSubConfig.clone();
            String prefix = pubSubConfig.subscriptionName == null ? "" : pubSubConfig.subscriptionName + "-";
            String hostname = InetAddress.getLocalHost().getHostName().toLowerCase() + "-";
            pubSubConfig.subscriptionName = hostname + prefix + pubSubConfig.topicName;
            
            // load message queue implementation
	        msgQueue = (IMessageQueuePush)SensorHub.getInstance().getModuleRegistry().loadClass(pubSubConfig.moduleClass);
            msgQueue.init(pubSubConfig);
            
            if (!publishOnly)
            {
                msgQueue.registerListener(new MessageListener() {
                    @Override
                    public void receive(byte[] msg)
                    {
                        if (!connected)
                        {
                            reportStatus("Connected to Pub/Sub channel");
                            connected = true;
                        }
                        
                        msgHandler.handle(new String(msg, StandardCharsets.UTF_8));
                    }                
                });
            }
            
            // start message queue
            connected = false;
            msgQueue.start();
        }
	    catch (Exception e)
	    {
            throw new IllegalStateException("Cannot load message queue implementation", e);
        }
	}
	

	@Override
	public void stop()
	{
		if (timer != null) {
		    timer.shutdownNow();
            timer = null;
		}
        
        if (msgHandler != null) {
            msgHandler.stop();
            msgHandler = null;
        }
        
        if (msgQueue != null) {
            msgQueue.stop();
            msgQueue = null;
        }
	    
	    if (firehoseClient != null) {
			firehoseClient.stop();
			firehoseClient = null;
	    }
	}
	

	private void ensureFlightFoi(String flightId, long recordTime)
	{						
	    String uid = FLIGHT_UID_PREFIX + flightId;
	    
	    // skip if FOI already exists
		AbstractFeature fpFoi = flightFois.get(uid);
        if (fpFoi != null) 
            return;
        
		// generate small SensorML for FOI (in this case the system is the FOI)
		PhysicalSystem foi = smlFac.newPhysicalSystem();
		foi.setId(flightId);
		foi.setUniqueIdentifier(uid);
		foi.setName(flightId + " Flight");
		flightFois.put(uid, foi);

		// send event
		long now = System.currentTimeMillis();
		eventHandler.publishEvent(new FoiEvent(now, flightId, this, foi, recordTime));

		getLogger().trace("{}: New FOI added: {}; Num FOIs = {}", flightId, uid, flightFois.size());
	}
	

	@Override
	public void newPosition(FlightObject pos)
	{
		//  Should never send null pos, but check it anyway
		if(pos == null) {
			return;
		}
		// Check for and add Pos and LawBox FOIs if they aren't already in cache
		String oshFlightId = pos.getOshFlightId();
		ensureFlightFoi(oshFlightId, pos.getClock());
		FlightObject prevPos = flightPositions.get(oshFlightId);
		if(prevPos != null) {
			// Calc vert change in ft/minute
			Long prevTime = prevPos.getClock() ;
			Long newTime = pos.getClock() ;
			Double prevAlt = prevPos.getAltitude();
			Double newAlt = pos.getAltitude();
//			System.err.println(" ??? " + oshFlightId + ":" + prevAlt + "," + newAlt + "," + prevTime + "," + newTime);
			if(prevAlt != null && newAlt != null && prevTime != null && newTime != null && (!prevTime.equals(newTime)) ) {
				// check math here!!!
				pos.verticalChange = (newAlt - prevAlt)/( (newTime - prevTime)/60.);
//				System.err.println(" ***  " + oshFlightId + ":" + prevAlt + "," + newAlt + "," + prevTime + "," + newTime + " ==> " + pos.verticalChange);
			}
		}
		
		flightPositions.put(oshFlightId, pos);
		flightPositionOutput.sendPosition(pos, oshFlightId);
	}
	

	@Override
	public void newFlightPlan(FlightPlan plan)
	{
		//  Should never send null plan
		if(plan == null) {
			return;
		}
		// Add new FlightPlan FOI if new
		String oshFlightId = plan.getOshFlightId();
		ensureFlightFoi(oshFlightId, plan.issueTime);

		// send new data to outputs
		flightPlanOutput.sendFlightPlan(plan);
	}
	

    @Override
    public boolean isConnected()
    {
        return connected;
    }
    
	
	@Override
	public Collection<String> getEntityIDs()
	{
		return Collections.unmodifiableCollection(flightFois.keySet());
	}


	@Override
	public AbstractFeature getCurrentFeatureOfInterest()
	{
		return null;
	}


	@Override
	public AbstractProcess getCurrentDescription(String entityID)
	{
		return null;
	}


	@Override
	public double getLastDescriptionUpdate(String entityID)
	{
		return 0;
	}


	@Override
	public AbstractFeature getCurrentFeatureOfInterest(String entityID)
	{
		return flightFois.get(entityID);
	}


	@Override
	public Collection<? extends AbstractFeature> getFeaturesOfInterest()
	{
		return Collections.unmodifiableCollection(flightFois.values());
	}


	@Override
	public Collection<String> getFeaturesOfInterestIDs()
	{
		return Collections.unmodifiableCollection(flightFois.keySet());
	}
	

	@Override
	public Collection<String> getEntitiesWithFoi(String foiID)
	{
	    String entityID = foiID.substring(foiID.lastIndexOf(':')+1);
        return Arrays.asList(entityID);
	}
}
