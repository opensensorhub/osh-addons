package uk.co.envsys.sensorhub.sensor.httpweather;

import java.util.Date;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;

public class HttpWeatherOutput extends AbstractSensorOutput<HttpWeatherSensor> {
	private double averageSamplingPeriod; 	
	private DataComponent weatherData;
    private DataEncoding weatherEncoding;
    
    /**
     * Default constructor, passes the sensor responsible for this output
     * @param parentSensor - the parent sensor
     */
	public HttpWeatherOutput(HttpWeatherSensor parentSensor) {
		super(parentSensor);
	}
	
	/**
	 * Initialisation function, called by framework, and when configuration is updated
	 * Sets the format of the weather observations, which in turn affects the sensors SOS/SWE profile
	 */
	protected void init() {
		averageSamplingPeriod = 30;
		SWEHelper fac = new SWEHelper();
        
        // build SWE Common record structure
    	weatherData = fac.newDataRecord(5); 	// Use a more sensible value than 5!	
        weatherData.setName(getName());
        weatherData.setDefinition("http://sensorml.com/ont/swe/property/WeatherData");
        weatherData.setDescription("Weather measurements transmitted to http server");
        
        weatherData.addComponent("time", fac.newTimeStampIsoUTC());
        
        // Initialise data components according to what is enabled in configuration
        HttpWeatherConfig config = parentSensor.getConfiguration();
       
        if(config.exposeInTemp) {
        	weatherData.addComponent("intemp", fac.newQuantity(SWEHelper.getPropertyUri("AmbientTemperature"), "Indoor temperature", null, "Cel"));
        }
        if(config.exposeOutTemp) {
        	weatherData.addComponent("outtemp", fac.newQuantity(SWEHelper.getPropertyUri("AmbientTemperature"), "Outdoor temperature", null, "Cel"));
        }
        if(config.exposeOutHum) {
        	weatherData.addComponent("outhum", fac.newQuantity(SWEHelper.getPropertyUri("HumidityValue"), "Outdoor humidity", null, "percent"));
        }
        if(config.exposeWindSpeed) {
        	weatherData.addComponent("windspeed", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeed"), "Wind Speed", null, "m/s"));
        }
        if(config.exposeAvgWindSpeed) {
        	weatherData.addComponent("avgwindspeed", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeed"), "Average Wind Speed", null, "m/s"));
        }
        if(config.exposeWindDir) {
        	// for wind direction, we also specify a reference frame
        	Quantity q = fac.newQuantity(SWEHelper.getPropertyUri("WindDirectionAngle"), "Wind Direction", null, "deg");
            q.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
            q.setAxisID("z");
            weatherData.addComponent("winddir", q);
        }
        if(config.exposeRain) {
        	weatherData.addComponent("rain", fac.newQuantity(SWEHelper.getPropertyUri("PrecipitationRate"), "Precipitation Rate", null, "Cel"));
        }
        if(config.exposeSunrise) {
        	weatherData.addComponent("sunrise", fac.newText(SWEHelper.getPropertyUri("SunriseTime"), "Sunrise Time", "h:m:s"));
        }
        if(config.exposeSunset) {
        	weatherData.addComponent("sunset", fac.newText(SWEHelper.getPropertyUri("SunsetTime"), "Sunset Time", "h:m:s"));
        }
        if(config.exposeStationPressure) {
        	weatherData.addComponent("pressure", fac.newQuantity(SWEHelper.getPropertyUri("AirPressureValue"), "Station Air Pressure", null, "hPa"));
        }
        if(config.exposeSeaPressure) {
        	weatherData.addComponent("seapressure", fac.newQuantity(SWEHelper.getPropertyUri("SeaSurfacePressure"), "Sea Surface Pressure", null, "hPa"));
        }
        if(config.exposeUV) {
        	weatherData.addComponent("uvindex", fac.newQuantity(SWEHelper.getPropertyUri("UltraVioletIndex"), "UV Index", null, "uvi"));
        }
        if(config.exposeSolar) {
        	weatherData.addComponent("solarradiation", fac.newQuantity(SWEHelper.getPropertyUri("SolarRadiation"), "Solar Radiation", null, "MJ/m"));
        }
        if(config.exposeHeatIndex) {
        	weatherData.addComponent("heatindex", fac.newQuantity(SWEHelper.getPropertyUri("HeatIndex"), "Outdoor heat index", null, "Cel"));
        }
        if(config.exposeDewPoint) {
        	weatherData.addComponent("dewpoint", fac.newQuantity(SWEHelper.getPropertyUri("DewPoint"), "Dew point", null, "Cel"));
        }
        if(config.exposeWindChill) {
        	weatherData.addComponent("windchill", fac.newQuantity(SWEHelper.getPropertyUri("WindChill"), "Wind Chill", null, "Cel"));
        }  
        // also generate encoding definition
        weatherEncoding = fac.newTextEncoding(",", "\n");
	}
	
	
	/**
	 * Called by the sensor when its configuration is updated, re-initialises
	 */
	public void updateConfig() {
		init();
	}
	
	/**
	 * Called by the nanolet to pass an observation back to sensorhub.
	 * Use the configuration to shape the data block accordingly.
	 */
	public void sendMeasurement(double inTemp, double outTemp, double outHum, double windSpeed, double avgWindSpeed, double windDir, double rain, 
			String sunrise, String sunset, double pressure, double seaPressure, double uvIndex, double solarRadiation, double heatIndex, 
			double dewPoint, double windChill) {
		
		int componentIndex = 0;
		DataBlock dataBlock = weatherData.createDataBlock();
		dataBlock.setDoubleValue(componentIndex++, new Date().getTime()/1000.0);
		HttpWeatherConfig config = parentSensor.getConfiguration();
	       
        if(config.exposeInTemp) {
        	dataBlock.setDoubleValue(componentIndex++, inTemp);
        }
        if(config.exposeOutTemp) {
        	dataBlock.setDoubleValue(componentIndex++, outTemp);
        }
        if(config.exposeOutHum) {
        	dataBlock.setDoubleValue(componentIndex++, outHum);
        }
        if(config.exposeWindSpeed) {
        	dataBlock.setDoubleValue(componentIndex++, windSpeed);
        }
        if(config.exposeAvgWindSpeed) {
        	dataBlock.setDoubleValue(componentIndex++, avgWindSpeed);
        }
        if(config.exposeWindDir) {
        	dataBlock.setDoubleValue(componentIndex++, windDir);
        }
        if(config.exposeRain) {
        	dataBlock.setDoubleValue(componentIndex++, rain);
        }
        if(config.exposeSunrise) {
        	dataBlock.setStringValue(componentIndex++, sunrise);
        }
        if(config.exposeSunset) {
        	dataBlock.setStringValue(componentIndex++, sunset);
        }
        if(config.exposeStationPressure) {
        	dataBlock.setDoubleValue(componentIndex++, pressure);
        }
        if(config.exposeSeaPressure) {
        	dataBlock.setDoubleValue(componentIndex++, seaPressure);
        }
        if(config.exposeUV) {
        	dataBlock.setDoubleValue(componentIndex++, uvIndex);
        }
        if(config.exposeSolar) {
        	dataBlock.setDoubleValue(componentIndex++, solarRadiation);
        }
        if(config.exposeHeatIndex) {
        	dataBlock.setDoubleValue(componentIndex++, heatIndex);
        }
        if(config.exposeDewPoint) {
        	dataBlock.setDoubleValue(componentIndex++, dewPoint);
        }
        if(config.exposeWindChill) {
        	dataBlock.setDoubleValue(componentIndex++, windChill);
        }
        
        // Set as the latest reading
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        // Publish
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, HttpWeatherOutput.this, dataBlock));
	}
	
	@Override
	public String getName() {
		return "httpweather";
	}

	@Override
	public DataComponent getRecordDescription() {
		return weatherData;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return weatherEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return averageSamplingPeriod;
	}

}
