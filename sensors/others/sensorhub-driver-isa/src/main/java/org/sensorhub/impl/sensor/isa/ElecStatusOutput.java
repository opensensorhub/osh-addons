package org.sensorhub.impl.sensor.isa;

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
    
    
    protected void sendRandomMeasurement()
    {
        
    }
}
