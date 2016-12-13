package org.sensorhub.impl.sensor.zwavedom;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.zwavedom.ZWaveDomHandler.LightInfoArray;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;

public class ZWaveDomLightOutput extends AbstractSensorOutput<ZWaveDomDriver>
{
	DataComponent lightComp;
	DataEncoding lightEncoding;
	DataBlock lightBlock;
	LightInfoArray lightData;
	ZWaveDomHandler handler;
	Timer timer;
	
	public ZWaveDomLightOutput(ZWaveDomDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "ZWaveLightData";
    }


    protected void init()
    {
    	handler = new ZWaveDomHandler();
    	
    	SWEHelper fac = new SWEHelper();
    	lightComp = fac.newDataRecord(4);
    	lightComp.setName(getName());
    	//NOTE: SwitchStatus needs to be defined by sensorml
    	lightComp.setDefinition("http://sensorml.com/ont/swe/property/switch");

    	Quantity idx = fac.newQuantity("http://sensorml.com/ont/swe/property/sensorID", 
        		"Sensor ID", 
        		"ID of Sensor", 
        		null, DataType.ASCII_STRING);
		lightComp.addComponent("idx", idx);
    	
		lightComp.addComponent("time", fac.newTimeStampIsoUTC());
		
		Quantity battery = fac.newQuantity("http://sensorml.com/ont/swe/property/batteryLevel", 
        		"Battery Level", 
        		"Battery Level of Switch", 
        		"%", DataType.DOUBLE);
		lightComp.addComponent("batteryLevel", battery);
		
		Quantity status = fac.newQuantity("http://sensorml.com/ont/swe/property/switchStatus", 
        		"Switch Status", 
        		"Status of Switch", 
        		null, DataType.BOOLEAN);
		lightComp.addComponent("switchStatus", status);
    	
    	lightBlock = lightComp.createDataBlock();
    	
    	// also generate encoding definition
    	lightEncoding = fac.newTextEncoding(",", "\n");
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
	            		LightInfoArray lightData = handler.getLightFromJSON(parentSensor.getHostURL() + "type=devices&filter=light");
	            		
	            		for (int i = 0; i < lightData.result.length; i++)
	            		{
	            			DataBlock data = lightBlock.renew();
	            			
	            			String update = lightData.result[i].LastUpdate;
	            			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	            			Date updateUTC = sdf.parse(update);
	            			
	            			Double batt =  Double.parseDouble(lightData.result[i].BatteryLevel);
	            			
	            			data.setStringValue(0, lightData.result[i].Name + " : idx " + lightData.result[i].idx);
	            			data.setDoubleValue(1, updateUTC.getTime()/1000);
	            			data.setDoubleValue(2, (batt/255.0)*100.0);
	            			data.setBooleanValue(3, (lightData.result[i].Status.equals("On")) ? true:false);
	            			latestRecord = data;
		            		latestRecordTime = System.currentTimeMillis();
		            		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, ZWaveDomLightOutput.this, latestRecord));
	            		}
	            	}
	            	
	            	catch (Exception e)
	            	{
	            		parentSensor.getLogger().error("Cannot get light sensor data", e);
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
		return lightComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return lightEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
