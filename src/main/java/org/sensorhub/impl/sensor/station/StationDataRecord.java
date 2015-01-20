package org.sensorhub.impl.sensor.station;

public class StationDataRecord 
{
	private Station station;	
	private long timeUtc;
	private String timeStringUtc;
	private double lat;
	private double lon;
	private double elevation;
	private double temperature;
	private double dewPoint;
	private double relativeHumidity;
	private double windSpeed;
	private double windDirection;
	private double windGust;
	private double minDailyTemperature;
	private double maxDailyTemperature;
	private int cloudCeiling;
	private int visibility;
	
	public Station getStation() {
		return station;
	}
	public void setStation(Station station) {
		this.station = station;
	}
	public long getTimeUtc() {
		return timeUtc;
	}
	public void setTimeUtc(long timeUtc) {
		this.timeUtc = timeUtc;
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
	public double getTemperature() {
		return temperature;
	}
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}
	public double getDewPoint() {
		return dewPoint;
	}
	public void setDewPoint(double dewPoint) {
		this.dewPoint = dewPoint;
	}
	public double getRelativeHumidity() {
		return relativeHumidity;
	}
	public void setRelativeHumidity(double relativeHumidity) {
		this.relativeHumidity = relativeHumidity;
	}
	public double getWindSpeed() {
		return windSpeed;
	}
	public void setWindSpeed(double windSpeed) {
		this.windSpeed = windSpeed;
	}
	public double getWindDirection() {
		return windDirection;
	}
	public void setWindDirection(double windDirection) {
		this.windDirection = windDirection;
	}
	public double getWindGust() {
		return windGust;
	}
	public void setWindGust(double windGust) {
		this.windGust = windGust;
	}
	public double getMinDailyTemperature() {
		return minDailyTemperature;
	}
	public void setMinDailyTemperature(double minDailyTemperature) {
		this.minDailyTemperature = minDailyTemperature;
	}
	public double getMaxDailyTemperature() {
		return maxDailyTemperature;
	}
	public void setMaxDailyTemperature(double maxDailyTemperature) {
		this.maxDailyTemperature = maxDailyTemperature;
	}
	public int getCloudCeiling() {
		return cloudCeiling;
	}
	public void setCloudCeiling(int cloudCeiling) {
		this.cloudCeiling = cloudCeiling;
	}
	public int getVisibility() {
		return visibility;
	}
	public void setVisibility(int visibility) {
		this.visibility = visibility;
	}
	public String getTimeStringUtc() {
		return timeStringUtc;
	}
	public void setTimeStringUtc(String timeStringUtc) {
		this.timeStringUtc = timeStringUtc;
	}
}
