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

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

public class ProcessPlanThread implements Runnable
{
	FlightObject obj;
	FlightAwareApi api;
	FlightAwareConverter converter;
	
	public ProcessPlanThread(FlightAwareConverter converter, FlightObject obj) {
		this.converter = converter;
		this.obj = obj;
		api = new FlightAwareApi();
	}
	
	@Override
	public void run() {
		try {
			FlightPlan plan = api.getFlightPlan(obj.id);
			if(plan == null) {
				return;
			}
			//	plan.time = obj.getClock();  // Use pitr?
			plan.time = System.currentTimeMillis() / 1000;
			converter.newFlightPlan(plan);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 	
	}

}
