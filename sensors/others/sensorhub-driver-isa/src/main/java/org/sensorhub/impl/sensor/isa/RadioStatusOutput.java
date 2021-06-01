package org.sensorhub.impl.sensor.isa;

import org.vast.swe.SWEConstants;


public class RadioStatusOutput extends ISAOutput
{
    
    public RadioStatusOutput(ISASensor parentSensor)
    {
        super("radio", parentSensor);
        ISAHelper isa = new ISAHelper();
        
        // output structure
        dataStruct = isa.createRecord()
            .name(this.name)
            .definition(ISAHelper.ISA_DEF_URI_BASE + "Radio_Status")
            .label("Wireless Link Status")
            .addSamplingTimeIsoUTC("time")
            .addField("link_loss", isa.createQuantity()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Link_Loss")
                .label("Link Loss")
                .uomCode("dB")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("link_state", isa.createBoolean()
                .label("Link State")
                .description("True if network link is enabled")
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Link_State"))
            .addField("range", isa.createQuantity()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Range")
                .label("Transmission Range")
                .uomCode("m")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("receive_power", isa.createQuantity()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Receive_Power")
                .uomCode("dB[mV]")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("signal_strength", isa.createQuantity()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Signal_Strength_Ratio")
                .uomCode("dB")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("transmit_power", isa.createQuantity()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Transmit_Power")
                .uomCode("dB[mV]")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .build();
                
        // default output encoding
        dataEnc = isa.newTextEncoding(",", "\n");
    }
    
    
    protected void sendRandomMeasurement()
    {
        
    }
}
