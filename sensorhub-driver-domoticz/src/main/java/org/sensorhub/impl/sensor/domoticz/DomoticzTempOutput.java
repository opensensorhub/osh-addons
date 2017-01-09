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

public class DomoticzTempOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent tempComp;
	DataEncoding tempEncoding;
	DataBlock tempBlock;
	
	public DomoticzTempOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzTempData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Temp SWE Template");
    	
    	SWEHelper sweHelpTemp = new SWEHelper();
    	DomoticzSWEHelper sweDomTemp = new DomoticzSWEHelper();
    	
    	tempComp = sweHelpTemp.newDataRecord(8);
    	tempComp.setName(getName());
    	tempComp.setDefinition("http://sensorml.com/ont/swe/property/Temp");
    	
    	tempComp.addComponent("idx", sweDomTemp.getIdxSWE()); // dataRecord(0)
    	tempComp.addComponent("name", sweDomTemp.getNameSWE()); // dataRecord(1)
    	tempComp.addComponent("time", sweHelpTemp.newTimeStampIsoUTC()); // dataRecord(2)
    	tempComp.addComponent("temperature", sweDomTemp.getTempSWE()); // dataRecord(3)
    	tempComp.addComponent("latLonAlt", sweDomTemp.getLocVecSWE()); // dataRecord(4, 5, 6)
    	tempComp.addComponent("locationDesc", sweDomTemp.getLocDescSWE()); // dataRecord(7)

    	// also generate encoding definition
    	tempEncoding = sweHelpTemp.newTextEncoding(",", "\n");
    }
    
    protected void postTempData(DomoticzResponse domTempData, ValidDevice validTemp)
    {
//    	System.out.println("posting Temp data for idx " + domTempData.getResult()[0].getIdx());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	String locDesc = (validTemp.getValidLocDesc().isEmpty()) ? "undeclared" : validTemp.getValidLocDesc();

    	// build and publish databook
    	DataBlock dataBlock = tempComp.createDataBlock();
    	dataBlock.setStringValue(0, domTempData.getResult()[0].getIdx());
    	dataBlock.setStringValue(1, domTempData.getResult()[0].getName());
    	dataBlock.setDoubleValue(2, time);
    	dataBlock.setDoubleValue(3, domTempData.getResult()[0].getTemp());
    	dataBlock.setDoubleValue(4, validTemp.getValidLocationLLA().getLat());
    	dataBlock.setDoubleValue(5, validTemp.getValidLocationLLA().getLon());
    	dataBlock.setDoubleValue(6, validTemp.getValidLocationLLA().getAlt());
    	dataBlock.setStringValue(7, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzTempOutput.this, dataBlock)); 
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
