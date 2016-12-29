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

public class DomoticzLumOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent lumComp;
	DataEncoding lumEncoding;
	DataBlock lumBlock;
	
	public DomoticzLumOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzLumData";
    }


    protected void init() throws IOException
    {
    	System.out.println("Adding Lum SWE Template");
    	
    	SWEHelper sweHelp = new SWEHelper();
    	lumComp = sweHelp.newDataRecord(4);
    	lumComp.setName(getName());
    	lumComp.setDefinition("http://sensorml.com/ont/swe/property/Environment");

    	Quantity idx = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/SensorID", 
        		"Sensor ID", 
        		"ID of Sensor", 
        		null, DataType.ASCII_STRING);
		lumComp.addComponent("idx", idx);
    	
		lumComp.addComponent("time", sweHelp.newTimeStampIsoUTC());
		
		Quantity battery = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/BatteryLevel", 
        		"Battery Level", 
        		"Battery Level of Switch", 
        		"%", DataType.INT);
		lumComp.addComponent("batteryLevel", battery);
		
		Quantity lux = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/Illuminance", 
        		"Illuminance", 
        		"Luminance Flux per Area", 
        		null, DataType.INT);
		lumComp.addComponent("lux", lux);
    	
    	// also generate encoding definition
    	lumEncoding = sweHelp.newTextEncoding(",", "\n");
    }
    
    protected void postLumData(ValidDevice validLum)
    {
    	System.out.println("posting Luminance data for idx " + validLum.getValidIdx());
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return lumComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return lumEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
