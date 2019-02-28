package org.sensorhub.impl.sensor.openhab;

import java.io.IOException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.openhab.OpenHabHandler.OpenHabItems;
import org.sensorhub.impl.sensor.openhab.OpenHabHandler.OpenHabThings;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

public class OpenHabAlertOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent alertComp;
	DataEncoding alertEncoding;
	DataBlock alertBlock;
	
	public OpenHabAlertOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABAlertData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Alert SWE Template");
    	
    	SWEHelper sweHelpAlert = new SWEHelper();
    	OpenHabSWEHelper sweHabAlert = new OpenHabSWEHelper();
    	
    	alertComp = sweHelpAlert.newDataRecord(8);
    	alertComp.setName(getName());
    	alertComp.setDefinition("http://sensorml.com/ont/swe/property/Alerts");
    	
    	alertComp.addComponent("name", sweHabAlert.getNameSWE()); // dataRecord(0)
    	alertComp.addComponent("time", sweHelpAlert.newTimeStampIsoUTC()); // dataRecord(1)
    	alertComp.addComponent("owningThing", sweHabAlert.getOwningThingSWE()); // dataRecord(2)
    	alertComp.addComponent("alertMsg", sweHabAlert.getAlertMsgSWE()); // dataRecord(3)
    	alertComp.addComponent("locationLLA", sweHabAlert.getLocVecSWE()); // dataRecord(4, 5, 6)
    	alertComp.addComponent("locationDesc", sweHabAlert.getLocDescSWE()); // dataRecord(7)

    	// also generate encoding definition
    	alertEncoding = sweHelpAlert.newTextEncoding(",", "\n");
    }
    
    protected void postAlertData(OpenHabThings alertThing, OpenHabItems alertItem, String alertMsg)
    {
//    	System.out.println("posting Alert data for " + alertItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (alertThing.getLocation().isEmpty()) ? "undeclared" : alertThing.getLocation();

    	// build and publish databook
    	DataBlock dataBlock = alertComp.createDataBlock();
    	dataBlock.setStringValue(0, alertItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setStringValue(2, alertThing.getLabel());
    	dataBlock.setStringValue(3, alertMsg);
    	dataBlock.setDoubleValue(4, lat);
    	dataBlock.setDoubleValue(5, lon);
    	dataBlock.setDoubleValue(6, alt);
    	dataBlock.setStringValue(7, locDesc);
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabAlertOutput.this, dataBlock)); 
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
