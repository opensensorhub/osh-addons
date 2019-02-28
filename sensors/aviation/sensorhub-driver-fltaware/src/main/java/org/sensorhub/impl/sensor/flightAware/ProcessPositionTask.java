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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessPositionTask implements Runnable
{
    static final Logger log = LoggerFactory.getLogger(ProcessPositionTask.class);
    
    FlightObject obj;
	FlightAwareApi api;
	MessageHandler converter;
	
	public ProcessPositionTask(MessageHandler converter, FlightObject obj) {
		this.obj = obj;
		this.converter = converter;
		this.api = new FlightAwareApi(converter.user, converter.passwd);
	}

	@Override
	public void run() {
		String dest = null;
		if (obj.ident == null || obj.ident.length() == 0) {
			log.error("obj.ident is empty or null.  Cannot construct oshFlightId for Position");
			return;
		}
		
		if (obj.dest == null || obj.dest.length() == 0) {		    
		    // Position message from FlightAware did not contain dest airport
		    log.trace("{}: Position message without destination", obj.ident);
		    
		    // try to fetch from cache
		    obj.dest = converter.idToDestinationCache.getIfPresent(obj.id);  
		    
		    // if not in cache, try to pull it from FlightAware API
		    if (obj.dest == null)
		    {
		        String json = null;
		        try {
    				json = api.invokeNew(FlightAwareApi.InFlightInfo_URL, "ident=" + obj.ident);
    				InFlightInfo info = (InFlightInfo) FlightAwareApi.fromJson(json, InFlightInfo.class);
    				dest = info.InFlightInfoResult.destination;
    				log.trace("{}: Fetched destination from FA API: {}", obj.ident, dest);
    			} catch (Exception e) {
    				log.error("{}: Cannot get InFlightInfo for from FA API. Error: {}", obj.ident, json, e);
    			}
		        
    			if(dest == null || dest.length() == 0) {
    				log.error("STILL Cannot construct oshFlightId for Position. Missing dest in InFlightInfo response");
    				return;
    			}
    			
    			obj.dest = dest;
    			converter.idToDestinationCache.put(obj.id, obj.dest);
		    }
		    else
		        log.trace("{}: Fetched destination from cache: {}", obj.ident, obj.dest);
		}		    
		
		converter.newFlightPosition(obj);
	}

}
