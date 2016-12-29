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

public class DomoticzSelectorOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent selectorComp;
	DataEncoding selectorEncoding;
	DataBlock selectorBlock;
	
	public DomoticzSelectorOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzSelectorData";
    }


    protected void init() throws IOException
    {
    	System.out.println("Adding Selector SWE Template");
    	
    	SWEHelper sweHelp = new SWEHelper();
    	selectorComp = sweHelp.newDataRecord(4);
    	selectorComp.setName(getName());
    	selectorComp.setDefinition("http://sensorml.com/ont/swe/property/Selector");

    	Quantity idx = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/SensorID", 
        		"Sensor ID", 
        		"ID of Sensor", 
        		null, DataType.ASCII_STRING);
    	selectorComp.addComponent("idx", idx);
    	
    	selectorComp.addComponent("time", sweHelp.newTimeStampIsoUTC());
		
		Quantity battery = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/BatteryLevel", 
        		"Battery Level", 
        		"Battery Level of Switch", 
        		"%", DataType.INT);
		selectorComp.addComponent("batteryLevel", battery);
		
		Quantity level = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/SetLevel", 
        		"Set Level", 
        		"Level of Selector Switch", 
        		"%", DataType.INT);
		selectorComp.addComponent("setLevel", level);

    	// also generate encoding definition
    	selectorEncoding = sweHelp.newTextEncoding(",", "\n");
    }
    
    protected void postSelectorData(ValidDevice validSelector)
    {
    	System.out.println("posting Selector data for idx " + validSelector.getValidIdx());
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return selectorComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return selectorEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
