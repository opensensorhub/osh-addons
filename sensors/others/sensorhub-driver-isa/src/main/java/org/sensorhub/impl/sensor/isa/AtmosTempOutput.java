package org.sensorhub.impl.sensor.isa;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.isa.ISASimulation.RandomWalk;

public class AtmosTempOutput extends ISAOutput
{
    RandomWalk tempValue = new RandomWalk(20.0, 5.0, 0.1, 0, 40);
    
    
    public AtmosTempOutput(ISASensor parentSensor)
    {
        super("temp", parentSensor);
        ISAHelper isa = new ISAHelper();
        
        // output structure
        dataStruct = isa.createRecord()
            .name(this.name)
            .definition(ISAHelper.getIsaUri("Atmospheric_Temperature"))
            .label("Atmospheric Temperature")
            .addSamplingTimeIsoUTC("time")
            .addField("value", isa.createQuantity()
                .definition(ISAHelper.getCfUri("air_temperature"))
                .description("Temperature measurement in degrees Celsius.")
                .uomCode("Cel"))
            .addField("error", isa.errorField("Cel"))
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
        dataBlk.setDoubleValue(i++, tempValue.next()); // value
        dataBlk.setDoubleValue(i++, 0.1); // error
        
        nextRecordTime = now + (long)(getAverageSamplingPeriod()*1000);
        latestRecordTime = now;
        latestRecord = dataBlk;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlk));
    }
}
