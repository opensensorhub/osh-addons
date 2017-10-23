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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessPositionThread implements Runnable
{
	FlightObject obj;
	private FlightAwareApi api = new FlightAwareApi();
	static final Logger log = LoggerFactory.getLogger(ProcessPositionThread.class);
	FlightAwareConverter converter;

	public ProcessPositionThread(FlightAwareConverter converter, FlightObject obj) {
		this.obj = obj;
		this.converter = converter;
	}

	@Override
	public void run() {
		FlightPlan plan = null;
		if(obj.ident == null || obj.dest == null || obj.ident.length() == 0 || obj.dest.length() == 0) {
			// Position message from FltAware did not contain dest airport.  Try to pull it from API
			try {
				plan = api.getFlightPlan(obj.id);
				//getParentModule().
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
			if(plan == null || plan.destinationAirport == null || plan.destinationAirport.length() == 0) {
				log.debug("STILL Cannot construct oshFlightId for Position. Missing dest in FlightPlan");
				return;
			}
			obj.dest = plan.destinationAirport;
		}
//		String oshFlightId = obj.getOshFlightId();
		converter.newFlightPosition(obj);
	}

}
