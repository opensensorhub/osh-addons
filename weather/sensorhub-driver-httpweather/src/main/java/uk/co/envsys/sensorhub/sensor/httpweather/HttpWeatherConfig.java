package uk.co.envsys.sensorhub.sensor.httpweather;

import java.util.UUID;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;


/**
 * @author Environment Systems - http://www.envsys.co.uk
 *
 */
public class HttpWeatherConfig extends SensorConfig {
	@DisplayInfo(label="Latitude", desc="Latitude of sensor pushing to http")
	public double centerLatitude = 34.8038; // in deg
	@DisplayInfo(label="Longitude", desc="Longitude of sensor pushing to http")
	public double centerLongitude = -86.7228; // in deg
	@DisplayInfo(label="Altitude", desc="Altitude of sensor pushing to http")
	public double centerAltitude = 0.000; // in meters
	@DisplayInfo(label="URL Base", desc="URL for the sensor to push to")
	public String urlBase = UUID.randomUUID().toString();
	@DisplayInfo(label="Expose Internal Temperature", desc="Whether to accept and publish internal temperature readings over http and SOS")
	public boolean exposeInTemp = true;
	@DisplayInfo(label="Expose External Temperature", desc="Whether to accept and publish external temperature readings over http and SOS")
	public boolean exposeOutTemp = true;
	@DisplayInfo(label="Expose External Humidity", desc="Whether to accept and publish external humidity readings over http and SOS")
	public boolean exposeOutHum = false;
	@DisplayInfo(label="Expose Wind Speed", desc="Whether to accept and publish wind speed readings over http and SOS")
	public boolean exposeWindSpeed = false;
	@DisplayInfo(label="Expose Wind Direction", desc="Whether to accept and publish wind direction readings over http and SOS")
	public boolean exposeWindDir = false;
	@DisplayInfo(label="Expose Average Wind Speed", desc="Whether to accept and publish average wind-speed readings over http and SOS")
	public boolean exposeAvgWindSpeed = false;
	@DisplayInfo(label="Expose Rain", desc="Whether to accept and publish rain measurement readings over http and SOS")
	public boolean exposeRain = false;
	@DisplayInfo(label="Expose Sunrise", desc="Whether to accept and publish sunrise information over http and SOS")
	public boolean exposeSunrise= false;
	@DisplayInfo(label="Expose Sunset", desc="Whether to accept and publish sunset information over http and SOS")
	public boolean exposeSunset = false;
	@DisplayInfo(label="Expose Station Pressure", desc="Whether to accept and publish air pressure readings over http and SOS")
	public boolean exposeStationPressure = false;
	@DisplayInfo(label="Expose Sea Pressure", desc="Whether to accept and publish sea-level air pressure readings over http and SOS")
	public boolean exposeSeaPressure = false;
	@DisplayInfo(label="Expose UV Index", desc="Whether to accept and publish UV index readings over http and SOS")
	public boolean exposeUV = false;
	@DisplayInfo(label="Expose Solar Radiation", desc="Whether to accept and publish solar radiation readings over http and SOS")
	public boolean exposeSolar = false;
	@DisplayInfo(label="Expose Heat Index", desc="Whether to accept and publish heat index readings over http and SOS")
	public boolean exposeHeatIndex= false;
	@DisplayInfo(label="Expose Dew Point", desc="Whether to accept and publish dew point readings over http and SOS")
	public boolean exposeDewPoint = false;
	@DisplayInfo(label="Expose Wind Chill", desc="Whether to accept and publish wind chill readings over http and SOS")
	public boolean exposeWindChill = false;
}
