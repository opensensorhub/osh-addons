package org.sensorhub.impl.sensor.station;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 		Class representing a physical weather station
 * </p>
 * @author Tony Cook
 *
 */

public class Station {
	private String name;
	private double lat;
	private double lon;
	private double elevation;
	
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
