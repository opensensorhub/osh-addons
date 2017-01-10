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

public class OpenHabAlarmEntryOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent entComp;
	DataEncoding entEncoding;
	DataBlock entBlock;
	
	public OpenHabAlarmEntryOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABAlarmEntryData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Entry Alarm SWE Template");
    	
    	SWEHelper sweHelpEnt = new SWEHelper();
    	OpenHabSWEHelper sweHabEnt = new OpenHabSWEHelper();
    	
    	entComp = sweHelpEnt.newDataRecord(7);
    	entComp.setName(getName());
    	entComp.setDefinition("http://sensorml.com/ont/swe/property/EntryAlarm");
    	
    	entComp.addComponent("name", sweHabEnt.getNameSWE()); // dataRecord(0)
    	entComp.addComponent("time", sweHelpEnt.newTimeStampIsoUTC()); // dataRecord(1)
    	entComp.addComponent("alarmStatus", sweHabEnt.getAlarmEntrySWE()); // dataRecord(2)
    	entComp.addComponent("locationLLA", sweHabEnt.getLocVecSWE()); // dataRecord(3, 4, 5)
    	entComp.addComponent("locationDesc", sweHabEnt.getLocDescSWE()); // dataRecord(6)
		
    	// also generate encoding definition
    	entEncoding = sweHelpEnt.newTextEncoding(",", "\n");
    }
    
    protected void postEntryData(OpenHabThings entThing, OpenHabItems entItem)
    {
//    	System.out.println("posting Entry Alarm data for " + entItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (entThing.getLocation().isEmpty()) ? "undeclared" : entThing.getLocation();

    	// build and publish databook
    	DataBlock dataBlock = entComp.createDataBlock();
    	dataBlock.setStringValue(0, entItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setStringValue(2, entItem.getState());
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabAlarmEntryOutput.this, dataBlock));
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return entComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return entEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
