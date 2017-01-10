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

public class OpenHabAlarmOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent alarmComp;
	DataEncoding alarmEncoding;
	DataBlock alarmBlock;
	
	public OpenHabAlarmOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABAlarmData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Alarm SWE Template");
    	
    	SWEHelper sweHelpAlarm = new SWEHelper();
    	OpenHabSWEHelper sweHabAlarm = new OpenHabSWEHelper();
    	
    	alarmComp = sweHelpAlarm.newDataRecord(7);
    	alarmComp.setName(getName());
    	alarmComp.setDefinition("http://sensorml.com/ont/swe/property/AlarmAll");
    	
    	alarmComp.addComponent("name", sweHabAlarm.getNameSWE()); // dataRecord(0)
    	alarmComp.addComponent("time", sweHelpAlarm.newTimeStampIsoUTC()); // dataRecord(1)
    	alarmComp.addComponent("alarmStatus", sweHabAlarm.getAlarmAllSWE()); // dataRecord(2)
    	alarmComp.addComponent("locationLLA", sweHabAlarm.getLocVecSWE()); // dataRecord(3, 4, 5)
    	alarmComp.addComponent("locationDesc", sweHabAlarm.getLocDescSWE()); // dataRecord(6)
		
    	// also generate encoding definition
    	alarmEncoding = sweHelpAlarm.newTextEncoding(",", "\n");
    }
    
    protected void postAlarmData(OpenHabThings alarmThing, OpenHabItems alarmItem)
    {
//    	System.out.println("posting Alarm data for " + alarmItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (alarmThing.getLocation().isEmpty()) ? "undeclared" : alarmThing.getLocation();

    	// build and publish databook
    	DataBlock dataBlock = alarmComp.createDataBlock();
    	dataBlock.setStringValue(0, alarmItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setStringValue(2, alarmItem.getState());
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabAlarmOutput.this, dataBlock));
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return alarmComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return alarmEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
