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

public class OpenHabBatteryOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent battComp;
	DataEncoding battEncoding;
	DataBlock battBlock;
	
	public OpenHabBatteryOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABBatteryData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Battery SWE Template");
    	
    	SWEHelper sweHelpBatt = new SWEHelper();
    	OpenHabSWEHelper sweHabBatt = new OpenHabSWEHelper();
    	
    	battComp = sweHelpBatt.newDataRecord(7);
    	battComp.setName(getName());
    	battComp.setDefinition("http://sensorml.com/ont/swe/property/Battery");
    	
    	battComp.addComponent("name", sweHabBatt.getNameSWE()); // dataRecord(0)
    	battComp.addComponent("time", sweHelpBatt.newTimeStampIsoUTC()); // dataRecord(1)
    	battComp.addComponent("batteryLevel", sweHabBatt.getBatteryLevelSWE()); // dataRecord(2)
    	battComp.addComponent("locationLLA", sweHabBatt.getLocVecSWE()); // dataRecord(3, 4, 5)
    	battComp.addComponent("locationDesc", sweHabBatt.getLocDescSWE()); // dataRecord(6)
		
    	// also generate encoding definition
    	battEncoding = sweHelpBatt.newTextEncoding(",", "\n");
    }
    
    protected void postBatteryData(OpenHabThings battThing, OpenHabItems battItem)
    {
//    	System.out.println("posting Battery Level data for " + battItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	int batt = -1;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (battThing.getLocation().isEmpty()) ? "undeclared" : battThing.getLocation();
    	
    	if (!battItem.getState().equalsIgnoreCase("NULL"))
    		batt = Integer.parseInt(battItem.getState());

    	// build and publish databook
    	DataBlock dataBlock = battComp.createDataBlock();
    	dataBlock.setStringValue(0, battItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setIntValue(2, batt);
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabBatteryOutput.this, dataBlock));
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return battComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return battEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
