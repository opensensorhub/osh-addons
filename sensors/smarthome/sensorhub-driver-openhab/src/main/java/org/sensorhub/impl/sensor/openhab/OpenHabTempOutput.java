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

public class OpenHabTempOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent tempComp;
	DataEncoding tempEncoding;
	DataBlock tempBlock;
	
	public OpenHabTempOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABTempData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Temp SWE Template");
    	
    	SWEHelper sweHelpTemp = new SWEHelper();
    	OpenHabSWEHelper sweHabTemp = new OpenHabSWEHelper();
    	
    	tempComp = sweHelpTemp.newDataRecord(7);
    	tempComp.setName(getName());
    	tempComp.setDefinition("http://sensorml.com/ont/swe/property/Temp");
    	
    	tempComp.addComponent("name", sweHabTemp.getNameSWE()); // dataRecord(0)
    	tempComp.addComponent("time", sweHelpTemp.newTimeStampIsoUTC()); // dataRecord(1)
    	tempComp.addComponent("temperature", sweHabTemp.getTempSWE()); // dataRecord(2)
    	tempComp.addComponent("locationLLA", sweHabTemp.getLocVecSWE()); // dataRecord(3, 4, 5)
    	tempComp.addComponent("locationDesc", sweHabTemp.getLocDescSWE()); // dataRecord(6)

    	// also generate encoding definition
    	tempEncoding = sweHelpTemp.newTextEncoding(",", "\n");
    }
    
    protected void postTempData(OpenHabThings tempThing, OpenHabItems tempItem)
    {
//    	System.out.println("posting Temp data for " + tempItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	float temp = Float.NaN;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (tempThing.getLocation().isEmpty()) ? "undeclared" : tempThing.getLocation();

    	if (!tempItem.getState().equalsIgnoreCase("NULL"))
    		temp = Float.parseFloat(tempItem.getState());
    	
    	// build and publish databook
    	DataBlock dataBlock = tempComp.createDataBlock();
    	dataBlock.setStringValue(0, tempItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setDoubleValue(2, Math.round(temp*100.0)/100.0);
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabTempOutput.this, dataBlock)); 
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return tempComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return tempEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
