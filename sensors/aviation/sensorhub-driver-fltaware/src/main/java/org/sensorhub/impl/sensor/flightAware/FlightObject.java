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
import org.sensorhub.impl.sensor.flightAware.DecodeFlightRouteResponse.Waypoint;

/**
 * 
 * <p>
 * 
 * </p>
 *
 * @author tcook
 * @since Sep 27, 2017
 *
 *
 *{
//	   "pitr":"1506541790",
//	   "type":"position",
//	   "ident":"DAL1877",
//	   "aircrafttype":"B739",
//	   "alt":"3550",
//	   "clock":"1506541784",
//	   "facility_hash":"483af88b36b961950eff691dda2824c2cb021a36",
//	   "facility_name":"FlightAware ADS-B",
//	   "id":"DAL1877-1506317145-airline-0309",
//	   "gs":"184",
//	   "heading":"271",
//	   "hexid":"AAE5F7",
//	   "lat":"33.64935",
//	   "lon":"-84.24522",
//	   "reg":"N801DZ",
//	   "updateType":"A",
//	   "altChange":"C",
//	   "air_ground":"A"
//	}
 * 
 * 
 * {
   "pitr":"1506541790",
   "type":"flightplan",
   "ident":"DAL16",
   "aircrafttype":"B739",
   "alt":"190",
   "dest":"KDTW",
   "edt":"1506531480",
   "eta":"1506542880",
   "facility_hash":"80ea8ea96452b0090f49d4d0a93ff51ab8b03a96",
   "facility_name":"Cleveland Center",
   "fdt":"1506530700",
   "hexid":"AB52A2",
   "id":"DAL16-1506317145-airline-0451",
   "orig":"KLAS",
   "reg":"N829DN",
   "route":"KLAS./.HIRED303029..MIZAR.MIZAR4.KDTW/2008",
   "speed":"459",
   "status":"A",
   "waypoints":[
      {
         "lat":36.08000,
         "lon":-115.15000
      },
      {
         "lat":36.09000,
         "lon":-115.10000
      },

 *
 */
public class FlightObject 
{
	transient String json; // original json message as received from firehose
	
    public String type;  // pos or fp
	public String ident;  // tail
	public String status;
	public String air_ground;  // always "A" for us so far
	public String aircrafttype;  // ICAO aircraft type code
	public String alt;
	public String pitr;  //  Timestamp value that should be supplied to the "pitr" connection initiation command when reconnecting and you wish to resume firehose playback at that approximate position
	public String clock;  // posix epoch timestamp
	public String id; // faFlightId
	public String gs;  // ground speed knots
	public String speed;  // fixed cruising speed in knots
	public String heading;
	public String lat;
	public String lon;
	public String orig; // 	ICAO airport code, waypoint, or latitude/longitude pair
	public String dest; // 	ICAO airport code, waypoint, or latitude/longitude pair
	public String reg;
	public String squawk;
	public String updateType;
	public String altChange;  // "C" for climbing, "D" for descending, " " when undetermined
	public String edt;
	public String eat;
	public String ete;
	public String fdt;
	public String route;
	public String facility_name;
	
	List<Waypoint> decodedRoute;
	public double verticalChange;  //feet per minute
    
    
    public long getMessageTime() {
        return Long.parseLong(pitr);
    }  
	
	public Long getDepartureTime() {
		if(edt != null)
			return Long.parseLong(edt);
		if(fdt != null)
			return Long.parseLong(fdt);
		return null;
	}
	
	public long getClock() {
		return Long.parseLong(clock);
	}

	public long getTimeMs() {
		return  Long.parseLong(clock);
	}

	public String getTimeStr() {
		if(clock == null)  return "";
		Instant instant = Instant.ofEpochMilli(getTimeMs() * 1000);
		return instant.toString();
	}
	
	public double getValue (String s) {
		return toDouble(s); 
	}
	
	private static double toDouble(String s) {
		if(s == null)
			return 0.0;  // missing?
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0.0;
		}
		
	}
	
	private static Double getBigDouble(String s) {
		if(s == null)
			return null;  // missing?
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public Double getLatitude() {
		return getBigDouble(lat);
	}
	
	public Double getLongtiude() {
		return getBigDouble(lon);
	}
	
	public Double getAltitude() {
		return getBigDouble(alt);
	}
	
	public Double getGroundSpeed() {
		return getBigDouble(gs);
	}
	
	public Double getHeading() {
		return getBigDouble(heading);
	}
	
	public String toTabbedString() {
		String result;
		//if any field is missing in the received message,
		//for eg if "squawk" is missing then squawk value will be null!
		//format as a table left justified, 10 chars min width
		result = String.format("%-10s %-10s\n %-10s %-10s\n %-10s %-10s\n "
				+ "%-10s %-10s\n %-10s %-10s\n %-10s %-10s\n "
				+ "%-10s %-10s\n %-10s %-10s\n %-10s %-10s\n "
				+ "%-10s %-10s\n %-10s %-10s\n %-10s %-10s\n "
				+ "%-10s %-10s\n",
				"type", type,
				"ident", ident,
				"airground", air_ground,
				"alt", alt,
				"clock", clock,
				"id", id,
				"gs", gs,
				"heading", heading,
				"lat", lat,
				"lon", lon,
				"reg", reg,
				"squawk", squawk,
				"updateType", updateType
				);
		return result;
	}

    public void addWaypoint(String name, String type, double lat, double lon) {
        if (decodedRoute == null)
            decodedRoute = new ArrayList<>(25);
        decodedRoute.add(new Waypoint(name, type, lat, lon));
    }
}