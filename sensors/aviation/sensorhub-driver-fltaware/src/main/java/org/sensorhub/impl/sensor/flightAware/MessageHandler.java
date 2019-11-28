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
import org.slf4j.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class MessageHandler implements IMessageHandler
{
	static final String POSITION_MSG_TYPE = "position";
	static final String FLIGHTPLAN_MSG_TYPE = "flightplan";
    static final String ARRIVAL_MSG_TYPE = "arrival";
	static final long MESSAGE_LATENCY_WARN_LIMIT = 30000L; // in ms
	
	Logger log;
	FlightAwareDriver driver;
	Gson gson = new GsonBuilder().setPrettyPrinting().create();
    List<FlightObjectListener> objectListeners = new ArrayList<>();
    List<FlightPlanListener> planListeners = new ArrayList<>();
    List<PositionListener> positionListeners = new ArrayList<>();
    IFlightObjectFilter flightFilter;
    BlockingQueue<Runnable> posQueue, fpQueue;
    ExecutorService posExec, fpExec;
    volatile long latestMessageReceiveTime = 0L; // in seconds
    volatile long latestMessageTimeStamp = 0L; // in seconds
    volatile long latestMessageTimeLag = 0L; // in seconds
    int msgCount = 0;
    boolean liveStarted = false;
    
    public MessageHandler(FlightAwareDriver driver) {
        this.driver = driver;
        this.log = driver.getLogger();
        this.flightFilter = driver.flightFilter;
        
        // keep flight plan processing sequential because it's lower throughput
        // and so we can properly detect duplicates
        this.fpQueue = new LinkedBlockingQueue<>(10000);
        this.fpExec = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, fpQueue);
        
        // process position messages in parallel
        this.posQueue = new LinkedBlockingQueue<>(10000);
        this.posExec = new ThreadPoolExecutor(2, 4, 1, TimeUnit.SECONDS, posQueue);
    }
        
    public void handle(String message) {
        try {
            latestMessageReceiveTime = System.currentTimeMillis()/1000;
            FlightObject obj = gson.fromJson(message, FlightObject.class);
            //if ("DAL595-1571978754-airline-0325".equals(obj.id))
                //System.out.println(message);
            latestMessageTimeStamp = Long.parseLong(obj.pitr);
            latestMessageTimeLag = latestMessageReceiveTime - latestMessageTimeStamp;
            
            log.trace("message count: {}, queue size: {}", ++msgCount, posQueue.size());
            log.trace("time lag: {}", latestMessageTimeLag);
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
            if (flightFilter != null && !flightFilter.test(obj))
                return;
            
            // process message
            processMessage(obj);
            
            // also notify raw object listeners
            newFlightObject(obj);
            
        } catch (Exception e) {
            log.error("Cannot read JSON\n{}", message, e);
            if (latestMessageTimeStamp == 0L)
                throw new IllegalStateException(message);
            return;
        }
    }

    private void processMessage(FlightObject obj) {
        switch (obj.type) {
            case FLIGHTPLAN_MSG_TYPE:
                fpExec.execute(new ProcessPlanTask(this, obj));
                break;
            case POSITION_MSG_TYPE:
                if (!isReplay()) // skip replayed position messages
                    posExec.execute(new ProcessPositionTask(this, obj));
                break;
            case ARRIVAL_MSG_TYPE:
                //log.info("{}_{} arrived at {}", obj.ident, obj.dest, Instant.ofEpochSecond(Long.parseLong(obj.aat)));
                break;
            default:
                log.warn("Unsupported message type: {}", obj.type);
                break;
        }
    }
    
    public void stop() {
        posExec.shutdownNow();
        fpExec.shutdownNow();
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

    protected void newFlightObject(FlightObject obj) {
        for (FlightObjectListener l: objectListeners)
            l.processMessage(obj);
    }

	protected void newFlightPlan(FlightObject obj, FlightPlan plan) {
		for (FlightPlanListener l: planListeners)
			l.newFlightPlan(plan);
	}
	
	protected void newFlightPosition(FlightObject pos) {
		for (PositionListener l: positionListeners)
			l.newPosition(pos);
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
