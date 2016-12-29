package org.sensorhub.impl.sensor.domoticz;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.domoticz.DomoticzDriver.ValidDevice;
import org.sensorhub.impl.sensor.domoticz.DomoticzHandler.DomoticzResponse;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;

public class DomoticzTempHumOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent tempHumComp;
	DataEncoding tempHumEncoding;
	DataBlock tempHumBlock;

	public DomoticzTempHumOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzTempHumData";
    }


    protected void init() throws IOException
    {
    	System.out.println("Adding Temp/Hum SWE Template");
    	
    	SWEHelper sweHelp = new SWEHelper();
    	tempHumComp = sweHelp.newDataRecord(5);
    	tempHumComp.setName(getName());
    	tempHumComp.setDefinition("http://sensorml.com/ont/swe/property/Environment");

    	Quantity idx = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/SensorID", 
        		"Sensor ID", 
        		"ID of Sensor", 
        		null, DataType.ASCII_STRING);
    	tempHumComp.addComponent("idx", idx);
    	
    	tempHumComp.addComponent("time", sweHelp.newTimeStampIsoUTC());
		
		Quantity battery = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/BatteryLevel", 
        		"Battery Level", 
        		"Battery Level of Switch", 
        		"%", DataType.INT);
		tempHumComp.addComponent("batteryLevel", battery);
		
		Quantity temp = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/Temperature", 
        		"Air Temperature", 
        		"Temperature of Air", 
        		null, DataType.DOUBLE);
		tempHumComp.addComponent("temperature", temp);
		
		Quantity relhum = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/RelativeHumidity", 
        		"Relative Humidity", 
        		"Relative Humidity", 
        		null, DataType.INT);
		tempHumComp.addComponent("relhumidity", relhum);

    	// also generate encoding definition
    	tempHumEncoding = sweHelp.newTextEncoding(",", "\n");
    }
    
    protected void postTempHumData(ValidDevice validTempHum)
    {
    	System.out.println("posting Temp/Hum data for idx " + validTempHum.getValidIdx());
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return tempHumComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return tempHumEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
