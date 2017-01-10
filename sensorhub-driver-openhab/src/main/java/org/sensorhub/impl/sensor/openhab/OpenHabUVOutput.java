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

public class OpenHabUVOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent uvComp;
	DataEncoding uvEncoding;
	DataBlock uvBlock;
	
	public OpenHabUVOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABUltravioletData";
    }


	// Ultraviolet output results in a 500 error when requesting the
	// sensorhub/sos observed property...haven't figured out why
    protected void init() throws IOException
    {
//    	System.out.println("Adding UV SWE Template");
    	
    	SWEHelper sweHelpUV = new SWEHelper();
    	OpenHabSWEHelper sweHabUV = new OpenHabSWEHelper();
    	
    	uvComp = sweHelpUV.newDataRecord(7);
    	uvComp.setName(getName());
    	uvComp.setDefinition("http://sensorml.com/ont/swe/property/Ultraviolet");

    	uvComp.addComponent("name", sweHabUV.getNameSWE()); // dataRecord(0)
    	uvComp.addComponent("time", sweHelpUV.newTimeStampIsoUTC()); // dataRecord(1)
    	uvComp.addComponent("ultravioletIndex", sweHabUV.getUVISWE()); // dataRecord(2)
    	uvComp.addComponent("locationLLA", sweHabUV.getLocVecSWE()); // dataRecord(3, 4, 5)
    	uvComp.addComponent("locationDesc", sweHabUV.getLocDescSWE()); // dataRecord(6)
    }
    
    
    protected void postUVData(OpenHabThings uvThing, OpenHabItems uvItem)
    {
//    	System.out.println("posting UV data for " + uvItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double uvi = -1.0;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (uvThing.getLocation().isEmpty()) ? "undeclared" : uvThing.getLocation();
    	
    	if (!uvItem.getState().equalsIgnoreCase("NULL"))
    		uvi = Double.parseDouble(uvItem.getState());

    	// build and publish databook
    	DataBlock dataBlock = uvComp.createDataBlock();
    	dataBlock.setStringValue(0, uvItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setIntValue(2, (int)Math.round(uvi));
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabUVOutput.this, dataBlock)); 
    }
    
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return uvComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return uvEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
