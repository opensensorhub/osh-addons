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
import org.slf4j.LoggerFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class MessageHandler implements IMessageHandler
{
	static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
	static final String POSITION_MSG_TYPE = "position";
	static final String FLIGHTPLAN_MSG_TYPE = "flightplan";
	static final long MESSAGE_LATENCY_WARN_LIMIT = 30000L; // in ms
	
	Gson gson = new GsonBuilder().setPrettyPrinting().create();
    List<FlightObjectListener> objectListeners = new ArrayList<>();
    List<FlightPlanListener> planListeners = new ArrayList<>();
    List<PositionListener> positionListeners = new ArrayList<>();
    Cache<String, String> idToDestinationCache;
    BlockingQueue<Runnable> queue;
    ExecutorService exec;
    String user;
    String passwd;
    volatile long lastMessageReceiveTime = 0L;
    volatile long lastMessageTime = 0L;
    
    
    public MessageHandler(String user, String pwd) {
        this.user = user;
        this.passwd = pwd;
        this.idToDestinationCache = CacheBuilder.newBuilder()
               .maximumSize(10000)
               .concurrencyLevel(2)
               .expireAfterWrite(24, TimeUnit.HOURS)
               .build();
        
        this.queue = new LinkedBlockingQueue<>(10000);
        this.exec = new ThreadPoolExecutor(2, 4, 1, TimeUnit.SECONDS, queue);
    }
    
    public void handle(String message) {
        try {
            lastMessageReceiveTime = System.currentTimeMillis();
            
            FlightObject obj = gson.fromJson(message, FlightObject.class);
            lastMessageTime = Long.parseLong(obj.pitr);            
            processMessage(obj);
            
            // also notify raw object listeners
            newFlightObject(obj);
            
        } catch (Exception e) {
            log.error("Cannot read JSON\n{}", message, e);
            if (lastMessageTime == 0L)
                throw new IllegalStateException(message);
            return;
        }
    }

    private void processMessage(FlightObject obj) {
        switch (obj.type) {
            case FLIGHTPLAN_MSG_TYPE:
                exec.execute(new ProcessPlanTask(this, obj));
                break;
            case POSITION_MSG_TYPE:
                exec.execute(new ProcessPositionTask(this, obj));
                break;
            default:
                log.warn("Unsupported message type: {}", obj.type);
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

    protected void newFlightObject(FlightObject obj) {
        for (FlightObjectListener l: objectListeners)
            l.processMessage(obj);
    }

	protected void newFlightPlan(FlightPlan plan) {
		for (FlightPlanListener l: planListeners)
			l.newFlightPlan(plan);
	}
	
	protected void newFlightPosition(FlightObject pos) {
		for (PositionListener l: positionListeners)
			l.newPosition(pos);
	}

    public long getLastMessageReceiveTime() {
        return lastMessageReceiveTime;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

}
