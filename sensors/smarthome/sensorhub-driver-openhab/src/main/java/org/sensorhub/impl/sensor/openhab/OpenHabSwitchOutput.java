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

public class OpenHabSwitchOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent switchComp;
	DataEncoding switchEncoding;
	DataBlock switchBlock;
	
	public OpenHabSwitchOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABSwitchData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Switch SWE Template");
    	
    	SWEHelper sweHelpSwitch = new SWEHelper();
    	OpenHabSWEHelper sweHabSwitch = new OpenHabSWEHelper();
    	
    	switchComp = sweHelpSwitch.newDataRecord(7);
    	switchComp.setName(getName());
    	switchComp.setDefinition("http://sensorml.com/ont/swe/property/Switch");

    	switchComp.addComponent("name", sweHabSwitch.getNameSWE()); // dataRecord(0)
    	switchComp.addComponent("time", sweHelpSwitch.newTimeStampIsoUTC()); // dataRecord(1)
    	switchComp.addComponent("switchState", sweHabSwitch.getSwitchStateSWE()); // dataRecord(2)
    	switchComp.addComponent("locationLLA", sweHabSwitch.getLocVecSWE()); // dataRecord(3, 4, 5)
    	switchComp.addComponent("locationDesc", sweHabSwitch.getLocDescSWE()); // dataRecord(6)

    	// also generate encoding definition
    	switchEncoding = sweHelpSwitch.newTextEncoding(",", "\n");
    }
    
    protected void postSwitchData(OpenHabThings switchThing, OpenHabItems switchItem)
    {
//    	System.out.println("posting Switch data for " + switchItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (switchThing.getLocation().isEmpty()) ? "undeclared" : switchThing.getLocation();

    	// build and publish databook
    	DataBlock dataBlock = switchComp.createDataBlock();
    	dataBlock.setStringValue(0, switchItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setStringValue(2, switchItem.getState());
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabSwitchOutput.this, dataBlock));
    }
    
    protected void start()
    { 
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return switchComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return switchEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
