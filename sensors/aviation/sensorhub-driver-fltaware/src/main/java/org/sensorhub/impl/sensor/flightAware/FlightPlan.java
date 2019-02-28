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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.sensorhub.impl.sensor.flightAware.DecodeFlightResult.Result;

/**
 * 
 * <p>
 * 
 * </p>
 *
 * @author tcook
 * @since Sep 27, 2017
 *
 */

public class FlightPlan
{
	Long issueTime;  //  FA does not provide- just use receive time of message 
	String oshFlightId;  // this is flightNumber_destAirport      
	String faFlightId; // flight aware uid- longer string for FA internal and some API calls
	String flightNumber;  // includes airline code prefix

	List<Waypoint> waypoints = new ArrayList<>();
	String originAirport = "";
	String destinationAirport = "";
	public static final String UNKOWN = "UKN";
	double departureTime = Double.NaN;
	double arrivalTime = Double.NaN;

	public FlightPlan() {
	}

	public FlightPlan(DecodeFlightResult result) {
		String prevName = "";
		for(Result.Data d: result.DecodeFlightRouteResult.data) {
			String name = d.name.trim();
			if(prevName.equals(name))
				continue;  // catch duplicate waypts 
			waypoints.add(new Waypoint(name ,  d.type.trim() , (float)d.latitude,(float) d.longitude));
			if (d.type.equalsIgnoreCase("Origin Airport")) {
				originAirport = name;
			} else if (d.type.equalsIgnoreCase("Destination Airport")) {
				destinationAirport = name;
			}  
			prevName = name;
		}
	}

	public void dump() {
		for(Waypoint w: waypoints) {
			System.err.println(w);
		}

	}

	static class Waypoint {
		String code = UNKOWN;
		String type = UNKOWN;
		double time = Double.NaN;
		double lat = Double.NaN;
		double lon = Double.NaN;
//		float alt = 0.0f;
		double alt = Double.NaN;

		public Waypoint(String name, String type, float lat, float lon) {
			this.code = name;
			this.type = type;
			this.lat = lat;
			this.lon = lon;
		}

		public String toString() {
			return code + "," + type + "," + lat + "," + lon;
		}
	}

	public float [] getLats() {
		float[] lats = new float[waypoints.size()];
		int i=0;
		for (Waypoint wp : waypoints) {
			lats[i++] = (float)wp.lat;
		}
		return lats;
	}

	public float[] getLons() {
		float [] lons = new float[waypoints.size()];
		int i=0;
		for (Waypoint wp : waypoints) {
			lons[i++] = (float)wp.lon;
		}
		return lons;
	}

	public String[] getNames() {
		String [] names = new String[waypoints.size()];
		int i=0;
		for (Waypoint wp : waypoints) {
			names[i++] = wp.code;
		}
		return names;
	}

	public String[] getTypes() {
		String [] types = new String[waypoints.size()];
		int i=0;
		for (Waypoint wp : waypoints) {
			types[i++] = wp.type;
		}
		return types;
	}

	public long getTime() {
		return issueTime;
	}

	public void setTime(long time) {
		this.issueTime = time;
	}

	public String getTimeStr() {
		Instant instant = Instant.ofEpochMilli(issueTime * 1000);
		return instant.toString();
	}

	public String getOshFlightId() {
		return oshFlightId;
	}

	public void setOshFlightId(String oshFlightId) {
		this.oshFlightId = oshFlightId;
	}

	public void addWaypoint(String name, float lat, float lon) {
		this.waypoints.add(new Waypoint(name, null, lat, lon));
	}

}
