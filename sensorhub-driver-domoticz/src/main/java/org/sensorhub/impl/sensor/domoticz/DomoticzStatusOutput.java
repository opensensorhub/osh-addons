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

public class DomoticzStatusOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent statusComp;
	DataEncoding statusEncoding;
	DataBlock statusBlock;
	
	public DomoticzStatusOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzStatusData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Status SWE Template");
    	
    	SWEHelper sweHelpStatus = new SWEHelper();
    	DomoticzSWEHelper sweDomStatus = new DomoticzSWEHelper();
    	
    	statusComp = sweHelpStatus.newDataRecord(11);
    	statusComp.setName(getName());
    	statusComp.setDefinition("http://sensorml.com/ont/swe/property/DomoticzStatus");
    	
    	statusComp.addComponent("idx", sweDomStatus.getIdxSWE()); // dataRecord(0)
    	statusComp.addComponent("name", sweDomStatus.getNameSWE()); // dataRecord(1)
    	statusComp.addComponent("time", sweHelpStatus.newTimeStampIsoUTC()); // dataRecord(2)
    	statusComp.addComponent("sensorType", sweDomStatus.getSensorTypeSWE()); // dataRecord(3)
    	statusComp.addComponent("sensorSubtype", sweDomStatus.getSensorSubTypeSWE()); // dataRecord(4)
    	statusComp.addComponent("batteryLevel", sweDomStatus.getBatteryLevelSWE()); // dataRecord(5)
    	statusComp.addComponent("data", sweDomStatus.getDataSWE()); // dataRecord(6)
    	statusComp.addComponent("latLonAlt", sweDomStatus.getLocVecSWE()); // dataRecord(7, 9, 9)
    	statusComp.addComponent("locationDesc", sweDomStatus.getLocDescSWE()); // dataRecord(10)

    	// also generate encoding definition
    	statusEncoding = sweHelpStatus.newTextEncoding(",", "\n");
    }
    
    protected void postStatusData(DomoticzResponse domStatusData, ValidDevice validStatus)
    {
//    	System.out.println("posting Status data for idx " + domStatusData.getResult()[0].getIdx());
    	
    	int batt = (domStatusData.getResult()[0].getBatteryLevel() != 255) ? domStatusData.getResult()[0].getBatteryLevel():-1;
    	double time = System.currentTimeMillis() / 1000.;
    	String locDesc = (validStatus.getValidLocDesc().isEmpty()) ? "undeclared" : validStatus.getValidLocDesc();

    	// build and publish databook
    	DataBlock dataBlock = statusComp.createDataBlock();
    	dataBlock.setStringValue(0, domStatusData.getResult()[0].getIdx());
    	dataBlock.setStringValue(1, domStatusData.getResult()[0].getName());
    	dataBlock.setDoubleValue(2, time);
    	dataBlock.setStringValue(3, domStatusData.getResult()[0].getType()); // Type given by domoticz
    	dataBlock.setStringValue(4, validStatus.getValidType().toString()); // Subtype given by user
    	dataBlock.setIntValue(5, batt);
    	dataBlock.setStringValue(6, domStatusData.getResult()[0].getData());
    	dataBlock.setDoubleValue(7, validStatus.getValidLocationLLA().getLat());
    	dataBlock.setDoubleValue(8, validStatus.getValidLocationLLA().getLon());
    	dataBlock.setDoubleValue(9, validStatus.getValidLocationLLA().getAlt());
    	dataBlock.setStringValue(10, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzStatusOutput.this, dataBlock)); 
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
