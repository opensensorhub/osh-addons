package org.sensorhub.impl.sensor.isa;

import org.sensorhub.api.data.DataEvent;

public class AtmosHumidityOutput extends ISAOutput
{       
    
    public AtmosHumidityOutput(ISASensor parentSensor)
    {
        super("humid", parentSensor);
        ISAHelper isa = new ISAHelper();
                
        // output structure
        dataStruct = isa.createRecord()
            .name(this.name)
            .definition(ISAHelper.ISA_DEF_URI_BASE + "Atmospheric_Humidity")
            .label("Atmospheric Humidity")
            .addSamplingTimeIsoUTC("time")
            .addField("value", isa.createQuantity()
                .definition(ISAHelper.MMI_CF_DEF_URI_BASE + "relative_humidity")
                .description("Amount of moisture in the atmosphere expressed in percentage of humidity.")
                .uomCode("%"))
            .addField("error", isa.errorField("%"))
            .build();
                
        // default output encoding
        dataEnc = isa.newTextEncoding(",", "\n");
    }
    
    
    @Override
    public double getAverageSamplingPeriod()
    {
        return 15.;
    }


    protected long nextRecordTime = Long.MIN_VALUE;
    protected void sendRandomMeasurement()
    {
        var now = System.currentTimeMillis();
        if (nextRecordTime > now)
            return;
        
        var dataBlk = dataStruct.createDataBlock();
        
        int i =0;        
        dataBlk.setDoubleValue(i++, ((double)now)/1000.);
        dataBlk.setDoubleValue(i++, (int)(Math.random()*100)); // value
        dataBlk.setDoubleValue(i++, 1); // error
        
        nextRecordTime = now + (long)(getAverageSamplingPeriod()*1000);
        latestRecordTime = now;
        latestRecord = dataBlk;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlk));
    }
}
