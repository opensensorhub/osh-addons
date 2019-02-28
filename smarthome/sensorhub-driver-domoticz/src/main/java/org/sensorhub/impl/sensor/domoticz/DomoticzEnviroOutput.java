package org.sensorhub.impl.sensor.domoticz;

import java.io.IOException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.domoticz.DomoticzDriver.ValidDevice;
import org.sensorhub.impl.sensor.domoticz.DomoticzHandler.DomoticzResponse;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

public class DomoticzEnviroOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent enviroComp;
	DataEncoding enviroEncoding;
	DataBlock enviroBlock;
	
	public DomoticzEnviroOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzEnviroData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Enviro SWE Template");
    	
    	SWEHelper sweHelpEnviro = new SWEHelper();
    	DomoticzSWEHelper sweDomEnviro = new DomoticzSWEHelper();
    	
    	enviroComp = sweHelpEnviro.newDataRecord(9);
    	enviroComp.setName(getName());
    	enviroComp.setDefinition("http://sensorml.com/ont/swe/property/Environment");
    	
    	enviroComp.addComponent("idx", sweDomEnviro.getIdxSWE()); // dataRecord(0)
    	enviroComp.addComponent("name", sweDomEnviro.getNameSWE()); // dataRecord(1)
    	enviroComp.addComponent("time", sweHelpEnviro.newTimeStampIsoUTC()); // dataRecord(2)
    	enviroComp.addComponent("sensorSubtype", sweDomEnviro.getSensorSubTypeSWE()); // dataRecord(3)
    	enviroComp.addComponent("enviroData", sweDomEnviro.getEnviroDataSWE()); // dataRecord(4)
    	enviroComp.addComponent("latLonAlt", sweDomEnviro.getLocVecSWE()); // dataRecord(5, 6, 7)
    	enviroComp.addComponent("locationDesc", sweDomEnviro.getLocDescSWE()); // dataRecord(8)

    	// also generate encoding definition
    	enviroEncoding = sweHelpEnviro.newTextEncoding(",", "\n");
    }
    
    protected void postEnviroData(DomoticzResponse domEnviroData, ValidDevice validEnviro)
    {
//    	System.out.println("posting Enviro data for idx " + domEnviroData.getResult()[0].getIdx());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	String locDesc = (validEnviro.getValidLocDesc().isEmpty()) ? "undeclared" : validEnviro.getValidLocDesc();

    	// build and publish databook
    	DataBlock dataBlock = enviroComp.createDataBlock();
    	dataBlock.setStringValue(0, domEnviroData.getResult()[0].getIdx());
    	dataBlock.setStringValue(1, domEnviroData.getResult()[0].getName());
    	dataBlock.setDoubleValue(2, time);
    	dataBlock.setStringValue(3, validEnviro.getValidType().toString()); // Subtype given by user
    	dataBlock.setStringValue(4, domEnviroData.getResult()[0].getData());
    	dataBlock.setDoubleValue(5, validEnviro.getValidLocationLLA().getLat());
    	dataBlock.setDoubleValue(6, validEnviro.getValidLocationLLA().getLon());
    	dataBlock.setDoubleValue(7, validEnviro.getValidLocationLLA().getAlt());
    	dataBlock.setStringValue(8, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzEnviroOutput.this, dataBlock)); 
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
