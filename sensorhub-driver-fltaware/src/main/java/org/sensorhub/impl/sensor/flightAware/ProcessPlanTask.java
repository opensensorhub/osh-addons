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

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

/**
 *   Process Flight Plan messages from FlightAware firehose feed.
 *   Note that because of the way FlightAware feed and API work, 
 *   we have to pull some info from the Firehose message (airports, departTime, issueTime)
 *   and some from the API (actual waypoints) 
 *
 * @author tcook
 *
 */
public class ProcessPlanTask implements Runnable
{
	FlightObject obj;
	FlightAwareApi api;
	MessageHandler converter;
	
	public ProcessPlanTask(MessageHandler converter, FlightObject obj) {
		this.converter = converter;
		this.obj = obj;
		this.api = new FlightAwareApi(converter.user, converter.passwd);
	}
	
	@Override
	public void run() {
		try {
			FlightPlan plan = api.getFlightPlan(obj.id);
			if(plan == null) {
				return;
			}
			//  By convention, I am using message receive time as issueTime
			//  Flight Aware does not include it in feed
			plan.issueTime = System.currentTimeMillis() / 1000;
			if(obj.orig != null)
				plan.originAirport = obj.orig;
			if(obj.dest != null)
				plan.destinationAirport = obj.dest;
			plan.departureTime = obj.getDepartureTime();
			converter.newFlightPlan(plan);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 	
	}

}
