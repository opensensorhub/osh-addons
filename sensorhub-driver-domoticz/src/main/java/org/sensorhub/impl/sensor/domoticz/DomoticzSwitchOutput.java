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

public class DomoticzSwitchOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent switchComp;
	DataEncoding switchEncoding;
	DataBlock switchBlock;
	
	public DomoticzSwitchOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzSwitchData";
    }


    protected void init() throws IOException
    {
    	System.out.println("Adding Switch SWE Template");
    	
    	SWEHelper sweHelp = new SWEHelper();
    	switchComp = sweHelp.newDataRecord(4);
    	switchComp.setName(getName());
    	switchComp.setDefinition("http://sensorml.com/ont/swe/property/Switch");

    	Quantity idx = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/SensorID", 
        		"Sensor ID", 
        		"ID of Sensor", 
        		null, DataType.ASCII_STRING);
    	switchComp.addComponent("idx", idx);
    	
    	switchComp.addComponent("time", sweHelp.newTimeStampIsoUTC());
		
		Quantity battery = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/BatteryLevel", 
        		"Battery Level", 
        		"Battery Level of Switch", 
        		"%", DataType.INT);
		switchComp.addComponent("batteryLevel", battery);
		
		Quantity state = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/SwitchState", 
        		"Switch Status", 
        		"Status of Switch", 
        		null, DataType.ASCII_STRING);
		switchComp.addComponent("switchState", state);

    	// also generate encoding definition
    	switchEncoding = sweHelp.newTextEncoding(",", "\n");
    }
    
    protected void postSwitchData(ValidDevice validSwitch)
    {
    	System.out.println("posting Switch data for idx " + validSwitch.getValidIdx());
    }
    
    protected void start()
    { 
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return switchComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return switchEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
