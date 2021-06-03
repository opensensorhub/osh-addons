package org.sensorhub.impl.sensor.isa;

import org.sensorhub.api.data.DataEvent;
import org.vast.swe.SWEConstants;


public class BioReadingOutput extends ISAOutput
{
    static final String[] BIO_MATERIAL_CLASSES = {
        "UNKNOWN", "BACTERIA", "BIOREGULATOR", "CYTOTOXIN", "NEUROTOXIN", "PATHOGEN",
        "PRION", "RIC", "SPORE", "TOXIN", "TOXMAT", "VIRAL", "UNSPECIFIED"};
    
    
    public BioReadingOutput(ISASensor parentSensor)
    {
        super("bio_reading", parentSensor);
        ISAHelper isa = new ISAHelper();
        
        // output structure
        dataStruct = isa.createRecord()
            .name(this.name)
            .definition(ISAHelper.getIsaUri("Biological_Reading"))
            .label("Biological Reading")
            .addSamplingTimeIsoUTC("time")
            .addField("material_class", isa.createCategory()
                .definition(ISAHelper.getIsaUri("Biological_Material_Class"))
                .label("Material Class")
                .description("Type of the biological material detected. If not provided, the default value is 'UNKNOWN'.")
                .addAllowedValues(BIO_MATERIAL_CLASSES)
                .addNilValue(BIO_MATERIAL_CLASSES[0], SWEConstants.NIL_UNKNOWN))
            .addField("channel", isa.createCount()
                .definition(ISAHelper.getIsaUri("DetectorChannel"))
                .label("Channel")
                .description("For multi-channel biological detectors, contains the channel on which the report was generated.")
                .addNilValue(-1, SWEConstants.NIL_UNKNOWN))
            .addField("density", isa.createQuantity()
                .definition(ISAHelper.getQudtUri("MassFraction"))
                .description("Particle density of agent containing particles within the sample.")
                .uomCode("[ppm]")
                .addQuality(isa.capabilities.resolution(1, "[ppm]"))
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("harmful", isa.createBoolean()
                .definition(ISAHelper.getIsaUri("Harmful"))
                .description("Whether or not this event has the potential to cause harm."))
            .addField("bottle_id", isa.createCount()
                .definition(ISAHelper.getPropertyUri("SampleID"))
                .label("Bottle ID")
                .description("ID of the sample bottle used to generate this report.")
                .addNilValue(-1, SWEConstants.NIL_UNKNOWN))
            .addField("duration", isa.createQuantity()
                .definition(ISAHelper.getPropertyUri("IntegrationTime"))
                .description("Integration time of the channel during which it was collecting data.")
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
        
        int i =0;        
        dataBlk.setDoubleValue(i++, ((double)now)/1000.);
        dataBlk.setStringValue(i++, BIO_MATERIAL_CLASSES[(int)(Math.random()*BIO_MATERIAL_CLASSES.length)]); // material class
        dataBlk.setIntValue(i++, 1); // channel
        dataBlk.setDoubleValue(i++, (int)(Math.random()*1000)); // density ppm
        dataBlk.setBooleanValue(i++, Math.random() > 0.5); // harmful
        dataBlk.setIntValue(i++, (int)(Math.random()*10000)); // bottle ID
        dataBlk.setDoubleValue(i++, (int)(Math.random()*10)+1); // integration time
        
        nextRecordTime = now + (long)(Math.random()*getAverageSamplingPeriod()*1000);
        latestRecordTime = now;
        latestRecord = dataBlk;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlk));
    }
}
