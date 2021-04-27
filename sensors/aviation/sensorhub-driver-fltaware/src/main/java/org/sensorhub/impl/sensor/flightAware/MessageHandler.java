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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.sensorhub.utils.NamedThreadFactory;
import org.slf4j.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class MessageHandler implements IMessageHandler
{
	static final String POSITION_MSG_TYPE = "position";
	static final String FLIGHTPLAN_MSG_TYPE = "flightplan";
    static final String ARRIVAL_MSG_TYPE = "arrival";
    static final String KEEPALIVE_MSG_TYPE = "keepalive";
	static final long MESSAGE_LATENCY_WARN_LIMIT = 30000L; // in ms
	
	Logger log;
	FlightAwareDriver driver;
	Gson gson = new GsonBuilder().create();
    List<FlightObjectListener> objectListeners = new ArrayList<>();
    List<FlightPlanListener> planListeners = new ArrayList<>();
    List<PositionListener> positionListeners = new ArrayList<>();
    IFlightObjectFilter flightFilter;
    BlockingQueue<Runnable> execQueue;
    ExecutorService exec;
    volatile long latestMessageReceiveTime = System.currentTimeMillis()/1000; // in seconds
    volatile long latestMessageTimeStamp = 0L; // in seconds
    volatile long latestMessageTimeLag = 0L; // in seconds
    int msgCount = 0;
    boolean liveStarted = false;
    
    
    public MessageHandler(FlightAwareDriver driver) {
        this.driver = driver;
        this.log = driver.getLogger();
        this.flightFilter = driver.flightFilter;
        
        // executor to process messages in parallel
        this.execQueue = new LinkedBlockingQueue<>(10000);
        this.exec = new ThreadPoolExecutor(2, 4, 1, TimeUnit.SECONDS, execQueue, new NamedThreadFactory("MsgHandlerPool"));
    }
        
    public void handle(String message) {
        try {
            latestMessageReceiveTime = System.currentTimeMillis()/1000;
            FlightObject fltObj = gson.fromJson(message, FlightObject.class);
            fltObj.json = message;
            latestMessageTimeStamp = Long.parseLong(fltObj.pitr);
            latestMessageTimeLag = latestMessageReceiveTime - latestMessageTimeStamp;
           
            if (log.isTraceEnabled())
            {
                log.trace("New message:\n{}",  message);
                log.trace("message count: {}, queue size: {}", ++msgCount, execQueue.size());
                log.trace("time lag: {}", latestMessageTimeLag);
            }
            
            if (!liveStarted && latestMessageTimeLag < 10)
            {
                liveStarted = true;
                log.info("Starting live feed");
            }
            else if (msgCount == 1)
            {
                log.info("Replaying historical feed");
            }
            
            // skip message if filtered
            if (fltObj.ident != null && flightFilter != null && !flightFilter.test(fltObj))
                return;
            
            // notify raw object listeners
            newFlightObject(fltObj);
            
            // process message
            processMessage(fltObj);
            
        } catch (Exception e) {
            log.error("Cannot read JSON\n{}", message, e);
            if (latestMessageTimeStamp == 0L)
                throw new IllegalStateException(message);
            return;
        }
    }

    private void processMessage(FlightObject fltObj) {
        switch (fltObj.type) {
            case FLIGHTPLAN_MSG_TYPE:
                exec.execute(new ProcessPlanTask(this, fltObj));
                break;
            case POSITION_MSG_TYPE:
                if (!isReplay()) // skip replayed position messages
                    exec.execute(new ProcessPositionTask(this, fltObj));
                break;
            case ARRIVAL_MSG_TYPE:
                //log.info("{}_{} arrived at {}", obj.ident, obj.dest, Instant.ofEpochSecond(Long.parseLong(obj.aat)));
                break;
            case KEEPALIVE_MSG_TYPE:
                break;
            default:
                log.warn("Unsupported message type: {}", fltObj.type);
                break;
        }
    }
    
    public void stop() {
        exec.shutdownNow();
    }

    public void addObjectListener(FlightObjectListener l) {
    	objectListeners.add(l);
    }

    public boolean removeObjectListener(FlightObjectListener l) {
        return objectListeners.remove(l);
    }

    public void addPlanListener(FlightPlanListener l) {
        planListeners.add(l);
    }

    public boolean removePlanListener(FlightPlanListener l) {
    	return planListeners.remove(l);
    }
	
    public void addPositionListener(PositionListener l) {
    	positionListeners.add(l);
    }

    public boolean removePositionListener(PositionListener l) {
    	return positionListeners.remove(l);
    }

    protected void newFlightObject(FlightObject fltObj) {
        for (FlightObjectListener l: objectListeners)
            l.processMessage(fltObj);
    }

	protected void newFlightPlan(FlightObject fltPlan) {
		for (FlightPlanListener l: planListeners)
			l.newFlightPlan(fltPlan);
	}
	
	protected void newFlightPosition(FlightObject fltPos) {
		for (PositionListener l: positionListeners)
			l.newPosition(fltPos);
	}

    public long getLatestMessageReceiveTime() {
        return latestMessageReceiveTime;
    }

    public long getLatestMessageTime() {
        return latestMessageTimeStamp;
    }
    
    public long getMessageTimeLag() {
        return latestMessageTimeLag;
    }
    
    public boolean isReplay() {
        return !liveStarted;
    }

}
