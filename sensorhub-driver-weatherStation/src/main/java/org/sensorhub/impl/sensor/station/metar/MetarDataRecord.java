package org.sensorhub.impl.sensor.station.metar;

import org.sensorhub.impl.sensor.station.StationDataRecord;

/*
 * <METAR_Record recordNumber="1">
<StationName>KHSV</StationName>
<City>Huntsville</City>
<State>Alabama</State>
<ZipCode>35824</ZipCode>
<MeasurementDateUTC>2015-02-28 19:53</MeasurementDateUTC>
<MeasurementDateLocal>2015-02-28 13:53</MeasurementDateLocal>
<Temperature units="degreesF">53.6</Temperature>
<DewPoint units="degreesF">32.0</DewPoint>
<RelativeHumidity units="%">50.0</RelativeHumidity>
<WindSpeed units="mph">7.0</WindSpeed>
<WindDirection units="degrees">0</WindDirection>
<AirPressure units="inches Hg">30.43</AirPressure>
<RainfallLastHour units="inches">0.0</RainfallLastHour>
<HeatIndex units="degreesF">53.6</HeatIndex>
<WindChill units="degreesF">51.53</WindChill>
<WindGust units="mph">0.0</WindGust>
<RainfaillLast_24Hours units="inches">0.0</RainfaillLast_24Hours>
<CloudCeiling units="feet">12000</CloudCeiling>
<Visibility units="feet">52800</Visibility>
<PresentWeather/>
 */

public class MetarDataRecord extends StationDataRecord {
	private Double windGust;
	private Integer visibility;
	private Integer cloudCeiling;
	private Double rainfallLast24Hours;
	private String presentWeather;
	private String skyConditions;
	
	public Double getWindGust() {
		return windGust;
	}
	public void setWindGust(Double windGust) {
		this.windGust = windGust;
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
	public Double getRainfallLast24Hours() {
		return rainfallLast24Hours;
	}
	public void setRainfallLast24Hours(Double rainfallLast24Hours) {
		this.rainfallLast24Hours = rainfallLast24Hours;
	}
	public String getPresentWeather() {
		return presentWeather;
	}
	public void setPresentWeather(String presentWeather) {
		this.presentWeather = presentWeather;
	}
	public String getSkyConditions() {
		return skyConditions;
	}
	public void setSkyConditions(String skyConditions) {
		this.skyConditions = skyConditions;
	}
}
