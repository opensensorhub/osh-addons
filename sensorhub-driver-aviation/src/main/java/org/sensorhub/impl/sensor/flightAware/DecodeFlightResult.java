package org.sensorhub.impl.sensor.flightAware;

import org.sensorhub.impl.sensor.flightAware.FlightPlan.Waypoint;

public class DecodeFlightResult extends FlightAwareResult
{
	Result DecodeFlightRouteResult = new Result();  // Gson class name doesn't matter, just the variable name.  And Case matters!!!

	class Result {
		int next_offset;
		Data [] data = new Data[] {};
		
		
		class Data {
			String name;
			String type;
			double latitude;
			double longitude;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for(Result.Data d: DecodeFlightRouteResult.data) {
			b.append(d.name + "," + d.type + "," + d.latitude + "," + d.longitude + "\n");
		}
		return b.toString();
	}
}
