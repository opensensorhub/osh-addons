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

public class OpenHabDimmerOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent dimmerComp;
	DataEncoding dimmerEncoding;
	DataBlock dimmerBlock;
	
	public OpenHabDimmerOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABDimmerData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Dimmer SWE Template");
    	
    	SWEHelper sweHelpDimmer = new SWEHelper();
    	OpenHabSWEHelper sweHabDimmer = new OpenHabSWEHelper();
    	
    	dimmerComp = sweHelpDimmer.newDataRecord(7);
    	dimmerComp.setName(getName());
    	dimmerComp.setDefinition("http://sensorml.com/ont/swe/property/Dimmer");

    	dimmerComp.addComponent("name", sweHabDimmer.getNameSWE()); // dataRecord(0)
    	dimmerComp.addComponent("time", sweHelpDimmer.newTimeStampIsoUTC()); // dataRecord(1)
    	dimmerComp.addComponent("setLevel", sweHabDimmer.getSetLevelSWE()); // dataRecord(2)
    	dimmerComp.addComponent("locationLLA", sweHabDimmer.getLocVecSWE()); // dataRecord(3, 4, 5)
    	dimmerComp.addComponent("locationDesc", sweHabDimmer.getLocDescSWE()); // dataRecord(6)

    	// also generate encoding definition
    	dimmerEncoding = sweHelpDimmer.newTextEncoding(",", "\n");
    }
    
    protected void postDimmerData(OpenHabThings dimThing, OpenHabItems dimItem)
    {
//    	System.out.println("posting Dimmer data for " + dimItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (dimThing.getLocation().isEmpty()) ? "undeclared" : dimThing.getLocation();
    	
    	// build and publish databook
    	DataBlock dataBlock = dimmerComp.createDataBlock();
    	dataBlock.setStringValue(0, dimItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setIntValue(2, Integer.parseInt(dimItem.getState()));
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabDimmerOutput.this, dataBlock)); 
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return dimmerComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return dimmerEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
