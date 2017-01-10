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

public class OpenHabAlarmBurglarOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent burgComp;
	DataEncoding burgEncoding;
	DataBlock burgBlock;
	
	public OpenHabAlarmBurglarOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABAlarmBurglarData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Burglar Alarm SWE Template");
    	
    	SWEHelper sweHelpBurg = new SWEHelper();
    	OpenHabSWEHelper sweHabBurg = new OpenHabSWEHelper();
    	
    	burgComp = sweHelpBurg.newDataRecord(7);
    	burgComp.setName(getName());
    	burgComp.setDefinition("http://sensorml.com/ont/swe/property/BurglarAlarm");
    	
    	burgComp.addComponent("name", sweHabBurg.getNameSWE()); // dataRecord(0)
    	burgComp.addComponent("time", sweHelpBurg.newTimeStampIsoUTC()); // dataRecord(1)
    	burgComp.addComponent("alarmStatus", sweHabBurg.getAlarmBurglarSWE()); // dataRecord(2)
    	burgComp.addComponent("locationLLA", sweHabBurg.getLocVecSWE()); // dataRecord(3, 4, 5)
    	burgComp.addComponent("locationDesc", sweHabBurg.getLocDescSWE()); // dataRecord(6)
		
    	// also generate encoding definition
    	burgEncoding = sweHelpBurg.newTextEncoding(",", "\n");
    }
    
    protected void postBurglarData(OpenHabThings burgThing, OpenHabItems burgItem)
    {
//    	System.out.println("posting Burglar Alarm data for " + burgItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (burgThing.getLocation().isEmpty()) ? "undeclared" : burgThing.getLocation();

    	// build and publish databook
    	DataBlock dataBlock = burgComp.createDataBlock();
    	dataBlock.setStringValue(0, burgItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setStringValue(2, burgItem.getState());
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabAlarmBurglarOutput.this, dataBlock));
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return burgComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return burgEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
