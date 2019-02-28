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

public class DomoticzTempHumOutput extends AbstractSensorOutput<DomoticzDriver>
{
	DataComponent tempHumComp;
	DataEncoding tempHumEncoding;
	DataBlock tempHumBlock;

	public DomoticzTempHumOutput(DomoticzDriver parentSensor) {
		super(parentSensor);
	}
	
	@Override
    public String getName()
    {
        return "DomoticzTempHumData";
    }


    protected void init() throws IOException
    {
//    	System.out.println("Adding Temp/Hum SWE Template");
    	
    	SWEHelper sweHelpTempHum = new SWEHelper();
    	DomoticzSWEHelper sweDomTempHum = new DomoticzSWEHelper();
    	
    	tempHumComp = sweHelpTempHum.newDataRecord(9);
    	tempHumComp.setName(getName());
    	tempHumComp.setDefinition("http://sensorml.com/ont/swe/property/TempHum");

    	tempHumComp.addComponent("idx", sweDomTempHum.getIdxSWE()); // dataRecord(0)
    	tempHumComp.addComponent("name", sweDomTempHum.getNameSWE()); // dataRecord(1)
    	tempHumComp.addComponent("time", sweHelpTempHum.newTimeStampIsoUTC()); // dataRecord(2)
    	tempHumComp.addComponent("temperature", sweDomTempHum.getTempSWE()); // dataRecord(3)
    	tempHumComp.addComponent("relativeHumidity", sweDomTempHum.getRelHumSWE()); // dataRecord(4)
    	tempHumComp.addComponent("latLonAlt", sweDomTempHum.getLocVecSWE()); // dataRecord(5, 6, 7)
    	tempHumComp.addComponent("locationDesc", sweDomTempHum.getLocDescSWE()); // dataRecord(8)

    	// also generate encoding definition
    	tempHumEncoding = sweHelpTempHum.newTextEncoding(",", "\n");
    }
    
    protected void postTempHumData(DomoticzResponse domTempHumData, ValidDevice validTempHum)
    {
//    	System.out.println("posting Temp/Hum data for idx " + validTempHum.getValidIdx());
    	
    	double time = System.currentTimeMillis() / 1000.;
    	String locDesc = (validTempHum.getValidLocDesc().isEmpty()) ? "undeclared" : validTempHum.getValidLocDesc();

    	// build and publish databook
    	DataBlock dataBlock = tempHumComp.createDataBlock();
    	dataBlock.setStringValue(0, domTempHumData.getResult()[0].getIdx());
    	dataBlock.setStringValue(1, domTempHumData.getResult()[0].getName());
    	dataBlock.setDoubleValue(2, time);
    	dataBlock.setDoubleValue(3, domTempHumData.getResult()[0].getTemp());
    	dataBlock.setDoubleValue(4, domTempHumData.getResult()[0].getHumidity());
    	dataBlock.setDoubleValue(5, validTempHum.getValidLocationLLA().getLat());
    	dataBlock.setDoubleValue(6, validTempHum.getValidLocationLLA().getLon());
    	dataBlock.setDoubleValue(7, validTempHum.getValidLocationLLA().getAlt());
    	dataBlock.setStringValue(8, locDesc);
    	
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, DomoticzTempHumOutput.this, dataBlock));
    }
    
    protected void start()
    {
    }
    

    protected void stop()
    {
    }
    
	@Override
	public DataComponent getRecordDescription() {
		return tempHumComp;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return tempHumEncoding;
	}

	@Override
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
