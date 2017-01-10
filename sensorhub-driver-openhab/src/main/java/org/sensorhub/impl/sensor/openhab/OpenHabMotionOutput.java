package org.sensorhub.impl.sensor.openhab;

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

public class OpenHabMotionOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent motionComp;
	DataEncoding motionEncoding;
	DataBlock motionBlock;
	
	public OpenHabMotionOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABMotionData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Motion SWE Template");
    	
    	SWEHelper sweHelpMotion = new SWEHelper();
    	OpenHabSWEHelper sweHabMotion = new OpenHabSWEHelper();
    	
    	motionComp = sweHelpMotion.newDataRecord(7);
    	motionComp.setName(getName());
    	motionComp.setDefinition("http://sensorml.com/ont/swe/property/Motion");
    	
    	motionComp.addComponent("name", sweHabMotion.getNameSWE()); // dataRecord(0)
    	motionComp.addComponent("time", sweHelpMotion.newTimeStampIsoUTC()); // dataRecord(1)
    	motionComp.addComponent("motionStatus", sweHabMotion.getMotionStatusSWE()); // dataRecord(2)
    	motionComp.addComponent("locationLLA", sweHabMotion.getLocVecSWE()); // dataRecord(3, 4, 5)
    	motionComp.addComponent("locationDesc", sweHabMotion.getLocDescSWE()); // dataRecord(6)

    	// also generate encoding definition
    	motionEncoding = sweHelpMotion.newTextEncoding(",", "\n");
    }
    
//    protected void postMotionData(DomoticzResponse domMotionData, ValidDevice validMotion)
//    {
//    	System.out.println("posting Motion data for idx " + domMotionData.getResult()[0].getIdx());
//    	
//    	double time = System.currentTimeMillis() / 1000.;
//    	
//    	String motionStatus;
//    	if (domMotionData.getResult()[0].getStatus().equalsIgnoreCase("On"))
//    		motionStatus = "Motion!";
//    	else if (domMotionData.getResult()[0].getStatus().equalsIgnoreCase("Off"))
//    		motionStatus = "No Motion";
//    	else
//    		motionStatus = "Unknown Status";
//    	
//    	double lat = Double.NaN;
//    	double lon = Double.NaN;
//    	double alt = Double.NaN;
//    	String locDesc = (validMotion.getValidLocDesc().isEmpty()) ? "undeclared" : validMotion.getValidLocDesc();
//
//    	if (validMotion.getValidLatLonAlt().length == 3)
//    	{
//    		lat = validMotion.getValidLatLonAlt()[0];
//    		lon = validMotion.getValidLatLonAlt()[1];
//    		alt = validMotion.getValidLatLonAlt()[2];
//    	}
//    	else if (validMotion.getValidLatLonAlt().length == 2)
//    	{
//    		lat = validMotion.getValidLatLonAlt()[0];
//    		lon = validMotion.getValidLatLonAlt()[1];
//    	}
//    	else
//    		System.out.println("Lat Lon Alt input for motion device idx " + validMotion.getValidIdx() + " is invalid");
//    	
//    	// build and publish databook
//    	DataBlock dataBlock = motionComp.createDataBlock();
//    	dataBlock.setStringValue(0, domMotionData.getResult()[0].getIdx());
//    	dataBlock.setStringValue(1, domMotionData.getResult()[0].getName());
//    	dataBlock.setDoubleValue(2, time);
//    	dataBlock.setStringValue(3, motionStatus);
//    	dataBlock.setDoubleValue(4, lat);
//    	dataBlock.setDoubleValue(5, lon);
//    	dataBlock.setDoubleValue(6, alt);
//    	dataBlock.setStringValue(7, locDesc);
//    	
//        // update latest record and send event
//        latestRecord = dataBlock;
//        latestRecordTime = System.currentTimeMillis();
//        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabMotionOutput.this, dataBlock)); 
//    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return motionComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return motionEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
