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
		    obj.dest = converter.idToDestinationCache.getIfPresent(obj.id);  
		    if (obj.dest == null)
		    {
		        if (converter.lastMessageTime >= converter.startTime)
		            log.debug("** {}: Unknown destination airport", obj.ident);
		        return;
		    }
		}
		
		log.trace("{}_{}: New position received", obj.ident, obj.dest);
		converter.newFlightPosition(obj);
	}

}
