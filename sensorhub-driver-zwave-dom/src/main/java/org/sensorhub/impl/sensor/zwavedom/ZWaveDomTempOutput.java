package org.sensorhub.impl.sensor.zwavedom;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.zwavedom.ZWaveDomHandler.TempInfoArray;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;

public class ZWaveDomTempOutput extends AbstractSensorOutput<ZWaveDomDriver>
{
	DataComponent tempComp;
	DataEncoding tempEncoding;
	DataBlock tempBlock;
	ZWaveDomHandler handler;
	TempInfoArray tempData;
	Timer timer;
	
	public ZWaveDomTempOutput(ZWaveDomDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "ZWaveTempData";
    }


    protected void init()
    {
    	handler = new ZWaveDomHandler();
    	
    	SWEHelper fac = new SWEHelper();
    	tempComp = fac.newDataRecord(8);
    	tempComp.setName(getName());
    	//NOTE: SwitchStatus needs to be defined by sensorml
    	tempComp.setDefinition("http://sensorml.com/ont/swe/property/temp");

    	Quantity idx = fac.newQuantity("http://sensorml.com/ont/swe/property/sensorID", 
        		"Sensor ID", 
        		"ID of Switch", 
        		null, DataType.ASCII_STRING);
		tempComp.addComponent("idx", idx);
    	
		tempComp.addComponent("time", fac.newTimeStampIsoUTC());
		
		Quantity battery = fac.newQuantity("http://sensorml.com/ont/swe/property/batteryLevel", 
        		"Battery Level", 
        		"Battery Level of Switch", 
        		"%", DataType.DOUBLE);
		tempComp.addComponent("batteryLevel", battery);
		
		Quantity baro = fac.newQuantity("http://sensorml.com/ont/swe/property/barometricPressure", 
        		"Barometric Pressure", 
        		"Barometric Pressure", 
        		"hPa", DataType.DOUBLE);
		tempComp.addComponent("barometricPressure", baro);
		
		
		Quantity chill = fac.newQuantity("http://sensorml.com/ont/swe/property/windChill", 
        		"Wind Chill", 
        		"Wind Chill", 
        		"degC", DataType.DOUBLE);
		tempComp.addComponent("windChill", chill);
		
		Quantity dew = fac.newQuantity("http://sensorml.com/ont/swe/property/dewPoint", 
        		"Dew Point", 
        		"Dew Point", 
        		"degC", DataType.DOUBLE);
		tempComp.addComponent("dewPoint", dew);
		
		Quantity hum = fac.newQuantity("http://sensorml.com/ont/swe/property/relativeHumidity", 
        		"Relative Humidity", 
        		"Relative Humidity", 
        		"%", DataType.DOUBLE);
		tempComp.addComponent("relativeHumidity", hum);
		
		Quantity temp = fac.newQuantity("http://sensorml.com/ont/swe/property/temperature", 
        		"Temperature", 
        		"Air Temperature", 
        		"degC", DataType.DOUBLE);
		tempComp.addComponent("temperature", temp);
		
    	
    	tempBlock = tempComp.createDataBlock();
    	
    	// also generate encoding definition
    	tempEncoding = fac.newTextEncoding(",", "\n");
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
	            		TempInfoArray tempData = handler.getTempFromJSON(parentSensor.getHostURL() + "type=devices&filter=temp");
	            		
	            		for (int i = 0; i < tempData.result.length; i++)
	            		{
	            			DataBlock data = tempBlock.renew();
	            			
	            			String update = tempData.result[i].LastUpdate;
	            			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	            			Date updateUTC = sdf.parse(update);
	            			
	            			Double batt =  Double.parseDouble(tempData.result[i].BatteryLevel);

	            			data.setStringValue(0, tempData.result[i].Name + " : idx " + tempData.result[i].idx);
	            			data.setDoubleValue(1, updateUTC.getTime()/1000);
	            			data.setDoubleValue(2, (batt/255.0)*100.0);
	            			
	            			if (tempData.result[i].Barometer == null)
	            			{
	            				data.setDoubleValue(3, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(3, Double.parseDouble(tempData.result[i].Barometer));
	            			}

	            			if (tempData.result[i].Chill == null)
	            			{
	            				data.setDoubleValue(4, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(4, Double.parseDouble(tempData.result[i].Chill));
	            			}
	            			
	            			if (tempData.result[i].DewPoint == null)
	            			{
	            				data.setDoubleValue(5, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(5, Double.parseDouble(tempData.result[i].DewPoint));
	            			}
	            			
	            			if (tempData.result[i].Humidity == null)
	            			{
	            				data.setDoubleValue(6, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(6, Double.parseDouble(tempData.result[i].Humidity));
	            			}
	            			
	            			if (tempData.result[i].Temp == null)
	            			{
	            				data.setDoubleValue(7, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(7, Double.parseDouble(tempData.result[i].Temp));
	            			}
	            			
	            			latestRecord = data;
		            		latestRecordTime = System.currentTimeMillis();
		            		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, ZWaveDomTempOutput.this, latestRecord));
	            		}
	            	}
	            	
	            	catch (Exception e)
	            	{
	            		parentSensor.getLogger().error("Cannot get temp sensor data", e);
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
		return tempComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return tempEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
