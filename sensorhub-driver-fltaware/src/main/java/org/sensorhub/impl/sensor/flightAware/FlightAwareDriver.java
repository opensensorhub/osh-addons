/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

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
                long lastMsgTime = msgHandler.getLastMessageTime();
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
                    if (config.connectionType != Mode.PUBSUB_ONLY)
                        startWithFirehose();
                    else
                        startWithPubSub();
                }
                
                // if connection to firehose didn't succeed
                else if (firehoseClient != null && !firehoseClient.isStarted())
                {
                    getLogger().error("Could not connect to Firehose");
                    stop();
                    
                    if (retriesLeft <= 0)
                    {
                        getLogger().error("Max number of retries reached");
                        return;
                    }
                    
                    retriesLeft--;
                    if (config.connectionType != Mode.FIREHOSE_ONLY)
                        startWithPubSub();
                    else
                        startWithFirehose();
                }
                
                // if initially connected but no message received for some time
                else if (lastMsgDelta > MESSAGE_RECEIVE_TIMEOUT)
                {
                    getLogger().error("No message received in the last {}s", MESSAGE_RECEIVE_TIMEOUT/1000);                        
                    if (firehoseClient != null)
                        firehoseClient.restart();
                }
                
                // if messages getting old
                else if (lastMsgAge > MESSAGE_LATENCY_WARN_LIMIT)
                {
                    getLogger().warn("Messages getting old. Last dated {}s ago", lastMsgAge);
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
        if (config.connectionType == Mode.FIREHOSE_ONLY || config.connectionType == Mode.FIREHOSE_THEN_PUBSUB)
            startWithFirehose();
        else
            startWithPubSub();
	}
	

    private void startWithFirehose()
    {
        getLogger().info("Connecting to Firehose channel");
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
        firehoseClient = new FlightAwareClient(config.hostname, config.userName, config.password, msgHandler);
        for(String mt: config.messageTypes)
            firehoseClient.addMessageType(mt);
        for(String airline: config.airlines)
            firehoseClient.addAirline(airline);
        
        // start firehose feed
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
        
        getLogger().info("Connecting to Pub/Sub channel");
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
	    // load message queue implementation
	    try
	    {
            msgQueue = (IMessageQueuePush)SensorHub.getInstance().getModuleRegistry().loadClass(config.pubSubConfig.moduleClass);
            msgQueue.init(config.pubSubConfig);
            
            if (!publishOnly)
            {
                msgQueue.registerListener(new MessageListener() {
                    @Override
                    public void receive(byte[] msg)
                    {
                        if (!connected)
                        {
                            getLogger().info("Connected to Pub/Sub channel");
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
	    catch (SensorHubException e)
	    {
            throw new IllegalStateException("Cannot load message queue implementation", e);
        }
	}
	

	@Override
	public void stop()
	{
		if (timer != null) {
		    try
            {
		        timer.shutdownNow();
		        timer.awaitTermination(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
		    
		    timer = null;
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
        return false;
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
