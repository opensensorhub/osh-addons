package org.sensorhub.impl.sensor.zwavedom;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.zwavedom.ZWaveDomHandler.TempInfoArray;
import org.sensorhub.impl.sensor.zwavedom.ZWaveDomHandler.UtilityInfoArray;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;

public class ZWaveDomUtilityOutput extends AbstractSensorOutput<ZWaveDomDriver>
{
	DataComponent utilComp;
	DataEncoding utilEncoding;
	DataBlock utilBlock;
	ZWaveDomHandler handler;
	UtilityInfoArray utilData;
	Timer timer;
	
	public ZWaveDomUtilityOutput(ZWaveDomDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "ZWaveUtilityData";
    }


    protected void init()
    {
    	handler = new ZWaveDomHandler();
    	
    	SWEHelper fac = new SWEHelper();
    	utilComp = fac.newDataRecord(8);
    	utilComp.setName(getName());
    	//NOTE: SwitchStatus needs to be defined by sensorml
    	utilComp.setDefinition("http://sensorml.com/ont/swe/property/utility");

    	Quantity idx = fac.newQuantity("http://sensorml.com/ont/swe/property/sensorID", 
        		"Sensor ID", 
        		"ID of Switch", 
        		null, DataType.ASCII_STRING);
		utilComp.addComponent("idx", idx);
    	
		utilComp.addComponent("time", fac.newTimeStampIsoUTC());
		
		Quantity battery = fac.newQuantity("http://sensorml.com/ont/swe/property/batteryLevel", 
        		"Battery Level", 
        		"Battery Level of Switch", 
        		"%", DataType.DOUBLE);
		utilComp.addComponent("batteryLevel", battery);
		
		Quantity utilData = fac.newQuantity("http://sensorml.com/ont/swe/property/utilityData", 
        		"Value", 
        		"Utility Percentage Data", 
        		"%", DataType.DOUBLE);
		utilComp.addComponent("utilityData", utilData);
    	
    	utilBlock = utilComp.createDataBlock();
    	
    	// also generate encoding definition
    	utilEncoding = fac.newTextEncoding(",", "\n");
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
	            		UtilityInfoArray utilData = handler.getUtilityFromJSON(parentSensor.getHostURL() + "type=devices&filter=utility");
	            		
	            		for (int i = 0; i < utilData.result.length; i++)
	            		{
	            			DataBlock data = utilBlock.renew();
	            			
	            			String update = utilData.result[i].LastUpdate;
	            			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	            			Date updateUTC = sdf.parse(update);
	            			
	            			Double batt =  Double.parseDouble(utilData.result[i].BatteryLevel);

	            			data.setStringValue(0, utilData.result[i].Name + " : idx " + utilData.result[i].idx);
	            			data.setDoubleValue(1, updateUTC.getTime()/1000);
	            			data.setDoubleValue(2, (batt/255.0)*100.0);
	            			
	            			if (utilData.result[i].Data == null)
	            			{
	            				data.setDoubleValue(3, Double.NaN);
	            			}
	            			else
	            			{
	            				data.setDoubleValue(3, Double.parseDouble(utilData.result[i].Data.replaceAll("%", "")));
	            			}
	            			
	            			latestRecord = data;
		            		latestRecordTime = System.currentTimeMillis();
		            		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, ZWaveDomUtilityOutput.this, latestRecord));
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
		return utilComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return utilEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
