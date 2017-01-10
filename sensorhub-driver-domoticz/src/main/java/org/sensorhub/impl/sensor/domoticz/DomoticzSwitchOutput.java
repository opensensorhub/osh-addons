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

public class DomoticzSwitchOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent switchComp;
	DataEncoding switchEncoding;
	DataBlock switchBlock;
	
	public DomoticzSwitchOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzSwitchData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Switch SWE Template");
    	
    	SWEHelper sweHelpSwitch = new SWEHelper();
    	DomoticzSWEHelper sweDomSwitch = new DomoticzSWEHelper();
    	
    	switchComp = sweHelpSwitch.newDataRecord(8);
    	switchComp.setName(getName());
    	switchComp.setDefinition("http://sensorml.com/ont/swe/property/Switch");
    	
    	switchComp.addComponent("idx", sweDomSwitch.getIdxSWE()); // dataRecord(0)
    	switchComp.addComponent("name", sweDomSwitch.getNameSWE()); // dataRecord(1)
    	switchComp.addComponent("time", sweHelpSwitch.newTimeStampIsoUTC()); // dataRecord(2)
    	switchComp.addComponent("switchState", sweDomSwitch.getSwitchStateSWE()); // dataRecord(3)
    	switchComp.addComponent("latLonAlt", sweDomSwitch.getLocVecSWE()); // dataRecord(4, 5, 6)
    	switchComp.addComponent("locationDesc", sweDomSwitch.getLocDescSWE()); // dataRecord(7)

    	// also generate encoding definition
    	switchEncoding = sweHelpSwitch.newTextEncoding(",", "\n");
    }
    
    protected void postSwitchData(DomoticzResponse domSwitchData, ValidDevice validSwitch)
    {
//    	System.out.println("posting Switch data for idx " + validSwitch.getValidIdx());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	String locDesc = (validSwitch.getValidLocDesc().isEmpty()) ? "undeclared" : validSwitch.getValidLocDesc();

    	// build and publish databook
    	DataBlock dataBlock = switchComp.createDataBlock();
    	dataBlock.setStringValue(0, domSwitchData.getResult()[0].getIdx());
    	dataBlock.setStringValue(1, domSwitchData.getResult()[0].getName());
    	dataBlock.setDoubleValue(2, time);
    	dataBlock.setStringValue(3, domSwitchData.getResult()[0].getStatus());
    	dataBlock.setDoubleValue(4, validSwitch.getValidLocationLLA().getLat());
    	dataBlock.setDoubleValue(5, validSwitch.getValidLocationLLA().getLon());
    	dataBlock.setDoubleValue(6, validSwitch.getValidLocationLLA().getAlt());
    	dataBlock.setStringValue(7, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzSwitchOutput.this, dataBlock));
    }
    
    protected void start()
    { 
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return switchComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return switchEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
