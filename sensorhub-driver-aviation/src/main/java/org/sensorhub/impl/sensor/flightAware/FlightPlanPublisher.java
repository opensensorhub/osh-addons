package org.sensorhub.impl.sensor.flightAware;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;

//  Not used yet. 
public class FlightPlanPublisher implements FlightObjectListener
{
	Map<String, FlightPlan> planMap;
	List<FlightPlanListener> planListeners;
	FlightAwareApi api;
	
	public FlightPlanPublisher() {
		planMap = new LinkedHashMap<>();
		planListeners = new ArrayList<>();
		// api = ...
	}
	
	@Override
	public void processMessage(FlightObject obj) {
		// call api with faFlightId and get flightplan
		if(!obj.type.equals("flightplan")) {
			System.err.println("FlightAwareSensor does not yet support: " + obj.type);
			return;
		}

		try {
			FlightPlan plan = api.getFlightPlan(obj.id);
			if(plan == null) {
				/// warn
				return;
			}
//			plan.time = obj.getClock();  // Use pitr?
			plan.time = System.currentTimeMillis() / 1000;
//			System.err.println(message);
//			System.err.println(plan);
			planMap.put(plan.oshFlightId, plan);
			for(FlightPlanListener l: planListeners)
				l.newFlightPlan(plan);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	
}
