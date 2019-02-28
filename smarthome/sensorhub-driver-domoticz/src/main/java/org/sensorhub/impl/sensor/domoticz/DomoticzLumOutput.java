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

public class DomoticzLumOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent lumComp;
	DataEncoding lumEncoding;
	DataBlock lumBlock;
	
	public DomoticzLumOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzLumData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Illuminance SWE Template");
    	
    	SWEHelper sweHelpLum = new SWEHelper();
    	DomoticzSWEHelper sweDomLum = new DomoticzSWEHelper();
    	
    	lumComp = sweHelpLum.newDataRecord(8);
    	lumComp.setName(getName());
    	lumComp.setDefinition("http://sensorml.com/ont/swe/property/Illuminance");
    	
    	lumComp.addComponent("idx", sweDomLum.getIdxSWE()); // dataRecord(0)
    	lumComp.addComponent("name", sweDomLum.getNameSWE()); // dataRecord(1)
    	lumComp.addComponent("time", sweHelpLum.newTimeStampIsoUTC()); // dataRecord(2)
    	lumComp.addComponent("lux", sweDomLum.getLumSWE()); // dataRecord(3)
    	lumComp.addComponent("latLonAlt", sweDomLum.getLocVecSWE()); // dataRecord(4, 5, 6)
    	lumComp.addComponent("locationDesc", sweDomLum.getLocDescSWE()); // dataRecord(7)
		
    	// also generate encoding definition
    	lumEncoding = sweHelpLum.newTextEncoding(",", "\n");
    }
    
    protected void postLumData(DomoticzResponse domLumData, ValidDevice validLum)
    {
//    	System.out.println("posting Luminance data for idx " + validLum.getValidIdx());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	String locDesc = (validLum.getValidLocDesc().isEmpty()) ? "undeclared" : validLum.getValidLocDesc();

    	// build and publish databook
    	DataBlock dataBlock = lumComp.createDataBlock();
    	dataBlock.setStringValue(0, domLumData.getResult()[0].getIdx());
    	dataBlock.setStringValue(1, domLumData.getResult()[0].getName());
    	dataBlock.setDoubleValue(2, time);
    	dataBlock.setIntValue(3, Integer.parseInt(domLumData.getResult()[0].getData().replaceAll("\\s", "").replaceAll("Lux", "")));
    	dataBlock.setDoubleValue(4, validLum.getValidLocationLLA().getLat());
    	dataBlock.setDoubleValue(5, validLum.getValidLocationLLA().getLon());
    	dataBlock.setDoubleValue(6, validLum.getValidLocationLLA().getAlt());
    	dataBlock.setStringValue(7, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzLumOutput.this, dataBlock));
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
