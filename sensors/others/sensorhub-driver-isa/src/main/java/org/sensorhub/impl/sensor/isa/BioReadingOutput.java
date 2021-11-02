package org.sensorhub.impl.sensor.isa;

import java.time.Instant;
import java.util.TreeMap;
import org.sensorhub.api.data.DataEvent;
import org.vast.swe.SWEConstants;


public class BioReadingOutput extends ISAOutput
{
    static final String[] BIO_MATERIAL_CLASSES = {
        "UNKNOWN", "BACTERIA", "BIOREGULATOR", "CYTOTOXIN", "NEUROTOXIN", "PATHOGEN",
        "PRION", "RIC", "SPORE", "TOXIN", "TOXMAT", "VIRAL", "UNSPECIFIED"};
    
    
    static class ScheduledMeasurement
    {
        String materialClass;
        double density;
    }
    
    TreeMap<Long, ScheduledMeasurement> scheduledMeasurements = new TreeMap<>();
    
    
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
                .label("Channel Number")
                .description("For multi-channel biological detectors, contains the channel on which the report was generated.")
                .addNilValue(-1, SWEConstants.NIL_UNKNOWN))
            .addField("density", isa.createQuantity()
                .definition(ISAHelper.getQudtUri("MassFraction"))
                .label("Particle Density")
                .description("Particle density of agent containing particles within the sample.")
                .uomCode("[ppm]")
                .addQuality(isa.capabilities.resolution(1, "[ppm]"))
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("harmful", isa.createBoolean()
                .definition(ISAHelper.getIsaUri("Harmful"))
                .label("Harmful")
                .description("Whether or not this event has the potential to cause harm."))
            .addField("bottle_id", isa.createCount()
                .definition(ISAHelper.getPropertyUri("SampleID"))
                .label("Bottle ID")
                .description("ID of the sample bottle used to generate this report.")
                .addNilValue(-1, SWEConstants.NIL_UNKNOWN))
            .addField("duration", isa.createQuantity()
                .definition(ISAHelper.getPropertyUri("IntegrationTime"))
                .label("Integration Time")
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


    @Override
    protected void sendSimulatedMeasurement()
    {
        var now = parentSensor.getCurrentTime();
        
        if (!scheduledMeasurements.isEmpty() && scheduledMeasurements.firstKey() > now)
            nextRecordTime = scheduledMeasurements.firstKey();
        
        if (nextRecordTime > now)
            return;
        
        var dataBlk = dataStruct.createDataBlock();
        
        int i = 0;
        
        // send scheduled measurement if configured
        if (!scheduledMeasurements.isEmpty())
        {
            var meas = scheduledMeasurements.get(nextRecordTime);
            
            dataBlk.setDoubleValue(i++, ((double)nextRecordTime)/1000.);
            dataBlk.setStringValue(i++, meas.materialClass); // material class
            dataBlk.setIntValue(i++, 1); // channel
            dataBlk.setDoubleValue(i++, meas.density); // density ppm
            dataBlk.setBooleanValue(i++, true); // harmful
            dataBlk.setIntValue(i++, 1); // bottle ID
            dataBlk.setDoubleValue(i++, 0.8); // integration time
            
            // prepare for next measurement
            var nextKey = scheduledMeasurements.higherKey(nextRecordTime);
            nextRecordTime = nextKey != null ? nextKey : Long.MAX_VALUE; 
        }
        
        // else default to random measurement
        else
        {
            dataBlk.setDoubleValue(i++, ((double)now)/1000.);
            dataBlk.setStringValue(i++, BIO_MATERIAL_CLASSES[(int)(Math.random()*BIO_MATERIAL_CLASSES.length)]); // material class
            dataBlk.setIntValue(i++, 1); // channel
            dataBlk.setDoubleValue(i++, (int)(Math.random()*1000)); // density ppm
            dataBlk.setBooleanValue(i++, Math.random() > 0.5); // harmful
            dataBlk.setIntValue(i++, (int)(Math.random()*10000)); // bottle ID
            dataBlk.setDoubleValue(i++, (int)(Math.random()*10)+1); // integration time
            nextRecordTime = now + (long)(Math.random()*getAverageSamplingPeriod()*1000);
        }
        
        latestRecordTime = now;
        latestRecord = dataBlk;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlk));
    }
    
    
    protected void addScheduledMeasurement(Instant time, String materialClass, double density)
    {
        var meas = new ScheduledMeasurement();
        meas.materialClass = materialClass;
        meas.density = density;
        scheduledMeasurements.put(time.toEpochMilli(), meas);
        nextRecordTime = scheduledMeasurements.firstKey();
    }
}
