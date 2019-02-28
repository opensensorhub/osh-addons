package org.sensorhub.impl.sensor.domoticz;

import java.io.IOException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.domoticz.DomoticzDriver.ValidDevice;
import org.sensorhub.impl.sensor.domoticz.DomoticzHandler.DomoticzResponse;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

public class DomoticzAlertOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent alertComp;
	DataEncoding alertEncoding;
	DataBlock alertBlock;
	
	public DomoticzAlertOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzAlertData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Alert SWE Template");
    	
    	SWEHelper sweHelpAlert = new SWEHelper();
    	DomoticzSWEHelper sweDomAlert = new DomoticzSWEHelper();
    	
    	alertComp = sweHelpAlert.newDataRecord(9);
    	alertComp.setName(getName());
    	alertComp.setDefinition("http://sensorml.com/ont/swe/property/Alerts");
    	
    	alertComp.addComponent("idx", sweDomAlert.getIdxSWE()); // dataRecord(0)
    	alertComp.addComponent("name", sweDomAlert.getNameSWE()); // dataRecord(1)
    	alertComp.addComponent("time", sweHelpAlert.newTimeStampIsoUTC()); // dataRecord(2)
    	alertComp.addComponent("sensorSubtype", sweDomAlert.getSensorSubTypeSWE()); // dataRecord(3)
    	alertComp.addComponent("alertMsg", sweDomAlert.getAlertMsgSWE()); // dataRecord(4)
    	alertComp.addComponent("latLonAlt", sweDomAlert.getLocVecSWE()); // dataRecord(5, 6, 7)
    	alertComp.addComponent("locationDesc", sweDomAlert.getLocDescSWE()); // dataRecord(8)

    	// also generate encoding definition
    	alertEncoding = sweHelpAlert.newTextEncoding(",", "\n");
    }
    
    protected void postAlertData(DomoticzResponse domAlertData, ValidDevice validAlert)
    {
//    	System.out.println("posting Alert data for idx " + domAlertData.getResult()[0].getIdx());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	String locDesc = (validAlert.getValidLocDesc().isEmpty()) ? "undeclared" : validAlert.getValidLocDesc();
 	
    	// build and publish databook
    	DataBlock dataBlock = alertComp.createDataBlock();
    	dataBlock.setStringValue(0, domAlertData.getResult()[0].getIdx());
    	dataBlock.setStringValue(1, domAlertData.getResult()[0].getName());
    	dataBlock.setDoubleValue(2, time);
    	dataBlock.setStringValue(3, validAlert.getValidType().toString());
    	dataBlock.setStringValue(4, validAlert.getValidAlertMsg());
    	dataBlock.setDoubleValue(5, validAlert.getValidLocationLLA().getLat());
    	dataBlock.setDoubleValue(6, validAlert.getValidLocationLLA().getLon());
    	dataBlock.setDoubleValue(7, validAlert.getValidLocationLLA().getAlt());
    	dataBlock.setStringValue(8, locDesc);
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzAlertOutput.this, dataBlock)); 
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return alertComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return alertEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
