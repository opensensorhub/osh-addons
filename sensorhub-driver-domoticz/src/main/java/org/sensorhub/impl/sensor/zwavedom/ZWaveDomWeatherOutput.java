package org.sensorhub.impl.sensor.zwavedom;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.zwavedom.ZWaveDomHandler.LightInfoArray;
import org.sensorhub.impl.sensor.zwavedom.ZWaveDomHandler.WeatherInfoArray;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;

public class ZWaveDomWeatherOutput extends AbstractSensorOutput<ZWaveDomDriver>
{
	DataComponent weatherComp;
	DataEncoding weatherEncoding;
	DataBlock weatherBlock;
	WeatherInfoArray weatherData;
	ZWaveDomHandler handler;
	Timer timer;
	
	public ZWaveDomWeatherOutput(ZWaveDomDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "ZWaveWeatherData";
    }


    protected void init()
    {
    	handler = new ZWaveDomHandler();
    	
    	SWEHelper fac = new SWEHelper();
    	weatherComp = fac.newDataRecord(15);
    	weatherComp.setName(getName());
    	weatherComp.setDefinition("http://sensorml.com/ont/swe/property/Weather");

    	Quantity idx = fac.newQuantity("http://sensorml.com/ont/swe/property/sensorID", 
        		"Sensor ID", 
        		"ID of Switch", 
        		null, DataType.ASCII_STRING);
    	weatherComp.addComponent("idx", idx);
    	
    	weatherComp.addComponent("time", fac.newTimeStampIsoUTC());
    	
		Quantity battery = fac.newQuantity("http://sensorml.com/ont/swe/property/batteryLevel", 
        		"Battery Level", 
        		"Battery Level of Switch", 
        		"%", DataType.DOUBLE);
		weatherComp.addComponent("batteryLevel", battery);
		
		Quantity baro = fac.newQuantity("http://sensorml.com/ont/swe/property/barometricPressure", 
        		"Barometric Pressure", 
        		"Barometric Pressure", 
        		"hPa", DataType.DOUBLE);
		weatherComp.addComponent("barometricPressure", baro);
		
		Quantity chill = fac.newQuantity("http://sensorml.com/ont/swe/property/windChill", 
        		"Wind Chill", 
        		"Wind Chill", 
        		"degC", DataType.DOUBLE);
		weatherComp.addComponent("windChill", chill);
		
		Quantity dew = fac.newQuantity("http://sensorml.com/ont/swe/property/dewPoint", 
        		"Dew Point", 
        		"Dew Point", 
        		"degC", DataType.DOUBLE);
		weatherComp.addComponent("dewPoint", dew);
		
		Quantity speed = fac.newQuantity("http://sensorml.com/ont/swe/property/windSpeed", 
        		"Wind Speed", 
        		"Wind Speed", 
        		"m/s", DataType.DOUBLE);
		weatherComp.addComponent("windSpeed", speed);
		
		Quantity dir = fac.newQuantity("http://sensorml.com/ont/swe/property/windDirection", 
        		"Wind Direction", 
        		"Wind Direction", 
        		null, DataType.ASCII_STRING);
		weatherComp.addComponent("windDir", dir);
		
		Quantity gust = fac.newQuantity("http://sensorml.com/ont/swe/property/windGust", 
        		"Wind Gust", 
        		"Wind Gust", 
        		"m/s", DataType.DOUBLE);
		weatherComp.addComponent("windGust", gust);
		
		Quantity hum = fac.newQuantity("http://sensorml.com/ont/swe/property/relativeHumidity", 
        		"Relative Humidity", 
        		"Relative Humidity", 
        		"%", DataType.DOUBLE);
		weatherComp.addComponent("relativeHumidity", hum);
		
		Quantity rain = fac.newQuantity("http://sensorml.com/ont/swe/property/rainAccumulation", 
        		"Rain Accumulation", 
        		"Rain Accumulation", 
        		"mm", DataType.DOUBLE);
		weatherComp.addComponent("rainAccumulation", rain);
		
		Quantity rainRate = fac.newQuantity("http://sensorml.com/ont/swe/property/rainRate", 
        		"Rain Rate", 
        		"Rain Rate", 
        		"mm/h", DataType.DOUBLE);
		weatherComp.addComponent("rainRate", rainRate);
		
		Quantity temp = fac.newQuantity("http://sensorml.com/ont/swe/property/temperature", 
        		"Temperature", 
        		"Air Temperature", 
        		"degC", DataType.DOUBLE);
		weatherComp.addComponent("temperature", temp);
		
		Quantity uvi = fac.newQuantity("http://sensorml.com/ont/swe/property/uvIndex", 
        		"UVI", 
        		"UV Index", 
        		"UVI", DataType.DOUBLE);
		weatherComp.addComponent("uvIndex", uvi);
		
		Quantity vis = fac.newQuantity("http://sensorml.com/ont/swe/property/visibility", 
        		"Visibility", 
        		"Visibility", 
        		"km", DataType.DOUBLE);
		weatherComp.addComponent("visibility", vis);
		
		weatherBlock = weatherComp.createDataBlock();
    	
    	// also generate encoding definition
    	weatherEncoding = fac.newTextEncoding(",", "\n");
    }
    
    
    protected void start()
    {
    	if (timer != null)
    		return;
    	
    	try
    	{
    		TimerTask timerTask = new TimerTask()
    		{
    			@Override
    			public void run()
    			{
	            	try
	            	{
	            		WeatherInfoArray weatherData = handler.getWeatherFromJSON(parentSensor.getHostURL() + "type=devices&filter=weather");
	            		
	            		for (int i = 0; i < weatherData.result.length; i++)
	            		{
	            			DataBlock data = weatherBlock.renew();
	            			
	            			String update = weatherData.result[i].LastUpdate;
	            			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	            			Date updateUTC = sdf.parse(update);
	            			
	            			Double batt =  Double.parseDouble(weatherData.result[i].BatteryLevel);
	            			
	            			data.setStringValue(0, weatherData.result[i].Name + " : idx " + weatherData.result[i].idx);
	            			data.setDoubleValue(1, updateUTC.getTime()/1000);
	            			data.setDoubleValue(2, (batt/255.0)*100.0);
	            			
	            			if (weatherData.result[i].Barometer == null)
	            			{
	            				data.setDoubleValue(3, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(3, Double.parseDouble(weatherData.result[i].Barometer));
	            			}

	            			if (weatherData.result[i].Chill == null)
	            			{
	            				data.setDoubleValue(4, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(4, Double.parseDouble(weatherData.result[i].Chill));
	            			}
	            			
	            			if (weatherData.result[i].DewPoint == null)
	            			{
	            				data.setDoubleValue(5, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(5, Double.parseDouble(weatherData.result[i].DewPoint));
	            			}
	            			
	            			if (weatherData.result[i].Speed == null)
	            			{
	            				data.setDoubleValue(6, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(6, Double.parseDouble(weatherData.result[i].Speed));
	            			}
	            			
	            			if (weatherData.result[i].DirectionStr == null)
	            			{
	            				data.setStringValue(7, "N/A");
	            			}
	            			else
	            			{
	            				data.setStringValue(7, weatherData.result[i].DirectionStr);
	            			}
	            			
	            			if (weatherData.result[i].Gust == null)
	            			{
	            				data.setDoubleValue(8, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(8, Double.parseDouble(weatherData.result[i].Gust));
	            			}
	            			
	            			if (weatherData.result[i].Humidity == null)
	            			{
	            				data.setDoubleValue(9, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(9, Double.parseDouble(weatherData.result[i].Humidity));
	            			}
	            			
	            			if (weatherData.result[i].Rain == null)
	            			{
	            				data.setDoubleValue(10, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(10, Double.parseDouble(weatherData.result[i].Rain));
	            			}
	            			
	            			if (weatherData.result[i].RainRate == null)
	            			{
	            				data.setDoubleValue(11, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(11, Double.parseDouble(weatherData.result[i].RainRate));
	            			}
	            			
	            			if (weatherData.result[i].Temp == null)
	            			{
	            				data.setDoubleValue(12, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(12, Double.parseDouble(weatherData.result[i].Temp));
	            			}
	            			
	            			if (weatherData.result[i].UVI == null)
	            			{
	            				data.setDoubleValue(13, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(13, Double.parseDouble(weatherData.result[i].UVI));
	            			}
	            			
	            			if (weatherData.result[i].Visibility == null)
	            			{
	            				data.setDoubleValue(14, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(14, Double.parseDouble(weatherData.result[i].Visibility));
	            			}
	            			
	            			latestRecord = data;
		            		latestRecordTime = System.currentTimeMillis();
		            		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, ZWaveDomWeatherOutput.this, latestRecord));
	            		}
	            	}
	            	
	            	catch (Exception e)
	            	{
	            		parentSensor.getLogger().error("Cannot get weather sensor data", e);
	            	}
	            }
	        };
	        timer = new Timer();
	        timer.scheduleAtFixedRate(timerTask, 0, (long)(getAverageSamplingPeriod()*1000));
	        }
    	catch (Exception e)
    	{
	        e.printStackTrace();
	    }  
    }
    
    
    protected void stop()
    {
    	if (timer != null)
    	{
	        timer.cancel();
	        timer = null;
    	}
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return weatherComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return weatherEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
