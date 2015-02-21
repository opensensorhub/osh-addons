package org.sensorhub.impl.sensor.station.metar;

import org.sensorhub.impl.sensor.station.StationDataRecord;

public class MetarDataRecord extends StationDataRecord {
	private Double windGust;
	private Double minDailyTemperature;
	private Double maxDailyTemperature;
	private Integer visibility;
	private Integer cloudCeiling;
	public Double getWindGust() {
		return windGust;
	}
	public void setWindGust(Double windGust) {
		this.windGust = windGust;
	}
	public Double getMinDailyTemperature() {
		return minDailyTemperature;
	}
	public void setMinDailyTemperature(Double minDailyTemp) {
		this.minDailyTemperature = minDailyTemp;
	}
	public Double getMaxDailyTemperature() {
		return maxDailyTemperature;
	}
	public void setMaxDailyTemperature(Double maxDailyTemp) {
		this.maxDailyTemperature = maxDailyTemp;
	}
	public Integer getVisibility() {
		return visibility;
	}
	public void setVisibility(Integer visibility) {
		this.visibility = visibility;
	}
	public Integer getCloudCeiling() {
		return cloudCeiling;
	}
	public void setCloudCeiling(Integer cloudCeiling) {
		this.cloudCeiling = cloudCeiling;
	}
}
