/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2017 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.flightAware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class FlightAwareConverter implements FlightObjectListener
{
	static final Logger log = LoggerFactory.getLogger(FlightAwareConverter.class);
	//  Do I really need to retain in maps?  Or just let listener classes deal with that
    List<FlightPlanListener> planListeners = new ArrayList<>();
    List<PositionListener> positionListeners = new ArrayList<>();
    Cache<String, String> idToDestinationCache;
    ExecutorService exec = Executors.newFixedThreadPool(2);
    String user;
    String passwd;
    
    public FlightAwareConverter(String user, String pwd) {
        this.user = user;
        this.passwd = pwd;
        this.idToDestinationCache = CacheBuilder.newBuilder()
               .maximumSize(10000)
               .concurrencyLevel(2)
               .expireAfterWrite(24, TimeUnit.HOURS)
               .build();
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

    public boolean removePositiobListener(PositionListener l) {
    	return positionListeners.remove(l);
    }
	
	@Override
	public void processMessage(FlightObject obj) {
		// call api and get flightplan
		if(!obj.type.equals("flightplan") && !obj.type.equals("position") ) {
			log.warn("FlightAwareConverter does not yet support: " + obj.type);
			return;
		}

		switch(obj.type) {
    		case "flightplan":
    		    exec.execute(new ProcessPlanThread(this, obj));
    			break;
    		case "position":
    		    exec.execute(new ProcessPositionThread(this, obj));
    			break;
    		default:
    			log.error("Unknown message slipped through somehow: " + obj.type);
		}
	}

	protected void newFlightPlan(FlightPlan plan) {
		for(FlightPlanListener l: planListeners)
			l.newFlightPlan(plan);
	}
	
	protected void newFlightPosition(FlightObject pos) {
		for(PositionListener l: positionListeners)
			l.newPosition(pos);
	}

}
