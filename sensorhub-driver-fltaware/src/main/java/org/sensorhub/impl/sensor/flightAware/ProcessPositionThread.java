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

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ProcessPositionThread implements Runnable
{
    static final Logger log = LoggerFactory.getLogger(ProcessPositionThread.class);
    
    FlightObject obj;
	private FlightAwareApi api = new FlightAwareApi();
	FlightAwareConverter converter;
	Cache<String, String> idToDestinationCode;
	
	public ProcessPositionThread(FlightAwareConverter converter, FlightObject obj) {
		this.obj = obj;
		this.converter = converter;
		
		idToDestinationCode = CacheBuilder.newBuilder()
    	       .maximumSize(10000)
    	       .expireAfterWrite(24, TimeUnit.HOURS)
    	       .build();
	}

	@Override
	public void run() {
		String dest = null;
		if(obj.ident == null || obj.ident.length() == 0) {
			log.debug("obj.ident is empty or null.  Cannot construct oshFlightId for Position");
			return;
		}
		
		obj.dest = idToDestinationCode.getIfPresent(obj.id);
		if(obj.dest == null || obj.dest.length() == 0) {
			log.debug("No destination airport associated to position. Fetching from Flight Aware API...");
			// Position message from FltAware did not contain dest airport.  Try to pull it from API
			try {
				String json = api.invokeNew(FlightAwareApi.InFlightInfo_URL, "ident=" + obj.ident);
				InFlightInfo info = (InFlightInfo) FlightAwareApi.fromJson(json, InFlightInfo.class);
				dest = info.InFlightInfoResult.destination;
				//getParentModule().
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
			if(dest == null || dest.length() == 0) {
				log.error("STILL Cannot construct oshFlightId for Position. Missing dest in Position and InFlightInfo API response for: {}", obj.ident );
				return;
			}
			
			obj.dest = dest;
			idToDestinationCode.put(obj.id, obj.dest);
		}
		converter.newFlightPosition(obj);
	}

}
