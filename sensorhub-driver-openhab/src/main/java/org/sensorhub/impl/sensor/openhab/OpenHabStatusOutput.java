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

public class OpenHabStatusOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent statusComp;
	DataEncoding statusEncoding;
	DataBlock statusBlock;
	
	public OpenHabStatusOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABStatusData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Status SWE Template");
    	
    	SWEHelper sweHelpStatus = new SWEHelper();
    	OpenHabSWEHelper sweHabStatus = new OpenHabSWEHelper();
    	
    	statusComp = sweHelpStatus.newDataRecord(10);
    	statusComp.setName(getName());
    	statusComp.setDefinition("http://sensorml.com/ont/swe/property/OpenHABStatus");
    	
    	statusComp.addComponent("name", sweHabStatus.getNameSWE()); // dataRecord(0)
    	statusComp.addComponent("time", sweHelpStatus.newTimeStampIsoUTC()); // dataRecord(1)
    	statusComp.addComponent("status", sweHabStatus.getThingStatusSWE()); // dataRecord(2)
    	statusComp.addComponent("bindingType", sweHabStatus.getBindingTypeSWE()); // dataRecord(3)
    	statusComp.addComponent("owningThing", sweHabStatus.getOwningThingSWE()); // dataRecord(4)
    	statusComp.addComponent("state", sweHabStatus.getItemStateSWE()); // dataRecord(5)
    	statusComp.addComponent("locationLLA", sweHabStatus.getLocVecSWE()); // dataRecord(6, 7, 8)
    	statusComp.addComponent("locationDesc", sweHabStatus.getLocDescSWE()); // dataRecord(9)

    	// also generate encoding definition
    	statusEncoding = sweHelpStatus.newTextEncoding(",", "\n");
    }
    
    protected void postStatusData(OpenHabThings statusThing, int kk, OpenHabItems statusItem)
    {
//    	System.out.println("posting Status data for " + statusItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (statusThing.getLocation().isEmpty()) ? "undeclared" : statusThing.getLocation();

    	// build and publish databook
    	DataBlock dataBlock = statusComp.createDataBlock();
    	dataBlock.setStringValue(0, statusItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setStringValue(2, statusThing.getStatusInfo().getStatus()); // "ONLINE" or "OFFLINE"
    	dataBlock.setStringValue(3, statusThing.getChannels()[kk].getChannelTypeUID()); // Channel Type
    	dataBlock.setStringValue(4, statusThing.getLabel()); // Name of owning "Thing"
    	dataBlock.setStringValue(5, statusItem.getState()); // Current value given by sensor
    	dataBlock.setDoubleValue(6, lat);
    	dataBlock.setDoubleValue(7, lon);
    	dataBlock.setDoubleValue(8, alt);
    	dataBlock.setStringValue(9, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabStatusOutput.this, dataBlock)); 
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
