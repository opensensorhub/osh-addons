package uk.co.envsys.sensorhub.sensor.meteobridge;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.ParserConfigurationException;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;
import org.xml.sax.SAXException;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;

public class MeteobridgeOutput extends AbstractSensorOutput<MeteobridgeSensor> {
	private DataComponent weatherData;
    private DataEncoding weatherEncoding;
    private Timer timer;
    
	
	public MeteobridgeOutput(MeteobridgeSensor parentSensor) {
		super(parentSensor);
	}
	
	@Override
	public String getName() {
		return "weather";
	}
	
	/**
	 * Initialise the output, making the dataRecord according to the configuration
	 */
    protected void init() {
        SWEHelper fac = new SWEHelper();
        
        // build SWE Common record structure
    	weatherData = fac.newDataRecord(5);	
        weatherData.setName(getName());
        weatherData.setDefinition("http://sensorml.com/ont/swe/property/WeatherData");
        weatherData.setDescription("Weather measurements from meteobridge");
        
        weatherData.addComponent("time", fac.newTimeStampIsoUTC());
        MeteobridgeConfig config = parentSensor.getConfiguration();
        if(config.th0tempEnabled) {
        	weatherData.addComponent("th0temp", fac.newQuantity(SWEHelper.getPropertyUri("AmbientTemperature"), "Outdoor temperature", null, "Cel"));
        }
        if(config.th0humEnabled) {
        	weatherData.addComponent("th0hum", fac.newQuantity(SWEHelper.getPropertyUri("HumidityValue"), "Outdoor humidity", null, "percent"));
        }
        if(config.th0dewEnabled) {
        	weatherData.addComponent("th0dew", fac.newQuantity(SWEHelper.getPropertyUri("DewPoint"), "Outdoor dew point", null, "Cel"));
        }
        if(config.th0heatindexEnabled) {
        	weatherData.addComponent("th0heatindex", fac.newQuantity(SWEHelper.getPropertyUri("HeatIndex"), "Outdoor heat index", null, "Cel"));
        }
        if(config.thb0tempEnabled) {
        	weatherData.addComponent("thb0temp", fac.newQuantity(SWEHelper.getPropertyUri("AmbientTemperature"), "Indoor temperature", null, "Cel"));
        }
        if(config.thb0humEnabled) {
        	weatherData.addComponent("thb0hum", fac.newQuantity(SWEHelper.getPropertyUri("HumidityValue"), "Indoor humidity", null, "percent"));
        }
        if(config.thb0dewEnabled) {
        	weatherData.addComponent("thb0dew", fac.newQuantity(SWEHelper.getPropertyUri("DewPoint"), "Indoor dew point", null, "Cel"));
        }
        if(config.thb0pressEnabled) {
        	weatherData.addComponent("thb0press", fac.newQuantity(SWEHelper.getPropertyUri("AirPressureValue"), "Station Air Pressure", null, "hPa"));
        }
        if(config.thb0seapressEnabled) {
        	weatherData.addComponent("thb0seapress", fac.newQuantity(SWEHelper.getPropertyUri("SeaSurfacePressure"), "Sea Surface Pressure", null, "hPa"));
        }
        if(config.wind0windEnabled) {
        	weatherData.addComponent("wind0wind", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeed"), "Wind Speed", null, "m/s"));
        }
        if(config.wind0avgwindEnabled) {
        	weatherData.addComponent("wind0avgwind", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeed"), "Average Wind Speed", null, "m/s"));
        }
        if(config.wind0dirEnabled) {
        	// for wind direction, we also specify a reference frame
        	Quantity q = fac.newQuantity(SWEHelper.getPropertyUri("WindDirectionAngle"), "Wind Direction", null, "deg");
            q.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
            q.setAxisID("z");
            weatherData.addComponent("wind0dir", q);
        }
        if(config.wind0chillEnabled) {
        	weatherData.addComponent("wind0chill", fac.newQuantity(SWEHelper.getPropertyUri("WindChill"), "Wind Chill", null, "Cel"));
        }
        if(config.wind0gustEnabled) {
        	weatherData.addComponent("wind0gust", fac.newQuantity(SWEHelper.getPropertyUri("WindGust"), "Wind Gust Factor", null, "gf"));
        }
        if(config.rain0rateEnabled) {
        	weatherData.addComponent("rain0rate", fac.newQuantity(SWEHelper.getPropertyUri("PrecipitationRate"), "Rain Rate", null, "mm/h"));
        }
        if(config.rain0totalEnabled) {
        	weatherData.addComponent("rain0total", fac.newQuantity(SWEHelper.getPropertyUri("Precipitation"), "Total Rain", null, "mm"));
        }
        if(config.uv0indexEnabled) {
        	weatherData.addComponent("uv0index", fac.newQuantity(SWEHelper.getPropertyUri("UltraVioletIndex"), "UV Index", null, "uvi"));
        }
        if(config.sol0radEnabled) {
        	weatherData.addComponent("sol0rad", fac.newQuantity(SWEHelper.getPropertyUri("SolarRadiation"), "Solar Radiation", null, "MJ/m"));
        }
        if(config.sol0evoEnabled) {
        	weatherData.addComponent("sol0evo", fac.newQuantity(SWEHelper.getPropertyUri("SolarEvapotranspiration"), "Solar Evapo-transpiration", null, "MJ/m"));
        }
        
        // also generate encoding definition
        weatherEncoding = fac.newTextEncoding(",", "\n");
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
		return parentSensor.getConfiguration().samplingFrequency;
	}
	
	/**
	 * Method to pull a measurement from the Meteobridge and send to sensor hub
	 * 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	protected void sendMeasurement() throws ParserConfigurationException, SAXException, IOException {
		// Get the address of the device from the configuration
		String host_address = parentSensor.getConfiguration().address;
		MeteobridgeReadings result = null;
		
		// Read and parse the data from the socket
		try(Socket bridgeSocket = new Socket(host_address, 5557);
			InputStream socketStream = bridgeSocket.getInputStream()) {
			String socketData = stringFromInputStream(socketStream);
			result = MeteobridgeReadings.fromXML(socketData);
		} 
		
		// Create the data block from the parsed results
		int componentIndex = 0;
		DataBlock dataBlock = weatherData.createDataBlock();
		dataBlock.setDoubleValue(componentIndex++, result.getDate().getTime()/1000.0);
		
		MeteobridgeConfig config = parentSensor.getConfiguration();
		
        if(config.th0tempEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getTh0Temp());
        }
        if(config.th0humEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getTh0Hum());
        }
        if(config.th0dewEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getTh0Dew());
        }
        if(config.th0heatindexEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getTh0HeatIndex());
        }
        if(config.thb0tempEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getThb0Temp());
        }
        if(config.thb0humEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getThb0Hum());
        }
        if(config.thb0dewEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getThb0Dew());
        }
        if(config.thb0pressEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getThb0Press());
        }
        if(config.thb0seapressEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getThb0SeaPress());
        }
        if(config.wind0windEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getWind0Wind());
        }
        if(config.wind0avgwindEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getWind0AvgWind());
        }
        if(config.wind0dirEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getWind0Dir());
        }
        if(config.wind0chillEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getWind0Chill());
        }
        if(config.wind0gustEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getWind0Gust());
        }
        if(config.rain0rateEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getRain0Rate());
        }
        if(config.rain0totalEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getRain0Total());
        }
        if(config.uv0indexEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getUv0Index());
        }
        if(config.sol0radEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getSol0Rad());
        }
        if(config.sol0evoEnabled) {
        	dataBlock.setDoubleValue(componentIndex++, result.getSol0Evo());
        }
	
		// Set as the latest reading
		latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        // Publish
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, MeteobridgeOutput.this, dataBlock));
	}
	
	protected void start() {
		if (timer != null)
            return;
        timer = new Timer();
        
        // start main measurement generation thread
        TimerTask task = new TimerTask() {
            public void run() {
                try {
					sendMeasurement();
				} catch (ParserConfigurationException | SAXException | IOException e) {
					// Log to sensorhub logging?
					e.printStackTrace();
				}
            }            
        };
        
        timer.scheduleAtFixedRate(task, 0, (long)(getAverageSamplingPeriod()*1000));        
	}
	
	protected void stop() {
		if (timer != null) {
            timer.cancel();
            timer = null;
        }
	}
	
	public static String stringFromInputStream(InputStream is) throws IOException {
		java.util.Scanner scanner = new java.util.Scanner(is);
		scanner.useDelimiter("\\A");
		String fromInputStream = scanner.hasNext() ? scanner.next() : "";
		scanner.close();
		is.close();
		return fromInputStream;
	}
	
	public void updateConfig(MeteobridgeConfig config) {
		init();
	}
	

}
