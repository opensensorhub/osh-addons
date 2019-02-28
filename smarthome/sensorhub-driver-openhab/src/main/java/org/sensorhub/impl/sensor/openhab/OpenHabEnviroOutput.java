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

public class OpenHabEnviroOutput extends AbstractSensorOutput<OpenHabDriver>
{
	DataComponent enviroComp;
	DataEncoding enviroEncoding;
	DataBlock enviroBlock;
	
	public OpenHabEnviroOutput(OpenHabDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "OpenHABEnviroData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Enviro SWE Template");
    	
    	SWEHelper sweHelpEnviro = new SWEHelper();
    	OpenHabSWEHelper sweHabEnviro = new OpenHabSWEHelper();
    	
    	enviroComp = sweHelpEnviro.newDataRecord(8);
    	enviroComp.setName(getName());
    	enviroComp.setDefinition("http://sensorml.com/ont/swe/property/Environment");
    	
    	enviroComp.addComponent("name", sweHabEnviro.getNameSWE()); // dataRecord(0)
    	enviroComp.addComponent("time", sweHelpEnviro.newTimeStampIsoUTC()); // dataRecord(1)
    	enviroComp.addComponent("owningThing", sweHabEnviro.getOwningThingSWE()); // dataRecord(2)
    	enviroComp.addComponent("enviroData", sweHabEnviro.getEnviroDataSWE()); // dataRecord(3)
    	enviroComp.addComponent("locationLLA", sweHabEnviro.getLocVecSWE()); // dataRecord(4, 5, 6)
    	enviroComp.addComponent("locationDesc", sweHabEnviro.getLocDescSWE()); // dataRecord(7)

    	// also generate encoding definition
    	enviroEncoding = sweHelpEnviro.newTextEncoding(",", "\n");
    }
    
    protected void postEnviroData(OpenHabThings enviroThing, OpenHabItems enviroItem)
    {
//    	System.out.println("posting Enviro data for idx " + enviroItem.getName());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double lat = Double.NaN;
    	double lon = Double.NaN;
    	double alt = Double.NaN;
    	String locDesc = (enviroThing.getLocation().isEmpty()) ? "undeclared" : enviroThing.getLocation();

    	// build and publish databook
    	DataBlock dataBlock = enviroComp.createDataBlock();
    	dataBlock.setStringValue(0, enviroItem.getName());
    	dataBlock.setDoubleValue(1, time);
    	dataBlock.setStringValue(2, enviroThing.getLabel()); // Owning Thing of Enviro Item
    	dataBlock.setStringValue(3, enviroItem.getState());
    	dataBlock.setDoubleValue(4, lat);
    	dataBlock.setDoubleValue(5, lon);
    	dataBlock.setDoubleValue(6, alt);
    	dataBlock.setStringValue(7, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, OpenHabEnviroOutput.this, dataBlock)); 
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return enviroComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return enviroEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
