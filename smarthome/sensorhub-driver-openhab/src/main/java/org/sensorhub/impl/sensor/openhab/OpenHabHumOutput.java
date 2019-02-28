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

public class OpenHabHumOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent humComp;
	DataEncoding humEncoding;
	DataBlock humBlock;

	public OpenHabHumOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABHumData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding RelHum SWE Template");
    	
    	SWEHelper sweHelpHum = new SWEHelper();
    	OpenHabSWEHelper sweHabHum = new OpenHabSWEHelper();
    	
    	humComp = sweHelpHum.newDataRecord(7);
    	humComp.setName(getName());
    	humComp.setDefinition("http://sensorml.com/ont/swe/property/RelHum");

    	humComp.addComponent("name", sweHabHum.getNameSWE()); // dataRecord(0)
    	humComp.addComponent("time", sweHelpHum.newTimeStampIsoUTC()); // dataRecord(1)
    	humComp.addComponent("relativeHumidity", sweHabHum.getRelHumSWE()); // dataRecord(2)
    	humComp.addComponent("locationLLA", sweHabHum.getLocVecSWE()); // dataRecord(3, 4, 5)
    	humComp.addComponent("locationDesc", sweHabHum.getLocDescSWE()); // dataRecord(6)

    	// also generate encoding definition
    	humEncoding = sweHelpHum.newTextEncoding(",", "\n");
    }
    
    protected void postHumData(OpenHabThings humThing, OpenHabItems humItem)
    {
//    	System.out.println("posting Humidity data for " + humItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	float hum = Float.NaN;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (humThing.getLocation().isEmpty()) ? "undeclared" : humThing.getLocation();
    	
    	if (!humItem.getState().equalsIgnoreCase("NULL"))
    		hum = Float.parseFloat(humItem.getState());

    	// build and publish databook
    	DataBlock dataBlock = humComp.createDataBlock();
    	dataBlock.setStringValue(0, humItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setDoubleValue(2, Math.round(hum*100.0)/100.0);
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabHumOutput.this, dataBlock));
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return humComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return humEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
