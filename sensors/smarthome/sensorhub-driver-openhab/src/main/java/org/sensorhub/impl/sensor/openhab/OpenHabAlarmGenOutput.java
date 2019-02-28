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

public class OpenHabAlarmGenOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent genComp;
	DataEncoding genEncoding;
	DataBlock genBlock;
	
	public OpenHabAlarmGenOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABAlarmGenData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding General Alarm SWE Template");
    	
    	SWEHelper sweHelpGen = new SWEHelper();
    	OpenHabSWEHelper sweHabGen = new OpenHabSWEHelper();
    	
    	genComp = sweHelpGen.newDataRecord(7);
    	genComp.setName(getName());
    	genComp.setDefinition("http://sensorml.com/ont/swe/property/GeneralAlarm");
    	
    	genComp.addComponent("name", sweHabGen.getNameSWE()); // dataRecord(0)
    	genComp.addComponent("time", sweHelpGen.newTimeStampIsoUTC()); // dataRecord(1)
    	genComp.addComponent("alarmStatus", sweHabGen.getAlarmBurglarSWE()); // dataRecord(2)
    	genComp.addComponent("locationLLA", sweHabGen.getLocVecSWE()); // dataRecord(3, 4, 5)
    	genComp.addComponent("locationDesc", sweHabGen.getLocDescSWE()); // dataRecord(6)
		
    	// also generate encoding definition
    	genEncoding = sweHelpGen.newTextEncoding(",", "\n");
    }
    
    protected void postGeneralData(OpenHabThings genThing, OpenHabItems genItem)
    {
//    	System.out.println("posting General Alarm data for " + genItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (genThing.getLocation().isEmpty()) ? "undeclared" : genThing.getLocation();

    	// build and publish databook
    	DataBlock dataBlock = genComp.createDataBlock();
    	dataBlock.setStringValue(0, genItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setStringValue(2, genItem.getState());
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabAlarmGenOutput.this, dataBlock));
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return genComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return genEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
