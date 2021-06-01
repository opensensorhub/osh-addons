package org.sensorhub.impl.sensor.isa;

import org.sensorhub.api.data.DataEvent;
import org.vast.swe.SWEConstants;


public class ElecStatusOutput extends ISAOutput
{
    
    public ElecStatusOutput(ISASensor parentSensor, boolean isDC)
    {
        super("elec", parentSensor);
        ISAHelper isa = new ISAHelper();
        var currentType = isDC ? "DC" : "AC";
        
        // output structure
        dataStruct = isa.createRecord()
            .name(this.name)
            .definition(ISAHelper.ISA_DEF_URI_BASE + "Electrical_Status")
            .label("Electrical Status")
            .addSamplingTimeIsoUTC("time")
            .addField("voltage", isa.createQuantity()
                .definition(ISAHelper.ISA_DEF_URI_BASE + currentType + "_Voltage_Load")
                .uomCode("V")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("current", isa.createQuantity()
                .definition(ISAHelper.ISA_DEF_URI_BASE + currentType + "_Current_Load")
                .uomCode("A")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("capacity", isa.createQuantity()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Reserve_Capacity")
                .uomCode("%")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("time_to_empty", isa.createQuantity()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Time_to_Empty")
                .uomCode("s")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
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
        
        // simulate discharging battery
        var dischargeStep = 2;
        var prevCapacity = latestRecord != null ? latestRecord.getDoubleValue(3) : 100;
        var newCapacity = prevCapacity > dischargeStep ? prevCapacity - dischargeStep : 99;
        var timeLeft = Math.round(newCapacity / dischargeStep * getAverageSamplingPeriod());
        
        int i = 0;
        dataBlk.setDoubleValue(i++, ((double)now)/1000.);
        dataBlk.setDoubleValue(i++, 12); // voltage
        dataBlk.setDoubleValue(i++, 0.25); // current
        dataBlk.setDoubleValue(i++, newCapacity); // capacity
        dataBlk.setDoubleValue(i++, timeLeft); // time_to_empty
        
        nextRecordTime = now + (long)(getAverageSamplingPeriod()*1000);
        latestRecordTime = now;
        latestRecord = dataBlk;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlk));
    }
}
