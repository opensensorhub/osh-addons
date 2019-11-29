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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
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
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.flightAware.FlightAwareConfig.Mode;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Asserts;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
    private static final String STATE_FA_ID_CACHE_FILE = "fltaware_ids";
    private static final String CACHE_TIME_STAMP_PROPERTY = "timestamp";
    private static final int MAX_ID_CACHE_SIZE = 50000;
    private static final int MAX_ID_CACHE_AGE = 24; // hours
    
    // warn us if messages start stacking up- note sys clock dependence
    // TODO: allow configuration of these values
    private static final int MESSAGE_TIMER_CHECK_PERIOD = 10000; // in ms
    private static final int MESSAGE_LATENCY_WARN_LIMIT = 120; // in s
    private static final int MAX_REPLAY_DURATION = 3600*4; // in s
    private static final int MAX_RETRY_INTERVAL = 5*60; // in s
    
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

	IFlightObjectFilter flightFilter;
    IFlightRouteDecoder flightRouteDecoder;
    Cache<String, String> faIdToDestinationCache;
    ScheduledExecutorService watchDogTimer;
    FlightAwareClient firehoseClient;
    IMessageQueuePush msgQueue;
    MessageHandler msgHandler;
    boolean connected;
    int numAttempts;
    int retryInterval;  
    long lastRetryTime;
    long lastUpdatedCache = 0L;
        
    
	class WatchDogCheck implements Runnable
	{	        
	    WatchDogCheck()
	    {
	        // call next attempt here since watchdog is also restarted 
	        // for every attempt
	        nextAttempt();
	    }
	    
	    void resetAttempts()
	    {
	        numAttempts = 0;
            nextAttempt();
	    }
	    
	    void nextAttempt()
        {
	        // increase retry intervals exponentially up to MAX_RETRY_INTERVAL
	        int expInterval = (int)(config.initRetryInterval * Math.pow(2, numAttempts));
            retryInterval = Math.min(expInterval, config.maxRetryInterval);
	        lastRetryTime = System.currentTimeMillis()/1000;
            numAttempts++;
        }
	    
	    @Override
        public void run()
        {
            if (msgHandler != null)
            {
                Thread.currentThread().setName("Watchdog");
                
                long now = System.currentTimeMillis()/1000;
                long lastMsgAge = now - msgHandler.getLatestMessageReceiveTime();
                long lastMsgLag = msgHandler.getMessageTimeLag();
                long sinceLastRetry = now - lastRetryTime;
                
                // if messages are received normally
                if (lastMsgAge < config.initRetryInterval)
                {
                    resetAttempts();
                    
                    // if messages getting old (usually when we can't keep up)
                    if (lastMsgLag > MESSAGE_LATENCY_WARN_LIMIT)
                        getLogger().warn("Messages getting old. Last dated {}s ago", lastMsgLag);
                    
                    // otherwise log we are ok
                    else
                        getLogger().info("FA connection OK: Last message received {}s ago", lastMsgAge);
                    
                    getLogger().info("FA ID cache size is {}", faIdToDestinationCache.size());
                }    
                
                // if no message received for a while
                else if (sinceLastRetry > retryInterval)
                {
                    getLogger().error("No message received in the last {}s", retryInterval);
                    getLogger().error("Reconnection attempt #{}", numAttempts);                                           
                    
                    try
                    {
                        stop();
                        
                        if (config.connectionType == Mode.PUBSUB ||
                           (config.connectionType == Mode.PUBSUB_THEN_FIREHOSE && numAttempts <= 3) ||
                           (config.connectionType == Mode.FIREHOSE_THEN_PUBSUB && numAttempts > 3))
                        {
                            startWithPubSub();
                        }
                        else if (config.connectionType == Mode.FIREHOSE ||
                                (config.connectionType == Mode.FIREHOSE_THEN_PUBSUB && numAttempts <= 3) ||
                                (config.connectionType == Mode.PUBSUB_THEN_FIREHOSE && numAttempts > 3))
                        {
                            startWithFirehose();
                        }
                    }
                    catch (Exception e)
                    {
                        getLogger().error("Error during automatic restart", e);
                    }
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
		
		// init flight filter
		if (config.filterConfig != null)
            this.flightFilter = config.filterConfig.getFilter();
        		
		// init flight route decoder
		this.flightRouteDecoder = new FlightRouteDecoderFlightXML(this);
		
		// init ID cache
		this.faIdToDestinationCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_ID_CACHE_SIZE)
                .concurrencyLevel(2)
                .expireAfterWrite(MAX_ID_CACHE_AGE, TimeUnit.HOURS)
                .build();
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
	

    private void startWithFirehose() throws SensorHubException
    {
        loadIdCache();
        
        reportStatus("Connecting to Firehose channel...");
        watchDogTimer = Executors.newSingleThreadScheduledExecutor();
        
        // connect to pub/sub channel for publishing only
        if (config.pubSubConfig != null)
            connectToPubSub(true);
        
        // create message handler
        if (msgQueue != null)
            msgHandler = new MessageHandlerWithForward(this, msgQueue);
        else
            msgHandler = new MessageHandler(this);
        msgHandler.addPlanListener(this);
        msgHandler.addPositionListener(this);
        
        // configure firehose feed
        firehoseClient = new FlightAwareClient(config.hostname, config.userName, config.password, getLogger(), new IMessageHandler() {
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
        
        // if cache is too old, replay older messages
        long replayDuration = Math.min(System.currentTimeMillis()/1000 - lastUpdatedCache, MAX_REPLAY_DURATION);
        if (replayDuration > 2)
        {
            getLogger().info("FA ID cache is {}s old. Replaying historical messages.", replayDuration);
            firehoseClient.setReplayDuration(replayDuration);
        }
        
        // start firehose feed
        connected = false;
        firehoseClient.start();
        
        // start watchdog thread
        startWatchDog();
	}
    
    
    private void startWithPubSub() throws SensorHubException
    {
        Asserts.checkNotNull(config.pubSubConfig, "PubSubConfig");
        loadIdCache();
        
        reportStatus("Connecting to Pub/Sub channel...");
        watchDogTimer = Executors.newSingleThreadScheduledExecutor();
        
        // create message handler
        msgHandler = new MessageHandler(this);
        msgHandler.addPlanListener(this);
        msgHandler.addPositionListener(this);
        
        // start pub/sub receiver
        connectToPubSub(false);
        
        // start watchdog thread
        startWatchDog();
    }
    
    
    private void startWatchDog()
    {     
        watchDogTimer.scheduleWithFixedDelay(new WatchDogCheck(),
                config.initRetryInterval*1000,
                MESSAGE_TIMER_CHECK_PERIOD,
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
                    public void receive(Map<String, String> attrs, byte[] payload)
                    {
                        if (!connected)
                        {
                            reportStatus("Connected to Pub/Sub channel");
                            connected = true;
                        }
                        
                        msgHandler.handle(new String(payload, StandardCharsets.UTF_8));
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
	public void stop() throws SensorHubException
	{
		if (watchDogTimer != null) {
		    watchDogTimer.shutdown();
            watchDogTimer = null;
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
	    
	    // also save state on stop
	    saveIdCache();
	    faIdToDestinationCache.invalidateAll();
	}
	
	
	private String getOshFlightId(FlightObject fltObj)
	{
	    return fltObj.ident + '_' + fltObj.dest;
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
	public void newPosition(FlightObject fltPos)
	{
		//  Should never send null pos, but check it anyway
		if(fltPos == null)
			return;
		
		// Check for and add Pos and LawBox FOIs if they aren't already in cache
		String oshFlightId = getOshFlightId(fltPos);
		ensureFlightFoi(oshFlightId, fltPos.getClock());
		FlightObject prevPos = flightPositions.get(oshFlightId);
		if(prevPos != null) {
			// Calc vert change in ft/minute
			Long prevTime = prevPos.getClock() ;
			Long newTime = fltPos.getClock() ;
			Double prevAlt = prevPos.getAltitude();
			Double newAlt = fltPos.getAltitude();
//			System.err.println(" ??? " + oshFlightId + ":" + prevAlt + "," + newAlt + "," + prevTime + "," + newTime);
			if(prevAlt != null && newAlt != null && prevTime != null && newTime != null && (!prevTime.equals(newTime)) ) {
				// check math here!!!
			    fltPos.verticalChange = (newAlt - prevAlt)/( (newTime - prevTime)/60.);
//				System.err.println(" ***  " + oshFlightId + ":" + prevAlt + "," + newAlt + "," + prevTime + "," + newTime + " ==> " + pos.verticalChange);
			}
		}
		
		flightPositions.put(oshFlightId, fltPos);
		flightPositionOutput.sendPosition(oshFlightId, fltPos);
	}
	

	@Override
	public void newFlightPlan(FlightObject fltPlan)
	{
		//  Should never send null plan
		if (fltPlan == null)
			return;
		
		// Add new FlightPlan FOI if new
		String oshFlightId = getOshFlightId(fltPlan);
		long issueTime = Long.parseLong(fltPlan.pitr)*1000;
		ensureFlightFoi(oshFlightId, issueTime);

		// send new data to outputs
		flightPlanOutput.sendFlightPlan(oshFlightId, fltPlan);
	}

    
    protected void loadIdCache() throws SensorHubException
    {
        IModuleStateManager stateMgr = SensorHub.getInstance().getModuleRegistry().getStateManager(getLocalID());
        
        // preload cache from file
        InputStream is = stateMgr.getAsInputStream(STATE_FA_ID_CACHE_FILE);
        if (is != null)
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    String[] values = line.split("=");
                    if (values.length != 2)
                        continue;
                    
                    if (CACHE_TIME_STAMP_PROPERTY.equals(values[0]))
                        this.lastUpdatedCache = Long.parseLong(values[1]);
                    else
                        faIdToDestinationCache.put(values[0], values[1]);
                }
            }
            catch (IOException e)
            {
                throw new SensorHubException("Error while saving state", e);
            }
            
            getLogger().info("Preloaded {} entries to FA ID cache", faIdToDestinationCache.size());
        }
    }
    
    
    protected void saveIdCache() throws SensorHubException
    {
        // save ID cache for hot restart
        if (faIdToDestinationCache != null && faIdToDestinationCache.size() > 0)
        {
            IModuleStateManager stateMgr = SensorHub.getInstance().getModuleRegistry().getStateManager(getLocalID());
            
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stateMgr.getOutputStream(STATE_FA_ID_CACHE_FILE))))
            {
                getLogger().info("Saving FA ID cache");
                
                for (Entry<String,String> entry: faIdToDestinationCache.asMap().entrySet())
                {
                    writer.append(entry.getKey()).append("=").append(entry.getValue());
                    writer.newLine();
                }
                
                // include timestamp
                this.lastUpdatedCache = System.currentTimeMillis()/1000;
                writer.append(CACHE_TIME_STAMP_PROPERTY).append("=").append(Long.toString(lastUpdatedCache));
                writer.newLine();
            }
            catch (IOException e)
            {
                throw new SensorHubException("Error while saving state", e);
            }
            
            getLogger().info("Saved {} entries from FA ID cache", faIdToDestinationCache.size());
        }
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
