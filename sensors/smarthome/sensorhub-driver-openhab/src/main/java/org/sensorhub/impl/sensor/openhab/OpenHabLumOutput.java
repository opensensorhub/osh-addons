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

public class OpenHabLumOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent lumComp;
	DataEncoding lumEncoding;
	DataBlock lumBlock;
	
	public OpenHabLumOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABLumData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Illuminance SWE Template");
    	
    	SWEHelper sweHelpLum = new SWEHelper();
    	OpenHabSWEHelper sweHabLum = new OpenHabSWEHelper();
    	
    	lumComp = sweHelpLum.newDataRecord(7);
    	lumComp.setName(getName());
    	lumComp.setDefinition("http://sensorml.com/ont/swe/property/Illuminance");
    	
    	lumComp.addComponent("name", sweHabLum.getNameSWE()); // dataRecord(0)
    	lumComp.addComponent("time", sweHelpLum.newTimeStampIsoUTC()); // dataRecord(1)
    	lumComp.addComponent("lux", sweHabLum.getLumSWE()); // dataRecord(2)
    	lumComp.addComponent("locationLLA", sweHabLum.getLocVecSWE()); // dataRecord(3, 4, 5)
    	lumComp.addComponent("locationDesc", sweHabLum.getLocDescSWE()); // dataRecord(6)
		
    	// also generate encoding definition
    	lumEncoding = sweHelpLum.newTextEncoding(",", "\n");
    }
    
    protected void postLumData(OpenHabThings lumThing, OpenHabItems lumItem)
    {
//    	System.out.println("posting Luminance data for " + lumItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	float lum = Float.NaN;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (lumThing.getLocation().isEmpty()) ? "undeclared" : lumThing.getLocation();
    	
    	if (!lumItem.getState().equalsIgnoreCase("NULL"))
    		lum = Float.parseFloat(lumItem.getState());

    	// build and publish databook
    	DataBlock dataBlock = lumComp.createDataBlock();
    	dataBlock.setStringValue(0, lumItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setFloatValue(2, lum);
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabLumOutput.this, dataBlock));
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return lumComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return lumEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
