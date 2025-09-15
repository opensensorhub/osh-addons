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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.sensorhub.api.comm.IMessageQueuePush;
import org.sensorhub.api.comm.IMessageQueuePush.MessageListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.flightAware.FlightAwareConfig.Mode;
import org.sensorhub.utils.aero.INavDatabase;
import org.sensorhub.utils.aero.impl.AeroUtils;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Asserts;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.opengis.gml.v32.impl.GMLFactory;


/**
 * 
 * @author tcook
 * @since Oct 1, 2017
 */
public class FlightAwareDriver extends AbstractSensorModule<FlightAwareConfig>
{
    private static final String STATE_CACHE_FILE = "fltaware_cache";
    private static final int MAX_CACHE_SIZE = 50000;
    private static final int MAX_CACHE_AGE = 24; // hours
    
    private static final int MESSAGE_TIMER_CHECK_PERIOD = 10; // in s
    private static final int MESSAGE_LATENCY_WARN_LIMIT = 120; // in s
    private static final int MAX_REPLAY_DURATION = 3600*4; // in s
    private static final int CACHE_SAVE_MULTIPLIER = 3; // in number of timer checks
    
    FlightPlanOutput flightPlanOutput;
    FlightPositionOutput flightPositionOutput;
    
    // Helpers
    SMLHelper smlFac = new SMLHelper();
    GMLFactory gmlFac = new GMLFactory(true);

    // Dynamically created FOIs
    static final String SENSOR_UID_PREFIX = "urn:osh:sensor:aviation:";
    static final String FLIGHT_UID_PREFIX = "urn:osh:aviation:flight:";

    IFlightObjectFilter flightFilter;
    IFlightRouteDecoder flightRouteDecoder;
    Cache<String, FlightInfo> flightCache;
    ScheduledExecutorService watchDogTimer;
    FlightAwareClient firehoseClient;
    IMessageQueuePush<?> msgQueue;
    MessageHandler msgHandler;
    volatile boolean connected;
    int numAttempts;
    int retryInterval;  
    long lastRetryTime;
    long lastUpdatedCache = 0L;
    
    
    static class FlightInfo
    {
        String dest;
        String route;
    }
        
    
    class WatchDogCheck implements Runnable
    {            
        int callsLeftBeforeCacheSave = CACHE_SAVE_MULTIPLIER;
        
        
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
                    
                    getLogger().debug("Cache size={}", flightCache.size());
                }
                
                // if no message received for a while
                else if (sinceLastRetry >= retryInterval)
                {
                    getLogger().error("No message received in the last {}s. Reconnecting...", lastMsgAge);
                    getLogger().error("Reconnection attempt #{}", numAttempts);
                    
                    try
                    {
                        doStop();
                        
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
                
                else
                {
                    reportStatus("No message received in the last " + lastMsgAge + "s. "
                            + "Will attempt reconnection in " + (retryInterval-sinceLastRetry) + "s");
                }
                
                callsLeftBeforeCacheSave--;
                if (callsLeftBeforeCacheSave <= 0)
                {
                    try
                    {
                        saveCache();
                        callsLeftBeforeCacheSave = CACHE_SAVE_MULTIPLIER;
                    }
                    catch (SensorHubException e)
                    {
                        getLogger().error("Cannot save cache", e);
                    }
                }
            }
        }
    }
    

	public FlightAwareDriver()
	{
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
    protected void doInit() throws SensorHubException
	{
		// IDs
		this.uniqueID = AeroUtils.AERO_SYSTEM_URI_PREFIX + "flightAware";
		this.xmlID = "FLTAWARE_FIREHOSE";

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
        
        // init flight cache
        this.flightCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .concurrencyLevel(2)
                .expireAfterAccess(MAX_CACHE_AGE, TimeUnit.HOURS)
                .build();
                
        if (config.decodeRoutes)
        {
            // init flight route decoder
            var navDB = INavDatabase.getInstance(getParentHub());
            if (navDB != null)
                this.flightRouteDecoder = new FlightRouteDecoderNavDb(this, navDB);
            else
                this.flightRouteDecoder = new FlightRouteDecoderFlightXML(this);
        }
    }
    

	@Override
	protected void doStart() throws SensorHubException
	{
	    // if configured to use firehose only, connect to firehose now
        if (config.connectionType == Mode.FIREHOSE || config.connectionType == Mode.FIREHOSE_THEN_PUBSUB)
            startWithFirehose();
        else
            startWithPubSub();
    }
    

    private void startWithFirehose() throws SensorHubException
    {
        loadCache();
        
        reportStatus("Connecting to Firehose channel...");
        watchDogTimer = Executors.newSingleThreadScheduledExecutor();
        
        // connect to pub/sub channel for publishing only
        if (config.pubSubConfig != null && config.pubSubConfig.enablePublish)
            connectToPubSub(true);
        
        // create message handler
        if (msgQueue != null)
            msgHandler = new MessageHandlerWithForward(this, msgQueue);
        else
            msgHandler = new MessageHandler(this);
        
        // set outputs as listeners
        if (flightPlanOutput != null)
            msgHandler.addPlanListener(flightPlanOutput);
        if (flightPositionOutput != null)
            msgHandler.addPositionListener(flightPositionOutput);
        
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
            getLogger().info("Cache is {}s old. Replaying historical messages.", replayDuration);
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
        loadCache();
        
        reportStatus("Connecting to Pub/Sub channel...");
        watchDogTimer = Executors.newSingleThreadScheduledExecutor();
        
        // create message handler
        msgHandler = new MessageHandler(this);
        
        // set outputs as listeners
        if (flightPlanOutput != null)
            msgHandler.addPlanListener(flightPlanOutput);
        if (flightPositionOutput != null)
            msgHandler.addPositionListener(flightPositionOutput);
        
        // start pub/sub receiver
        connectToPubSub(false);
        
        // start watchdog thread
        startWatchDog();
    }
    
    
    private void startWatchDog()
    {     
        watchDogTimer.scheduleWithFixedDelay(new WatchDogCheck(),
                config.initRetryInterval,
                MESSAGE_TIMER_CHECK_PERIOD,
                TimeUnit.SECONDS);
    }
    
    
    private void connectToPubSub(boolean publishOnly)
    {
        try
        {
            // load message queue implementation
            var moduleReg = getParentHub().getModuleRegistry();
	        msgQueue = (IMessageQueuePush<?>)moduleReg.loadSubModule(this, config.pubSubConfig, true);
            
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
            msgQueue = null;
            throw new IllegalStateException("Cannot start message queue", e);
        }
    }
    

	@Override
	protected void doStop() throws SensorHubException
	{
		if (watchDogTimer != null) {
		    watchDogTimer.shutdown();
            watchDogTimer = null;
        }
        
        if (msgQueue != null) {
            msgQueue.stop();
            msgQueue = null;
        }
        
        if (firehoseClient != null) {
            firehoseClient.stop();
            firehoseClient = null;
        }
        
        if (msgHandler != null) {
            msgHandler.stop();
            msgHandler = null;
        }
        
        // also save state on stop
        saveCache();
        if (flightCache != null)
            flightCache.invalidateAll();
    }

    
    protected void loadCache() throws SensorHubException
    {
        IModuleStateManager stateMgr = getParentHub().getModuleRegistry().getStateManager(getLocalID());
        
        // preload cache from file
        InputStream is = stateMgr.getAsInputStream(STATE_CACHE_FILE);
        if (is != null)
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    String[] values = line.split(",");
                    if (values.length < 2)
                    {
                        getLogger().error("Invalid cache entry: {}", line);
                        continue;
                    }
                    
                    FlightInfo info = new FlightInfo();
                    info.dest = values[1];
                    info.route = "null".equals(values[2]) ? null : values[2];
                    flightCache.put(values[0], info);
                }
                
                // read file last modified time stamp
                File moduleDataFolder = getParentHub().getModuleRegistry().getModuleDataFolder(getLocalID());
                this.lastUpdatedCache = new File(moduleDataFolder, STATE_CACHE_FILE+".dat").lastModified()/1000;
            }
            catch (IOException e)
            {
                throw new SensorHubException("Error while saving state", e);
            }
            
            getLogger().info("Loaded {} entries to cache. Last modified on {}", flightCache.size(), Instant.ofEpochSecond(lastUpdatedCache));
        }
    }
    
    
    protected void saveCache() throws SensorHubException
    {
        // save ID cache for hot restart
        if (flightCache != null && flightCache.size() > 0)
        {
            IModuleStateManager stateMgr = getParentHub().getModuleRegistry().getStateManager(getLocalID());
            
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stateMgr.getOutputStream(STATE_CACHE_FILE))))
            {
                for (Entry<String,FlightInfo> entry: flightCache.asMap().entrySet())
                {
                    writer.append(entry.getKey()).append(',')
                          .append(entry.getValue().dest).append(',')
                          .append(entry.getValue().route);
                    writer.newLine();
                }
            }
            catch (IOException e)
            {
                throw new SensorHubException("Error while saving state", e);
            }
            
            getLogger().info("Saved cache state: {} entries", flightCache.size());
        }
    }
    

    @Override
    public boolean isConnected()
    {
        return connected;
    }
}
