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
            .definition(ISAHelper.getIsaUri("Electrical_Status"))
            .label("Electrical Status")
            .addSamplingTimeIsoUTC("time")
            .addField("voltage", isa.createQuantity()
                .definition(ISAHelper.getIsaUri(currentType + "_Voltage_Load"))
                .label(currentType + " Voltage Load")
                .description(currentType + " voltage used by the load.")
                .uomCode("V")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("current", isa.createQuantity()
                .definition(ISAHelper.getIsaUri(currentType + "_Current_Load"))
                .label(currentType + " Current Load")
                .description(currentType + " current consumed by the load.")
                .uomCode("A")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("capacity", isa.createQuantity()
                .definition(ISAHelper.getIsaUri("Reserve_Capacity"))
                .label("Reserve Capacity")
                .description("Current battery level as a ratio of maximum capacity.")
                .uomCode("%")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("time_to_empty", isa.createQuantity()
                .definition(ISAHelper.getIsaUri("Time_to_Empty"))
                .label("Time to empty")
                .description("Battery remaining time based on expected usage.")
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


    @Override
    protected void sendSimulatedMeasurement()
    {
        var now = parentSensor.getCurrentTime();
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
