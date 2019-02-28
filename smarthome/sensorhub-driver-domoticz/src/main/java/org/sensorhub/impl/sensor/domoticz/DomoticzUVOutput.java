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

public class DomoticzUVOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent uvComp;
	DataEncoding uvEncoding;
	DataBlock uvBlock;
	
	public DomoticzUVOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzUltravioletData";
    }


	// Ultraviolet output results in a 500 error when requesting the
	// sensorhub/sos observed property...haven't figured out why
    protected void init() throws IOException
    {
//    	System.out.println("Adding UV SWE Template");
    	
    	SWEHelper sweHelpUV = new SWEHelper();
    	DomoticzSWEHelper sweDomUV = new DomoticzSWEHelper();
    	
    	uvComp = sweHelpUV.newDataRecord(8);
    	uvComp.setName(getName());
    	uvComp.setDefinition("http://sensorml.com/ont/swe/property/Ultraviolet");

    	uvComp.addComponent("idx", sweDomUV.getIdxSWE()); // dataRecord(0)
    	uvComp.addComponent("name", sweDomUV.getNameSWE()); // dataRecord(1)
    	uvComp.addComponent("time", sweHelpUV.newTimeStampIsoUTC()); // dataRecord(2)
    	uvComp.addComponent("ultravioletIndex", sweDomUV.getUVISWE()); // dataRecord(3)
    	uvComp.addComponent("latLonAlt", sweDomUV.getLocVecSWE()); // dataRecord(4, 5, 6)
    	uvComp.addComponent("locationDesc", sweDomUV.getLocDescSWE()); // dataRecord(7)
    }
    
    
    protected void postUVData(DomoticzResponse domUVData, ValidDevice validUV)
    {
//    	System.out.println("posting UV data for idx " + validUV.getValidIdx());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	double uvi = Double.parseDouble(domUVData.getResult()[0].getUVI());
    	String locDesc = (validUV.getValidLocDesc().isEmpty()) ? "undeclared" : validUV.getValidLocDesc();

    	// build and publish databook
    	DataBlock dataBlock = uvComp.createDataBlock();
    	dataBlock.setStringValue(0, domUVData.getResult()[0].getIdx());
    	dataBlock.setStringValue(1, domUVData.getResult()[0].getName());
    	dataBlock.setDoubleValue(2, time);
    	dataBlock.setDoubleValue(3, uvi);
    	dataBlock.setDoubleValue(4, validUV.getValidLocationLLA().getLat());
    	dataBlock.setDoubleValue(5, validUV.getValidLocationLLA().getLon());
    	dataBlock.setDoubleValue(6, validUV.getValidLocationLLA().getAlt());
    	dataBlock.setStringValue(7, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzUVOutput.this, dataBlock)); 
    }
    
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return uvComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return uvEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
