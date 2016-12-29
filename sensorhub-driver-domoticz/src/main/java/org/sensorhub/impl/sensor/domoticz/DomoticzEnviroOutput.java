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

public class DomoticzEnviroOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent statusComp;
	DataEncoding statusEncoding;
	DataBlock statusBlock;
	
	public DomoticzEnviroOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzStatusData";
    }


    protected void init() throws IOException
    {
    	System.out.println("Adding Status SWE Template");
    	
    	SWEHelper sweHelpStatus = new SWEHelper();
    	DomoticzSWEHelper sweDomStatus = new DomoticzSWEHelper();
    	
    	statusComp = sweHelpStatus.newDataRecord(11);
    	statusComp.setName(getName());
    	statusComp.setDefinition("http://sensorml.com/ont/swe/property/DomoticzStatus");
    	
    	statusComp.addComponent("idx", sweDomStatus.getIdxSWE()); // dataRecord(0)
    	statusComp.addComponent("name", sweDomStatus.getNameSWE()); // dataRecord(1)
    	statusComp.addComponent("time", sweHelpStatus.newTimeStampIsoUTC()); // dataRecord(2)
    	statusComp.addComponent("sensorType", sweDomStatus.getSensorTypeSWE()); // dataRecord(3)
    	statusComp.addComponent("sensorSubtype", sweDomStatus.getSensorSubTypeSWE()); // dataRecord(4)
    	statusComp.addComponent("batteryLevel", sweDomStatus.getBatteryLevelSWE()); // dataRecord(5)
    	statusComp.addComponent("data", sweDomStatus.getDataSWE()); // dataRecord(6)
    	statusComp.addComponent("latLonAlt", sweDomStatus.getLocVecSWE()); // dataRecord(7, 9, 9)
    	statusComp.addComponent("locationDesc", sweDomStatus.getLocDescSWE()); // dataRecord(10)

    	// also generate encoding definition
    	statusEncoding = sweHelpStatus.newTextEncoding(",", "\n");
    }
    
    protected void postStatusData(DomoticzResponse domStatusData, ValidDevice validStatus)
    {
    	System.out.println("posting Status data for idx " + domStatusData.getResult()[0].getIdx());
    	
    	int batt = (domStatusData.getResult()[0].getBatteryLevel() != 255) ? domStatusData.getResult()[0].getBatteryLevel():-1;
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (validStatus.getValidLocDesc().isEmpty()) ? "undeclared" : validStatus.getValidLocDesc();

    	if (validStatus.getValidLatLonAlt().length == 3)
    	{
    		lat = validStatus.getValidLatLonAlt()[0];
    		lon = validStatus.getValidLatLonAlt()[1];
    		alt = validStatus.getValidLatLonAlt()[2];
    	}
    	else if (validStatus.getValidLatLonAlt().length == 2)
    	{
    		lat = validStatus.getValidLatLonAlt()[0];
    		lon = validStatus.getValidLatLonAlt()[1];
    	}
    	else
    		System.out.println("Lat Lon Alt input for temp device idx " + validStatus.getValidIdx() + " is invalid");
    	
    	// build and publish databook
    	DataBlock dataBlock = statusComp.createDataBlock();
    	dataBlock.setStringValue(0, domStatusData.getResult()[0].getIdx());
    	dataBlock.setStringValue(1, domStatusData.getResult()[0].getName());
    	dataBlock.setDoubleValue(2, time);
    	dataBlock.setStringValue(3, domStatusData.getResult()[0].getType()); // Type given by domoticz
    	dataBlock.setIntValue(4, validStatus.getValidType()); // Subtype given by user
    	dataBlock.setIntValue(5, batt);
    	dataBlock.setStringValue(6, domStatusData.getResult()[0].getData());
    	dataBlock.setDoubleValue(7, lat);
    	dataBlock.setDoubleValue(8, lon);
    	dataBlock.setDoubleValue(9, alt);
    	dataBlock.setStringValue(10, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzEnviroOutput.this, dataBlock)); 
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return statusComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return statusEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
