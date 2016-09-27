package org.sensorhub.impl.sensor.station;


/**
 * <p>
 * 		Class representing a physical weather station
 * </p>
 * @author Tony Cook
 *
 */

public class Station {
	private String id;
	private String name;
	private double lat;
	private double lon;
	private double elevation;
	private String stationType;  // Metar, CWOP, etc. May create an enum for this
	
	public final String getId() {
		return id;
	}
	public final void setId(String id) {
		this.id = id;
	}
	public final String getStationType() {
		return stationType;
	}
	public final void setStationType(String stationType) {
		this.stationType = stationType;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLon() {
		return lon;
	}
	public void setLon(double lon) {
		this.lon = lon;
	}
	public double getElevation() {
		return elevation;
	}
	public void setElevation(double elevation) {
		this.elevation = elevation;
	}

}
