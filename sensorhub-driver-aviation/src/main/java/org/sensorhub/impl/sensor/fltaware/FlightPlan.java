package org.sensorhub.impl.sensor.fltaware;

import java.util.ArrayList;
import java.util.List;

import org.sensorhub.impl.sensor.fltaware.DecodeFlightResult.Result;

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
 *
 */

public class FlightPlan
{
	long time; // time of what?
	String flightId;  
	String faFlightId; // ??
	List<Waypoint> waypoints = new ArrayList<>();
	String originAirport = "";
	String destinationAirport = "";
	
	public FlightPlan() {
	}
	
	public FlightPlan(DecodeFlightResult result) {
		for(Result.Data d: result.DecodeFlightRouteResult.data) {
			waypoints.add(new Waypoint(d.name.trim() ,  d.type.trim() , (float)d.latitude,(float) d.longitude));
			if (d.type.equalsIgnoreCase("Origin Airport")) {
				originAirport = d.name;
			} else if (d.type.equalsIgnoreCase("Destination Airport")) {
				destinationAirport = d.name;
			}  
		}
	}
	
	public void dump() {
		for(Waypoint w: waypoints) {
			System.err.println(w);
		}
		
	}
	
	class Waypoint {
		String name;
		String type;
		float lat;
		float lon;

		public Waypoint(String name, String type, float lat, float lon) {
			this.name = name;
			this.type = type;
			this.lat = lat;
			this.lon = lon;
		}

		public String toString() {
			return name + "," + type + "," + lat + "," + lon;
		}
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

	public String[] getNames() {
		String [] names = new String[waypoints.size()];
		int i=0;
		for (Waypoint wp : waypoints) {
			names[i++] = wp.name;
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

	public static FlightPlan getSamplePlan() {
		FlightPlan fp =  new FlightPlan();

		fp.flightId = "DAL1323";
		fp.time = System.currentTimeMillis() / 1000;

		fp.waypoints.add(fp.new Waypoint("KLAX","Origin Airport",33.9424944f,-118.4080472f));
//		fp.waypoints.add(fp.new Waypoint("DLREY","Waypoint",33.9436944f,-118.4651389f));
//		fp.waypoints.add(fp.new Waypoint("ENNEY","Waypoint",33.9424722f,-118.5057778f));
//		fp.waypoints.add(fp.new Waypoint("NAANC","Waypoint",33.9316667f,-118.6438889f));
//		fp.waypoints.add(fp.new Waypoint("HAYNK","Waypoint",33.8405833f,-118.6373056f));
//		fp.waypoints.add(fp.new Waypoint("PEVEE","Waypoint",33.6972222f,-118.5208333f));
//		fp.waypoints.add(fp.new Waypoint("HOLTZ","Waypoint",33.6445556f,-118.343f));
//		fp.waypoints.add(fp.new Waypoint("DOTSS","Waypoint",33.6441111f,-117.7952778f));
//		fp.waypoints.add(fp.new Waypoint("EYEDL","Waypoint",33.7513611f,-117.2336389f));
//		fp.waypoints.add(fp.new Waypoint("HOMER","Waypoint",33.7460556f,-116.9738889f));
//		fp.waypoints.add(fp.new Waypoint("CLEEE","Waypoint",33.7233889f,-116.0733056f));

//		fp.waypoints.add(fp.new Waypoint("PKE","VOR-TAC (NAVAID)",34.1019444f,-114.6819444f));
//		fp.waypoints.add(fp.new Waypoint("DRK","VOR-TAC (NAVAID)",34.7025547f,-112.4803456f));
//		fp.waypoints.add(fp.new Waypoint("PYRIT","Reporting Point",34.8695528f,-110.5114333f));
//		fp.waypoints.add(fp.new Waypoint("ZUN","VOR-TAC (NAVAID)",34.9657533f,-109.1545094f));
//		fp.waypoints.add(fp.new Waypoint("ABQ","VOR-TAC (NAVAID)",35.0437956f,-106.8163119f));
//		fp.waypoints.add(fp.new Waypoint("ACH","VOR-TAC (NAVAID)",35.1116667f,-105.04f));
//		fp.waypoints.add(fp.new Waypoint("PNH","VOR-TAC (NAVAID)",35.235f,-101.6991667f));
//		fp.waypoints.add(fp.new Waypoint("IRW","VOR-TAC (NAVAID)",35.3586111f,-97.6091667f));
//		fp.waypoints.add(fp.new Waypoint("MEM","VOR-TAC (NAVAID)",35.015f,-89.9833333f));
//		fp.waypoints.add(fp.new Waypoint("HUTCC","Waypoint (RNAV)",34.6321028f,-87.4287833f));
//		fp.waypoints.add(fp.new Waypoint("KNSAW","Waypoint (RNAV)",34.5713778f,-87.0986722f));

		fp.waypoints.add(fp.new Waypoint("EEZRA","Waypoint",34.5436944f,-86.9376111f));
		fp.waypoints.add(fp.new Waypoint("JKSON","Waypoint",34.3369444f,-86.15f));
		fp.waypoints.add(fp.new Waypoint("GNRLE","Waypoint",34.1399167f,-85.3977222f));
		fp.waypoints.add(fp.new Waypoint("STOWL","Waypoint",34.0498333f,-85.0511111f));
		fp.waypoints.add(fp.new Waypoint("STHRN","Waypoint",33.9222778f,-84.9186944f));
		fp.waypoints.add(fp.new Waypoint("MMOON","Waypoint",33.8823889f,-84.9140278f));
		fp.waypoints.add(fp.new Waypoint("NAVVY","Waypoint",33.7391667f,-84.8973056f));
		fp.waypoints.add(fp.new Waypoint("KATL","Destination Airport",33.6366996f,-84.427864f));

		return fp;
	}

}
