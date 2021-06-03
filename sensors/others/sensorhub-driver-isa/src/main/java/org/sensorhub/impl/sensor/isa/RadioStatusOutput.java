package org.sensorhub.impl.sensor.isa;

import org.sensorhub.api.data.DataEvent;
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
            .definition(ISAHelper.getIsaUri("Radio_Status"))
            .label("Wireless Link Status")
            .addSamplingTimeIsoUTC("time")
            .addField("link_loss", isa.createQuantity()
                .definition(ISAHelper.getIsaUri("Link_Loss"))
                .label("Link Loss")
                .uomCode("dB")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("link_state", isa.createBoolean()
                .definition(ISAHelper.getIsaUri("Link_State")))
                .label("Link State")
                .description("True if network link is enabled")
            .addField("range", isa.createQuantity()
                .definition(ISAHelper.getIsaUri("Range"))
                .label("Transmission Range")
                .uomCode("m")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("receive_power", isa.createQuantity()
                .definition(ISAHelper.getIsaUri("Receive_Power"))
                .uomCode("dB[mV]")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("signal_strength", isa.createQuantity()
                .definition(ISAHelper.getIsaUri("Signal_Strength_Ratio"))
                .uomCode("dB")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("transmit_power", isa.createQuantity()
                .definition(ISAHelper.getIsaUri("Transmit_Power"))
                .uomCode("dB[mV]")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .build();
                
        // default output encoding
        dataEnc = isa.newTextEncoding(",", "\n");
    }
    
    
    @Override
    public double getAverageSamplingPeriod()
    {
        return 20.;
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
        dataBlk.setDoubleValue(i++, Double.NaN); // link loss
        dataBlk.setBooleanValue(i++, Math.random() > 0.1); // link state
        dataBlk.setDoubleValue(i++, 2600); // range
        dataBlk.setDoubleValue(i++, -30 + Math.random()*10); // receive power
        dataBlk.setDoubleValue(i++, Double.NaN); // signal strength
        dataBlk.setDoubleValue(i++, 22); // transmit power
        
        nextRecordTime = now + (long)(getAverageSamplingPeriod()*1000);
        latestRecordTime = now;
        latestRecord = dataBlk;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlk));
    }
}
