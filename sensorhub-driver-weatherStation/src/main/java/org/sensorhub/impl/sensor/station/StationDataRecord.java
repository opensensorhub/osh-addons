package org.sensorhub.impl.sensor.station;

public class StationDataRecord 
{
	private Station station;	
	private long timeUtc;
	private String timeStringUtc;
	private Double lat;
	private Double lon;
	private Double elevation;
	private Double temperature;
	private Double dewPoint;
	private Double relativeHumidity;
	private Double windSpeed;
	private Double windDirection;
	private Double pressure;
	private Double hourlyPrecip; 
	
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
	public String getTimeStringUtc() {
		return timeStringUtc;
	}
	public void setTimeStringUtc(String timeStringUtc) {
		this.timeStringUtc = timeStringUtc;
	}
	public Double getLat() {
		return lat;
	}
	public void setLat(Double lat) {
		this.lat = lat;
	}
	public Double getLon() {
		return lon;
	}
	public void setLon(Double lon) {
		this.lon = lon;
	}
	public Double getElevation() {
		return elevation;
	}
	public void setElevation(Double elevation) {
		this.elevation = elevation;
	}
	public Double getTemperature() {
		return temperature;
	}
	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}
	public Double getDewPoint() {
		return dewPoint;
	}
	public void setDewPoint(Double dewPoint) {
		this.dewPoint = dewPoint;
	}
	public Double getRelativeHumidity() {
		return relativeHumidity;
	}
	public void setRelativeHumidity(Double relativeHumidity) {
		this.relativeHumidity = relativeHumidity;
	}
	public Double getWindSpeed() {
		return windSpeed;
	}
	public void setWindSpeed(Double windSpeed) {
		this.windSpeed = windSpeed;
	}
	public Double getWindDirection() {
		return windDirection;
	}
	public void setWindDirection(Double windDirection) {
		this.windDirection = windDirection;
	}
	public Double getPressure() {
		return pressure;
	}
	public void setPressure(Double pressure) {
		this.pressure = pressure;
	}
	public Double getHourlyPrecip() {
		return hourlyPrecip;
	}
	public void setHourlyPrecip(Double hourlyPrecip) {
		this.hourlyPrecip = hourlyPrecip;
	}
}
