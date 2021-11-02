package org.sensorhub.impl.sensor.isa;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.isa.ISASimulation.RandomWalk;

public class AtmosPressureOutput extends ISAOutput
{
    RandomWalk pressValue = new RandomWalk(101325, 1000, 10, 87000, 105000);
    
    
    public AtmosPressureOutput(ISASensor parentSensor)
    {
        super("press", parentSensor);
        ISAHelper isa = new ISAHelper();
        
        // output structure
        dataStruct = isa.createRecord()
            .name(this.name)
            .definition(ISAHelper.getIsaUri("Atmospheric_Pressure"))
            .label("Atmospheric Pressure")
            .addSamplingTimeIsoUTC("time")
            .addField("value", isa.createQuantity()
                .definition(ISAHelper.getCfUri("air_pressure"))
                .description("The measurement of the force per unit area in pascals (newtons per square meter).")
                .uomCode("Pa"))
            .addField("error", isa.errorField("Pa"))
            .build();
                
        // default output encoding
        dataEnc = isa.newTextEncoding(",", "\n");
    }
    
    
    @Override
    public double getAverageSamplingPeriod()
    {
        return 60.;
    }


    @Override
    protected void sendSimulatedMeasurement()
    {
        var now = parentSensor.getCurrentTime();
        if (nextRecordTime > now)
            return;
        
        var dataBlk = dataStruct.createDataBlock();
        
        int i = 0;        
        dataBlk.setDoubleValue(i++, ((double)now)/1000.);
        dataBlk.setDoubleValue(i++, pressValue.next()); // value
        dataBlk.setDoubleValue(i++, 10); // error
        
        nextRecordTime = now + (long)(getAverageSamplingPeriod()*1000);
        latestRecordTime = now;
        latestRecord = dataBlk;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlk));
    }
}
