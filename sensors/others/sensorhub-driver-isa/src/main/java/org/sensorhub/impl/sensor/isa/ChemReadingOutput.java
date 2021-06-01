package org.sensorhub.impl.sensor.isa;

import org.sensorhub.api.data.DataEvent;
import org.vast.swe.SWEConstants;


public class ChemReadingOutput extends ISAOutput
{
    static final String[] CHEM_MATERIAL_CLASSES = {
        "UNKNOWN", "ARSENICAL BLISTER AGENT", "BLISTER AGENT", "BLOOD AGENT",
        "CHEMICAL AGENT TYPE", "CHOKING AGENT", "CYANOGEN AGENT", "DELIRIANT AGENT",
        "DEPRESSANT AGENT", "G AGENT", "INCAPACITATING AGENT", "MILITARY CHEMICAL COMPOUND TYPE",
        "MUSTARD AGENT", "NERVE AGENT", "PSYCHEDELIC AGENT", "STIMULANT AGENT", "TOXIC INDUSTRIAL CHEMICAL",
        "URTICANT AGENT", "V AGENT", "UNSPECIFIED"};
    
        
    public ChemReadingOutput(ISASensor parentSensor)
    {
        super("chem_reading", parentSensor);
        ISAHelper isa = new ISAHelper();
        
        // output structure
        dataStruct = isa.createRecord()
            .name(this.name)
            .definition(ISAHelper.ISA_DEF_URI_BASE + "Chemical_Reading")
            .label("Chemical Reading")
            .addSamplingTimeIsoUTC("time")
            .addField("material_class", isa.createCategory()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Chemical_Material_Class")
                .label("Material Class")
                .description("Type of the chemical material detected. If not provided, the default value is 'UNKNOWN'.")
                .addAllowedValues(CHEM_MATERIAL_CLASSES)
                .addNilValue(CHEM_MATERIAL_CLASSES[0], SWEConstants.NIL_UNKNOWN))
            .addField("material_name", isa.createText()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Material_Name")
                .label("Material Name")
                .description("Name of the chemical material detected.")
                .addNilValue("-", SWEConstants.NIL_UNKNOWN))
            .addField("service_number", isa.createText()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Service_Number")
                .label("Service Number")
                .description("Name of the Chemical Abstracts Service Number associated with the detected material.")
                .addNilValue("-", SWEConstants.NIL_UNKNOWN))
            .addField("harmful", isa.createBoolean()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "Harmful")
                .description("Whether or not the chemical can cause harm. If not provided, the default value is true."))
            .addField("density", isa.createQuantity()
                .definition(ISAHelper.getQudtUri("Concentration"))
                .description("Concentration of the detected chemical.")
                .uomCode("kg/m3")
                .addQuality(isa.capabilities.resolution(1e-3, "kg/m3"))
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("duration", isa.createQuantity()
                .definition(ISAHelper.getPropertyUri("IntegrationTime"))
                .description("Duration of the integration for the sample.")
                .uomCode("s")
                .addQuality(isa.capabilities.resolution(0.1, "s"))
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("concentration_time", isa.createQuantity()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "ConcentrationTime")
                .description("Integrated concentration of chemical.")
                .uomCode("kg.s/m3")
                .significantFigures(6)
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("deposition", isa.createQuantity()
                .definition(ISAHelper.getPropertyUri("SurfaceDensity"))
                .description("Mass of chemical deposited per unit area.")
                .uomCode("kg/m2")
                .addQuality(isa.capabilities.resolution(1e-6, "kg/m2"))
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("mass_fraction", isa.createQuantity()
                .definition(ISAHelper.getPropertyUri("MassFraction"))
                .description("Mass fraction concentration of chemical material.")
                .uomCode("[ppm]")
                .addQuality(isa.capabilities.resolution(1, "[ppm]"))
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("g-bar_reading", isa.createCount()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "GBarReading")
                .description("Concentration of G-type agent, expressed on a scale to indicate hazard [0,8].")
                .addAllowedInterval(0, 8)
                .addNilValue(-1, SWEConstants.NIL_UNKNOWN))
            .addField("h-bar_reading", isa.createCount()
                .definition(ISAHelper.ISA_DEF_URI_BASE + "HBarReading")
                .description("Concentration of H-type agent, expressed on a scale to indicate hazard [0,8].")
                .addAllowedInterval(0, 8)
                .addNilValue(-1, SWEConstants.NIL_UNKNOWN))
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
        dataBlk.setStringValue(i++, CHEM_MATERIAL_CLASSES[(int)(Math.random()*CHEM_MATERIAL_CLASSES.length)]); // material class
        dataBlk.setStringValue(i++, "Substance X"); // material name
        dataBlk.setIntValue(i++, (int)(Math.random()*100)); // service num
        dataBlk.setBooleanValue(i++, Math.random() > 0.5); // harmful
        dataBlk.setDoubleValue(i++, Math.random()*1e-2); // density
        dataBlk.setDoubleValue(i++, 1.5); // integration time
        dataBlk.setDoubleValue(i++, Double.NaN); // concentration.time
        dataBlk.setDoubleValue(i++, Math.random()*1e-4); // deposition
        dataBlk.setDoubleValue(i++, (int)(Math.random()*1000)); // mass fraction
        dataBlk.setDoubleValue(i++, Double.NaN);
        dataBlk.setDoubleValue(i++, Double.NaN);
        
        nextRecordTime = now + (long)(Math.random()*getAverageSamplingPeriod()*1000);
        latestRecordTime = now;
        latestRecord = dataBlk;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlk));
    }
}
