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

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.vast.util.Asserts;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;


/**
 * Process Flight Plan messages from FlightAware firehose feed.
 * Note that because of the way FlightAware feed and API work, 
 * we have to pull some info from the Firehose message (airports, departTime, issueTime)
 * and some from the API (actual waypoints) 
 *
 * @author tcook
 */
public class ProcessPlanTask implements Runnable
{
    Logger log;    
    FlightObject fltObj;
	MessageHandler msgHandler;
    Cache<String, String> faIdToDestinationCache;
	IFlightRouteDecoder flightRouteDecoder;
	
	public ProcessPlanTask(MessageHandler msgHandler, FlightObject fltObj) {
	    this.log = msgHandler.log;
		this.msgHandler = msgHandler;
		this.faIdToDestinationCache = Asserts.checkNotNull(msgHandler.driver.faIdToDestinationCache, Cache.class);
		this.flightRouteDecoder = Asserts.checkNotNull(msgHandler.driver.flightRouteDecoder, IFlightRouteDecoder.class);
		this.fltObj = fltObj;		
	}
	
	@Override
	public void run() {
		try {	
		    // save flight destination airport so we can look it up
		    // when it's missing from position messages
		    if (fltObj.dest != null && fltObj.dest.trim().length() > 0)
		    {
    		    faIdToDestinationCache.get(fltObj.id, new Callable<String>() {
                    @Override
                    public String call() throws Exception
                    {
                        log.trace("{}_{}: Adding to cache, key={}", fltObj.ident, fltObj.dest, fltObj.id);
                        return fltObj.dest;
                    }		        
    		    });
		    }
		    
		    // decode only real-time flight plan messages
		    //if (!msgHandler.isReplay())
		    {		        
		        // cannot decode if airport codes or route are missing
	            if (Strings.isNullOrEmpty(fltObj.orig) ||
	                Strings.isNullOrEmpty(fltObj.dest) ||
	                Strings.isNullOrEmpty(fltObj.route))
	                return;
	            
	            // keep only flight plans produced by airlines
	            if (Strings.isNullOrEmpty(fltObj.facility_name) || !fltObj.facility_name.equals("Airline"))
	                return;
	            
		        if (flightRouteDecoder.decode(fltObj))
    		        msgHandler.newFlightPlan(fltObj);
		    }
		} catch (Exception e) {
			log.error("Error while decoding flight plan", e);
		} 	
	}

}
