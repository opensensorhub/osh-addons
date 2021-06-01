package org.sensorhub.impl.sensor.isa;

import org.sensorhub.api.data.DataEvent;

public class AtmosPressureOutput extends ISAOutput
{
    
    public AtmosPressureOutput(ISASensor parentSensor)
    {
        super("press", parentSensor);
        ISAHelper isa = new ISAHelper();
        
        // output structure
        dataStruct = isa.createRecord()
            .name(this.name)
            .definition(ISAHelper.ISA_DEF_URI_BASE + "Atmospheric_Pressure")
            .label("Atmospheric Pressure")
            .addSamplingTimeIsoUTC("time")
            .addField("value", isa.createQuantity()
                .definition(ISAHelper.MMI_CF_DEF_URI_BASE + "air_pressure")
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


    protected long nextRecordTime = Long.MIN_VALUE;
    protected void sendRandomMeasurement()
    {
        var now = System.currentTimeMillis();
        if (nextRecordTime > now)
            return;
        
        var dataBlk = dataStruct.createDataBlock();
        
        int i = 0;        
        dataBlk.setDoubleValue(i++, ((double)now)/1000.);
        dataBlk.setDoubleValue(i++, 101325 + (int)(Math.random()*1000.-500.)); // value
        dataBlk.setDoubleValue(i++, 10); // error
        
        nextRecordTime = now + (long)(getAverageSamplingPeriod()*1000);
        latestRecordTime = now;
        latestRecord = dataBlk;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlk));
    }
}
