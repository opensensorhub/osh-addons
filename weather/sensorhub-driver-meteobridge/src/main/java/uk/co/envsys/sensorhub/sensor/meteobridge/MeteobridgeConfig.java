package uk.co.envsys.sensorhub.sensor.meteobridge;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

public class MeteobridgeConfig extends SensorConfig {
	@DisplayInfo(label="Latitude", desc="Latitude of meteobridge-connected station")
	public double centerLatitude = 34.8038; // in deg
	@DisplayInfo(label="Longitude", desc="Longitude of meteobridge-connected station")
	public double centerLongitude = -86.7228; // in deg
	@DisplayInfo(label="Altitude", desc="Altitude of meteobridge-connected station")
	public double centerAltitude = 0.000; // in meters
	@DisplayInfo(label="IP Address", desc="IP Address of the meteobridge device")
	public String address = "127.0.0.1"; // localhost default
	@DisplayInfo(label="Pull Sampling Frequency", desc="How often in seconds to query the meteobridge")
	public double samplingFrequency = 30; // localhost default
	@DisplayInfo(label="Serial Number", desc="Serial number on meteobridge device")
	public String serialNumber = "4950287502834983"; 
	@DisplayInfo(label="Enable th0temp", desc="Enable outdoor temperature sensor")
	public boolean th0tempEnabled = true;
	@DisplayInfo(label="Enable th0hum", desc="Enable outdoor humidity sensor")
	public boolean th0humEnabled = true;
	@DisplayInfo(label="Enable th0dew", desc="Enable outdoor dew point sensor")
	public boolean th0dewEnabled = true;
	@DisplayInfo(label="Enable th0heatindex", desc="Enable outdoor heat index sensor")
	public boolean th0heatindexEnabled = false;
	@DisplayInfo(label="Enable thb0temp", desc="Enable indoor temperature sensor")
	public boolean thb0tempEnabled = true;
	@DisplayInfo(label="Enable thb0hum", desc="Enable indoor humidity sensor")
	public boolean thb0humEnabled = true;
	@DisplayInfo(label="Enable thb0dew", desc="Enable indoor dew point sensor")
	public boolean thb0dewEnabled = true;
	@DisplayInfo(label="Enable thb0press", desc="Enable station pressure sensor")
	public boolean thb0pressEnabled = true;
	@DisplayInfo(label="Enable thb0seapress", desc="Enable normalized pressure sensor")
	public boolean thb0seapressEnabled = true;
	@DisplayInfo(label="Enable wind0wind", desc="Enable wind speed sensor")
	public boolean wind0windEnabled = true;
	@DisplayInfo(label="Enable wind0avgwind", desc="Enable average wind speedsensor")
	public boolean wind0avgwindEnabled = false;
	@DisplayInfo(label="Enable wind0dir", desc="Enable wind direction sensor")
	public boolean wind0dirEnabled = true;
	@DisplayInfo(label="Enable wind0chill", desc="Enable wind chill sensor")
	public boolean wind0chillEnabled = true;
	@DisplayInfo(label="Enable wind0gust", desc="Enable wind gust sensor")
	public boolean wind0gustEnabled = true;
	@DisplayInfo(label="Enable rain0rate", desc="Enable rain rate sensor")
	public boolean rain0rateEnabled = true;
	@DisplayInfo(label="Enable rain0total", desc="Enable rain fall sensor")
	public boolean rain0totalEnabled = true;
	@DisplayInfo(label="Enable uv0index", desc="Enable UV index sensor")
	public boolean uv0indexEnabled = true;
	@DisplayInfo(label="Enable sol0rad", desc="Enable solar radiation sensor")
	public boolean sol0radEnabled = true;
	@DisplayInfo(label="Enable sol0evo", desc="Enable evapotranspiration sensor")
	public boolean sol0evoEnabled = false;
}