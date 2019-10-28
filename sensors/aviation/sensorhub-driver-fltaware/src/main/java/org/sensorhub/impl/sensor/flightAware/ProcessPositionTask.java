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

/**
 * Process Position messages from FlightAware firehose feed.
 * Messages sometimes omit the destination airport so we have to look it up
 * in a map of FA ID -> destination airport code.
 * 
 * @author tcook
 */
public class ProcessPositionTask implements Runnable
{
    Logger log;
    FlightObject obj;
	FlightAwareApi api;
	MessageHandler msgHandler;
	
	public ProcessPositionTask(MessageHandler msgHandler, FlightObject obj) {
        this.log = LoggerFactory.getLogger(msgHandler.log.getName() + ":" + getClass().getSimpleName());
		this.obj = obj;
		this.msgHandler = msgHandler;
	}

	@Override
	public void run() {
		if (obj.ident == null || obj.ident.length() == 0) {
			log.error("obj.ident is empty or null.");
			return;
		}
		
		if (obj.dest == null || obj.dest.length() == 0) {		    
		    // Position message from FlightAware did not contain dest airport
		    log.trace("** {}: Position message without destination", obj.ident);
		    
		    // try to fetch from cache
		    obj.dest = msgHandler.faIdToDestinationCache.getIfPresent(obj.id);  
		    if (obj.dest == null)
		    {
		        if (!msgHandler.isReplay())
		            log.debug("** {}: Destination airport not found in cache", obj.id);
		        return;
		    }
		}
		
		log.trace("{}_{}: New position received", obj.ident, obj.dest);
		msgHandler.newFlightPosition(obj);
	}

}
