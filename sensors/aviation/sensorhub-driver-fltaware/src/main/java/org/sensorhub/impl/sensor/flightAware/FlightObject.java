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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

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
	public String type;  // pos or fp
	public String ident;  // tail
	public String status;
	public String air_ground;  // always "A" for us so far
	public String aircrafttype;  // ICAO aircraft type code
	public String alt;
	public String pitr;  //  Timestamp value that should be supplied to the "pitr" connection initiation command when reconnecting and you wish to resume firehose playback at that approximate position


	public String clock;  // posix epoch timestamp
	public String id = ""; // faFlightId
	public String gs;  // ground speed knots
	public String speed;  // fixed cruising speed in knots
	public String heading;
	public String lat;
	public String lon;
	public String orig = ""; // 	ICAO airport code, waypoint, or latitude/longitude pair
	public String dest = ""; // 	ICAO airport code, waypoint, or latitude/longitude pair
	public String reg;
	public String squawk;
	public String updateType;
	public String altChange;  // "C" for climbing, "D" for descending, " " when undetermined
	public String edt;
	public String eat;
	public String ete;
	public String fdt;
	List<Waypoint> waypoints = new ArrayList<>();
	
	// Adding for LawBox support
	public double verticalChange;  //feet per mminute
	
	public Long getDepartureTime() {
		if(edt != null)
			return Long.parseLong(edt);
		if(fdt != null)
			return Long.parseLong(fdt);
		
		return null;
	}
	
	class Waypoint {
		public Waypoint(float lat, float lon) {
			this.lat = lat;
			this.lon = lon;
		}
		
		float lat;
		float lon;
		float alt;
	}

	public void addWaypoints(double lat, double lon) {
		waypoints.add(new Waypoint((float)lat, (float)lon));
	}

	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("type: " + type + "\n");
		b.append("  id: " + id + "\n");
		b.append("  ident: " + ident + "\n");
		b.append("  status: " + status + "\n");
		b.append("  orig: " + orig + "\n");
		b.append("  dest: " + dest + "\n");
		b.append("  clock: " + clock + "\n");
		b.append("  time: " + getTimeStr() + "\n");
		b.append("  heading: " + heading + "\n");
		b.append("  groundspeed: " + gs + "\n");
		b.append("  speed: " + speed + "\n");
		b.append("  lat: " + lat + "\n");
		b.append("  lon: " + lon + "\n");
		b.append("  alt: " + alt + "\n");
		b.append("  altChange: " + altChange + "\n");
//		b.append("  waypoints: \n");
//		if(waypoints.size() > 0) {
//			int cnt = 0;
//			for (Waypoint wp : waypoints) {
//				b.append("    " + wp.lat + "," + wp.lon );
//				if(wp.alt > 0) {
//					b.append(" " + wp.alt );
//					System.err.println("Alt in waypt!!");
//					System.exit(0);
//				}
//				b.append("\n");
//				
////				if(cnt++>=3)
////					break;
//			}
//		}

		return b.toString();
	}

	public float [] getLats() {
		float[] lats = new float[waypoints.size()];
		int i=0;
		for (Waypoint wp : waypoints) {
			lats[i++] = wp.lat;
		}
		return lats;
	}

	public float[] getLons() {
		float [] lons = new float[waypoints.size()];
		int i=0;
		for (Waypoint wp : waypoints) {
			lons[i++] = wp.lon;
		}
		return lons;
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
	
	// Need to add null checking where this is called
	public String getOshFlightId() {
		if(ident == null || dest == null)
			return null;
		return ident + "_" + dest;
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
}