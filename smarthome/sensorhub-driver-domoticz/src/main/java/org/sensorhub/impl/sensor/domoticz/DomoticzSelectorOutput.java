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

public class DomoticzSelectorOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent selectorComp;
	DataEncoding selectorEncoding;
	DataBlock selectorBlock;
	
	public DomoticzSelectorOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzSelectorData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Selector SWE Template");
    	
    	SWEHelper sweHelpSelector = new SWEHelper();
    	DomoticzSWEHelper sweDomSelector = new DomoticzSWEHelper();
    	
    	selectorComp = sweHelpSelector.newDataRecord(9);
    	selectorComp.setName(getName());
    	selectorComp.setDefinition("http://sensorml.com/ont/swe/property/Selector");

    	selectorComp.addComponent("idx", sweDomSelector.getIdxSWE()); // dataRecord(0)
    	selectorComp.addComponent("name", sweDomSelector.getNameSWE()); // dataRecord(1)
    	selectorComp.addComponent("time", sweHelpSelector.newTimeStampIsoUTC()); // dataRecord(2)
    	selectorComp.addComponent("switchState", sweDomSelector.getSwitchStateSWE()); // dataRecord(3)
    	selectorComp.addComponent("setLevel", sweDomSelector.getSetLevelSWE()); // dataRecord(4)
    	selectorComp.addComponent("latLonAlt", sweDomSelector.getLocVecSWE()); // dataRecord(5, 6, 7)
    	selectorComp.addComponent("locationDesc", sweDomSelector.getLocDescSWE()); // dataRecord(8)

    	// also generate encoding definition
    	selectorEncoding = sweHelpSelector.newTextEncoding(",", "\n");
    }
    
    protected void postSelectorData(DomoticzResponse domSelectorData, ValidDevice validSelector)
    {
//    	System.out.println("posting Selector data for idx " + validSelector.getValidIdx());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	String locDesc = (validSelector.getValidLocDesc().isEmpty()) ? "undeclared" : validSelector.getValidLocDesc();

    	// build and publish databook
    	DataBlock dataBlock = selectorComp.createDataBlock();
    	dataBlock.setStringValue(0, domSelectorData.getResult()[0].getIdx());
    	dataBlock.setStringValue(1, domSelectorData.getResult()[0].getName());
    	dataBlock.setDoubleValue(2, time);
    	dataBlock.setStringValue(3, domSelectorData.getResult()[0].getStatus());
    	dataBlock.setIntValue(4, domSelectorData.getResult()[0].getLevel());
    	dataBlock.setDoubleValue(5, validSelector.getValidLocationLLA().getLat());
    	dataBlock.setDoubleValue(6, validSelector.getValidLocationLLA().getLon());
    	dataBlock.setDoubleValue(7, validSelector.getValidLocationLLA().getAlt());
    	dataBlock.setStringValue(8, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzSelectorOutput.this, dataBlock)); 
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return selectorComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return selectorEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
