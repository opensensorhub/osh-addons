package org.sensorhub.impl.sensor.domoticz;

public class LocationLLA
{
	public double lat = Double.NaN;
	public double lon = Double.NaN;
	public double alt = Double.NaN;

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
	public double getAlt() {
		return alt;
	}
	public void setAlt(double alt) {
		this.alt = alt;
	}
}