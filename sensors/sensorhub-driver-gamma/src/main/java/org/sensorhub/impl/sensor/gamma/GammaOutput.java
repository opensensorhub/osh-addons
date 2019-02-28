package org.sensorhub.impl.sensor.gamma;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

public class GammaOutput extends AbstractSensorOutput<GammaSensor>
{
    DataComponent dataStruct;
    DataEncoding dataEnc;
    
    public GammaOutput(GammaSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "gammaExposure";
    }


    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.newDataRecord(3);
        dataStruct.setName(getName());
        
        // build SWE Common record structure
        dataStruct.setDefinition("http://sensorml.com/ont/swe/property/DoseRate");
        dataStruct.setDescription("Radiation Dose");
        
        /************************* Add appropriate data fields *******************************************************************************/
        // add time, average, and instantaneous radiation exposure levels
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        dataStruct.addComponent("DoseRateAvg", fac.newQuantity(SWEHelper.getPropertyUri("DoseRate"), "Dose Rate Average", null, "uR/h"));
        dataStruct.addComponent("DoseRateInst", fac.newQuantity(SWEHelper.getPropertyUri("DoseRate"), "Dose Instant", null, "uR"));
        /*************************************************************************************************************************************/
        
        // also generate encoding definition
        dataEnc = fac.newTextEncoding(",", "\n");
    }


    @Override
    public double getAverageSamplingPeriod()
    {
    	// sample every 1 second
        return 1.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEnc;
    }
    
    
    protected void sendOutput(long msgTime, int AvgDose, int InsDose)
    {
    	DataBlock dataBlock = dataStruct.createDataBlock();
    	dataBlock.setDoubleValue(0, msgTime);
    	dataBlock.setDoubleValue(1, AvgDose);
    	dataBlock.setDoubleValue(2, InsDose);
    	
    }
}
