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
	private FlightAwareApi api;
	FlightPlanOutput flightPlanOutput;
	TurbulenceOutput turbulenceOutput;
	String toUid;
	
	public ProcessPlanThread(FlightObject obj, FlightPlanOutput fpo, TurbulenceOutput to, String uid) {
		this.obj = obj;
		this.flightPlanOutput = fpo;
		this.turbulenceOutput = to;
		this.toUid = uid;
		
		api = new FlightAwareApi();
	}
	
	@Override
	public void run() {
		try {
			FlightPlan plan = api.getFlightPlan(obj.id);
			if(plan == null) {
				return;
			}
			//			plan.time = obj.getClock();  // Use pitr?
			plan.time = System.currentTimeMillis() / 1000;
			//			System.err.println(plan);
			if(plan != null) {
				// Deal with FOI at sensor level
				// Check to see if existing FlightPlan entry with this flightId
//				AbstractFeature fpFoi = flightAwareFois.get(FLIGHT_PLAN_UID_PREFIX + plan.oshFlightId);
				// should we replace if it is already there?  Shouldn't matter as long as data is changing
//				if(fpFoi == null) {
//					addFlightPlanFoi(plan.oshFlightId, plan.time);
//				}
				flightPlanOutput.sendFlightPlan(plan);

				//  And Turbulence- only adding FOI
//				AbstractFeature turbFoi = flightAwareFois.get(TURBULENCE_UID_PREFIX + plan.oshFlightId);
//				if(turbFoi == null)
//					addTurbulenceFoi(TURBULENCE_UID_PREFIX + plan.oshFlightId, plan.time);
				turbulenceOutput.addFlightPlan(toUid + plan.oshFlightId, plan);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 	

	}

}
