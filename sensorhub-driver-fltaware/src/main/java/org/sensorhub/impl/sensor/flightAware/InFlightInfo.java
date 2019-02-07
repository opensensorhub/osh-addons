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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

/**
 * 
 * <p>
 * 
 * </p>
 *
 * @author tcook
 * @since Sep 22, 2017
 * 
 * 
 *   InFlightInfoResult;             {  
      faFlightID;             UAL422-1505885180-airline-0222,
      ident;             UAL422,
      prefix;             ,
      type;             A319,
      suffix;             ,
      origin;             KDEN,
      destination;             KAUS,
      timeout;             timed_out,
      timestamp;             1506102725,
      departureTime;             1506096360,
      firstPositionTime;             1506096476,
      arrivalTime;             1506102720,
      longitude;             0,
      latitude;             0,
      lowLongitude;             -104.89111,
      lowLatitude;             30.21389,
      highLongitude;             -97.08305,
      highLatitude;             39.83333,
      groundspeed;             0,
      altitude;             0,
      heading;             0,
      altitudeStatus;             ,
      updateType;             ,
      altitudeChange;             ,
      waypoints;             39.86 -104.67 39
 *
 */


public class InFlightInfo extends FlightAwareResult
{
	Result InFlightInfoResult = new Result();  // Gson class name doesn't matter, just the variable name.  And Case matters!!!

	List<Waypoint> waypoints;// = new ArrayList<>();

	class Result {
		String faFlightID;
		String ident;
		String prefix;
		String type;
		String suffix;
		String origin;
		public String destination;
		String timeout;
		long timestamp;
		long departureTime;
		long firstPositionTime;
		long arrivalTime;
		double longitude;
		double latitude;
		double lowLongitude;
		double lowLatitude;
		double highLongitude;
		double highLatitude;
		int groundspeed;
		int altitude;
		int heading;
		String altitudeStatus;
		String updateType;
		String altitudeChange;
		String waypoints;
	}
	
	class Waypoint {
		public double lat;
		public double lon;
		
		@Override
		public String  toString() {
			return lat + "," + lon;
		}
	}
	
	public List<Waypoint> createWaypoints() {
		if(waypoints != null)
			return waypoints;
		List<Waypoint> pts = new ArrayList<>();
		if(InFlightInfoResult.waypoints == null)
			return pts;
		String [] vals = InFlightInfoResult.waypoints.split(" ");
		for(int i=0; i<vals.length; i+=2) {
			Waypoint wp = new Waypoint();
			wp.lat = Double.parseDouble(vals[i]);
			if(i < vals.length)  // just in case they feed us a missing end point
				wp.lon = Double.parseDouble(vals[i+1]);
			pts.add(wp);
		}
		
		return pts;
	}

	public static void main(String[] args) throws Exception {
		Gson gson = new Gson();
		InFlightInfo pojo = new InFlightInfo();
		
		String json = gson.toJson(pojo);
		System.err.println(json);
		json = json.replace("HELL", "DIXIE");
		InFlightInfo  info = gson.fromJson(json, InFlightInfo.class);
		System.err.println(info.InFlightInfoResult.destination + " " + info.InFlightInfoResult.timeout);
	}
	
//	public InFlightInfoResult getInFlightInfoResult() {
//		return inFlightInfoResult;
//	}
//
//	public void setInFlightInfoResult(InFlightInfoResult inFlightInfoResult) {
//		this.inFlightInfoResult = inFlightInfoResult;
//	}
}
