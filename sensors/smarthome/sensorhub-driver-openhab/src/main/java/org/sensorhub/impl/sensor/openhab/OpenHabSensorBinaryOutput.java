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

public class OpenHabSensorBinaryOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent binComp;
	DataEncoding binEncoding;
	DataBlock binBlock;
	
	public OpenHabSensorBinaryOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABSensorBinaryData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Sensor Binary SWE Template");
    	
    	SWEHelper sweHelpBin = new SWEHelper();
    	OpenHabSWEHelper sweHabBin = new OpenHabSWEHelper();
    	
    	binComp = sweHelpBin.newDataRecord(7);
    	binComp.setName(getName());
    	binComp.setDefinition("http://sensorml.com/ont/swe/property/SensorBinary");
    	
    	binComp.addComponent("name", sweHabBin.getNameSWE()); // dataRecord(0)
    	binComp.addComponent("time", sweHelpBin.newTimeStampIsoUTC()); // dataRecord(1)
    	binComp.addComponent("sensorStatus", sweHabBin.getSensorBinarySWE()); // dataRecord(2)
    	binComp.addComponent("locationLLA", sweHabBin.getLocVecSWE()); // dataRecord(3, 4, 5)
    	binComp.addComponent("locationDesc", sweHabBin.getLocDescSWE()); // dataRecord(6)
		
    	// also generate encoding definition
    	binEncoding = sweHelpBin.newTextEncoding(",", "\n");
    }
    
    protected void postBinData(OpenHabThings binThing, OpenHabItems binItem)
    {
//    	System.out.println("posting Sensor Binary data for " + binItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (binThing.getLocation().isEmpty()) ? "undeclared" : binThing.getLocation();

    	// build and publish databook
    	DataBlock dataBlock = binComp.createDataBlock();
    	dataBlock.setStringValue(0, binItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setStringValue(2, binItem.getState());
    	dataBlock.setDoubleValue(3, lat);
    	dataBlock.setDoubleValue(4, lon);
    	dataBlock.setDoubleValue(5, alt);
    	dataBlock.setStringValue(6, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabSensorBinaryOutput.this, dataBlock));
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return binComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return binEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
