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

public class DomoticzUVOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent uvComp;
	DataEncoding uvEncoding;
	DataBlock uvBlock;
	
	public DomoticzUVOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzUVData";
    }


    protected void init() throws IOException
    {
    	System.out.println("Adding UV SWE Template");
    	
    	SWEHelper sweHelp = new SWEHelper();
    	uvComp = sweHelp.newDataRecord(4);
    	uvComp.setName(getName());
    	uvComp.setDefinition("http://sensorml.com/ont/swe/property/Environment");

    	Quantity idx = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/SensorID", 
        		"Sensor ID", 
        		"ID of Sensor", 
        		null, DataType.ASCII_STRING);
		uvComp.addComponent("idx", idx);
    	
		uvComp.addComponent("time", sweHelp.newTimeStampIsoUTC());
		
		Quantity battery = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/BatteryLevel", 
        		"Battery Level", 
        		"Battery Level of Switch", 
        		"%", DataType.INT);
		uvComp.addComponent("batteryLevel", battery);
		
		Quantity uvi = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/UVI", 
        		"UV Index", 
        		"Index of Ultraviolet Radiation", 
        		null, DataType.DOUBLE);
		uvComp.addComponent("uvi", uvi);
    	
    	// also generate encoding definition
    	uvEncoding = sweHelp.newTextEncoding(",", "\n");
    }
    
    
    protected void postUVData(ValidDevice validUV)
    {
    	System.out.println("posting UV data for idx " + validUV.getValidIdx());
    }
    
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return uvComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return uvEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
