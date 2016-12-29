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
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Vector;

public class DomoticzTempOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent tempComp;
	DataEncoding tempEncoding;
	DataBlock tempBlock;
	
	public DomoticzTempOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzTempData";
    }


    protected void init() throws IOException
    {
    	System.out.println("Adding Temp SWE Template");
    	
    	SWEHelper sweHelp = new SWEHelper();
    	GeoPosHelper posHelp = new GeoPosHelper();
    	
    	tempComp = sweHelp.newDataRecord(8);
    	tempComp.setName(getName());
    	tempComp.setDefinition("http://sensorml.com/ont/swe/property/Environment");

    	Quantity idx = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/SensorID", 
        		"Sensor ID", 
        		"ID of Sensor", 
        		null, DataType.ASCII_STRING);
		tempComp.addComponent("idx", idx);
    	
		tempComp.addComponent("time", sweHelp.newTimeStampIsoUTC());
		
		Quantity battery = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/BatteryLevel", 
        		"Battery Level", 
        		"Battery Level of Switch", 
        		"%", DataType.INT);
		tempComp.addComponent("batteryLevel", battery);
		
		Quantity temp = sweHelp.newQuantity("http://sensorml.com/ont/swe/property/Temperature", 
        		"Air Temperature", 
        		"Temperature of Air", 
        		null, DataType.DOUBLE);
		tempComp.addComponent("temperature", temp);
		
		Vector locVector = posHelp.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
        locVector.setLabel("Location");
        locVector.setDescription("Location LLA input by user");
        tempComp.addComponent("latLonAlt", locVector);
        
        Text locDesc = sweHelp.newText("http://sensorml.com/ont/swe/property/Location",
        		"Sensor Location", "Sensor Location Description");
        tempComp.addComponent("locationDesc", locDesc);

    	// also generate encoding definition
    	tempEncoding = sweHelp.newTextEncoding(",", "\n");
    }
    
    protected void postTempData(DomoticzResponse domTempData, ValidDevice validTemp)
    {
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	int batt = (domTempData.getResult()[0].getBatteryLevel() != 255) ? domTempData.getResult()[0].getBatteryLevel():-1;
    	System.out.println("posting Temp data for idx " + domTempData.getResult()[0].getIdx());
    	if (validTemp.getValidLatLonAlt().length == 3)
    	{
    		lat = validTemp.getValidLatLonAlt()[0];
    		lon = validTemp.getValidLatLonAlt()[1];
    		alt = validTemp.getValidLatLonAlt()[2];
    	}
    	else if (validTemp.getValidLatLonAlt().length == 2)
    	{
    		lat = validTemp.getValidLatLonAlt()[0];
    		lon = validTemp.getValidLatLonAlt()[1];
    	}
    	else
    		System.out.println("Lat Lon Alt input for temp device idx" + validTemp.getValidIdx() + "is invalid");
    	
    	// build and publish databook
    	DataBlock dataBlock = tempComp.createDataBlock();
    	dataBlock.setStringValue(0, domTempData.getResult()[0].getIdx());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setIntValue(2, batt);
    	dataBlock.setDoubleValue(3, domTempData.getResult()[0].getTemp());
    	dataBlock.setDoubleValue(4, lat);
    	dataBlock.setDoubleValue(5, lon);
    	dataBlock.setDoubleValue(6, alt);
    	dataBlock.setStringValue(7, validTemp.getValidLocDesc());
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzTempOutput.this, dataBlock)); 
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
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
