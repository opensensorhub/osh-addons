package org.sensorhub.impl.sensor.fltaware;

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
	List<Waypoint> waypoints = new ArrayList<>();

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
		b.append("  heading: " + heading + "\n");
		b.append("  groundspeed: " + gs + "\n");
		b.append("  speed: " + speed + "\n");
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

	public String getInternalId() {
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
	
	public static FlightObject getSampleFlightPlan() {
		FlightObject obj = new FlightObject() ;
		obj.clock = "" + System.currentTimeMillis()/1000;
		obj.ident = "DAL2224";
		obj.dest = "KSAT";
		obj.addWaypoints( 29.53,-98.47);
		obj.addWaypoints( 29.56,-98.37);
		obj.addWaypoints( 29.57,-98.35);
		obj.addWaypoints( 29.58,-98.31);
		obj.addWaypoints( 29.59,-98.31);
		obj.addWaypoints( 29.62,-98.21);
		obj.addWaypoints( 29.67,-98.04);
		obj.addWaypoints( 29.68,-98.01);
		obj.addWaypoints( 29.71,-97.86);
		obj.addWaypoints( 29.71,-97.83);
		obj.addWaypoints( 29.71,-97.81);
		obj.addWaypoints( 29.71,-97.80);
		obj.addWaypoints( 29.71,-97.76);
		obj.addWaypoints( 29.70,-97.50);
		obj.addWaypoints( 29.69,-97.40);
		obj.addWaypoints( 29.69,-97.28);
		obj.addWaypoints( 29.69,-97.27);
		obj.addWaypoints( 29.68,-97.09);
		obj.addWaypoints( 29.67,-96.99);
		obj.addWaypoints( 29.67,-96.88);
		obj.addWaypoints( 29.66,-96.72);
		obj.addWaypoints( 29.66,-96.62);
		obj.addWaypoints( 29.64,-96.41);
		obj.addWaypoints( 29.62,-96.01);
		obj.addWaypoints( 29.64,-95.73);
		obj.addWaypoints( 29.64,-95.67);
		obj.addWaypoints( 29.66,-95.28);
		obj.addWaypoints( 29.70,-95.08);
		obj.addWaypoints( 29.84,-94.47);
		obj.addWaypoints( 30.14,-93.11);
		obj.addWaypoints( 30.27,-92.47);
		obj.addWaypoints( 30.42,-91.65);
		obj.addWaypoints( 30.49,-91.29);
		obj.addWaypoints( 30.61,-90.87);
		obj.addWaypoints( 30.77,-90.33);
		obj.addWaypoints( 31.07,-89.32);
		obj.addWaypoints( 31.47,-87.89);
		obj.addWaypoints( 31.52,-87.81);
		obj.addWaypoints( 31.53,-87.79);
		obj.addWaypoints( 32.33,-86.45);
		obj.addWaypoints( 32.45,-86.24);
		obj.addWaypoints( 32.57,-86.03);
		obj.addWaypoints( 32.64,-85.92);
		obj.addWaypoints( 32.66,-85.89);
		obj.addWaypoints( 32.87,-85.52);
		obj.addWaypoints( 32.92,-85.43);
		obj.addWaypoints( 32.96,-85.36);
		obj.addWaypoints( 33.17,-85.01);
		obj.addWaypoints( 33.34,-84.82);
		obj.addWaypoints( 33.35,-84.81);
		obj.addWaypoints( 33.41,-84.77);
		obj.addWaypoints( 33.73,-84.58);
		obj.addWaypoints( 33.73,-84.43);
		obj.addWaypoints( 33.73,-84.31);
		obj.addWaypoints( 33.73,-84.14);
		obj.addWaypoints( 33.73,-84.04);
		obj.addWaypoints( 33.73,-84.06);
		obj.addWaypoints( 33.68,-84.25);
		obj.addWaypoints( 33.66,-84.35);
		obj.addWaypoints( 33.64,-84.43);
		
		return obj;
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