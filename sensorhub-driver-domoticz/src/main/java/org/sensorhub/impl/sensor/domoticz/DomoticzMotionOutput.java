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

public class DomoticzMotionOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent motionComp;
	DataEncoding motionEncoding;
	DataBlock motionBlock;
	
	public DomoticzMotionOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzMotionData";
    }


    protected void init() throws IOException
    {
    	System.out.println("Adding Motion SWE Template");
    	
    	SWEHelper sweHelpMotion = new SWEHelper();
    	DomoticzSWEHelper sweDomMotion = new DomoticzSWEHelper();
    	
    	motionComp = sweHelpMotion.newDataRecord(8);
    	motionComp.setName(getName());
    	motionComp.setDefinition("http://sensorml.com/ont/swe/property/Motion");
    	
    	motionComp.addComponent("idx", sweDomMotion.getIdxSWE()); // dataRecord(0)
    	motionComp.addComponent("name", sweDomMotion.getNameSWE()); // dataRecord(1)
    	motionComp.addComponent("time", sweHelpMotion.newTimeStampIsoUTC()); // dataRecord(2)
    	motionComp.addComponent("motionStatus", sweDomMotion.getMotionStatusSWE()); // dataRecord(3)
    	motionComp.addComponent("latLonAlt", sweDomMotion.getLocVecSWE()); // dataRecord(4, 5, 6)
    	motionComp.addComponent("locationDesc", sweDomMotion.getLocDescSWE()); // dataRecord(7)

    	// also generate encoding definition
    	motionEncoding = sweHelpMotion.newTextEncoding(",", "\n");
    }
    
    protected void postMotionData(DomoticzResponse domMotionData, ValidDevice validMotion)
    {
    	System.out.println("posting Motion data for idx " + domMotionData.getResult()[0].getIdx());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	
    	String motionStatus;
    	if (domMotionData.getResult()[0].getStatus().equalsIgnoreCase("On"))
    		motionStatus = "Motion!";
    	else if (domMotionData.getResult()[0].getStatus().equalsIgnoreCase("Off"))
    		motionStatus = "No Motion";
    	else
    		motionStatus = "Unknown Status";

    	String locDesc = (validMotion.getValidLocDesc().isEmpty()) ? "undeclared" : validMotion.getValidLocDesc();

    	// build and publish databook
    	DataBlock dataBlock = motionComp.createDataBlock();
    	dataBlock.setStringValue(0, domMotionData.getResult()[0].getIdx());
    	dataBlock.setStringValue(1, domMotionData.getResult()[0].getName());
    	dataBlock.setDoubleValue(2, time);
    	dataBlock.setStringValue(3, motionStatus);
    	dataBlock.setDoubleValue(4, validMotion.getValidLocationLLA().getLat());
    	dataBlock.setDoubleValue(5, validMotion.getValidLocationLLA().getLon());
    	dataBlock.setDoubleValue(6, validMotion.getValidLocationLLA().getAlt());
    	dataBlock.setStringValue(7, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzMotionOutput.this, dataBlock)); 
    }
    
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
