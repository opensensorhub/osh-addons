package org.sensorhub.impl.sensor.isa;

import org.sensorhub.api.data.DataEvent;
import org.vast.sensorML.sampling.SamplingSphere;
import org.vast.swe.SWEConstants;
import org.locationtech.jts.geom.Point;
import net.opengis.swe.v20.DataType;


public class RadioReadingOutput extends ISAOutput
{
    static final String[] RADIO_MATERIAL_RADIATION_CODE = {
        "UNKNOWN", "ALPHA", "BETA", "GAMMA", "NEUTRON"};
    
    static final String[] RADIO_MATERIAL_CATEGORY_CODE = {
        "UNKNOWN", "FRESH REACTOR FUEL", "MIXTURE", "NUCLEAR POWER PLANT", "NUCLEAR ROTA", "NUCLEAR WEAPON FALLOUT",
        "SPENT REACTOR FUEL", "UNSPECIFIED"};
    
    
    // current values
    volatile String radiationCode = RADIO_MATERIAL_RADIATION_CODE[0];
    volatile String materialClass = RADIO_MATERIAL_CATEGORY_CODE[0];
    volatile int atomicNumber = 94;
    volatile int massNumber = 244;
    volatile float activity = 0;
    volatile boolean triggered = false;
    
    
    public RadioReadingOutput(ISASensor parentSensor)
    {
        super("radio_reading", parentSensor);
        ISAHelper isa = new ISAHelper();
        
        // output structure
        dataStruct = isa.createRecord()
            .name(this.name)
            .definition(ISAHelper.getIsaUri("Radiological_Reading"))
            .label("Radiological Reading")
            .addSamplingTimeIsoUTC("time")
            .addField("primary_code", isa.createCategory()
                .definition(ISAHelper.getIsaUri("Radioactive_Material_Primary_Radiation_Code"))
                .label("Radioactive Material Primary Radiation Code")
                .description("Most intense radiation type detected.")
                .addAllowedValues(RADIO_MATERIAL_RADIATION_CODE)
                .addNilValue(RADIO_MATERIAL_RADIATION_CODE[0], SWEConstants.NIL_UNKNOWN))
            .addField("material_category", isa.createCategory()
                .definition(ISAHelper.getIsaUri("Radioactive_Material_Category_Code"))
                .label("Radioactive Material Category Code")
                .description("Class of radioactive material.")
                .addAllowedValues(RADIO_MATERIAL_CATEGORY_CODE)
                .addNilValue(RADIO_MATERIAL_CATEGORY_CODE[0], SWEConstants.NIL_UNKNOWN))
            .addField("isotope_atomic_number", isa.createCount()
                .definition(ISAHelper.getIsaUri("Isotope_Atomic_Number"))
                .label("Isotope Atomic Number")
                .description("The number of protons in the atomic nucleus of the radionuclide identified.")
                .addNilValue(0, SWEConstants.NIL_UNKNOWN))
            .addField("isotope_mass_number", isa.createCount()
                .definition(ISAHelper.getIsaUri("Isotope_Mass_Number"))
                .label("Isotope Mass Number")
                .description("The number of nucleons (protons and neutrons) in the atomic nucleus of the radionuclide identified.")
                .addNilValue(0, SWEConstants.NIL_UNKNOWN))
            .addField("activity", isa.createQuantity()
                .definition(ISAHelper.getQudtUri("Activity"))
                .label("Activity")
                .description("Amount of radioactive material detected.")
                .uomCode("Bq")
                .dataType(DataType.FLOAT)
                .addQuality(isa.capabilities.resolution(1, "Bq"))
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("deposition_concentration", isa.createQuantity()
                .definition(ISAHelper.getQudtUri("Concentration"))
                .label("Deposition Concentration")
                .description("Amount of radioactive material deposited per unit area.")
                .uomCode("Bq/m2")
                .dataType(DataType.FLOAT)
                .addQuality(isa.capabilities.resolution(0.01, "Bq/m2"))
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("concentration", isa.createQuantity()
                .definition(ISAHelper.getQudtUri("Concentration"))
                .label("Concentration")
                .description("Amount of radioactive material per unit volume.")
                .uomCode("Bq/m3")
                .addQuality(isa.capabilities.resolution(0.01, "Bq/m3"))
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("dose", isa.createQuantity()
                .definition(ISAHelper.getQudtUri("AbsorbedDose"))
                .label("Dose")
                .description("Amount of radiation received by personnel.")
                .uomCode("Gy")
                .dataType(DataType.FLOAT)
                .addQuality(isa.capabilities.resolution(0.01, "Bq/m3"))
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("dose_rate", isa.createQuantity()
                .definition(ISAHelper.getQudtUri("AbsorbedDoseRate"))
                .label("Dose Rate")
                .description("Rate at which radiation dose is changing.")
                .uomCode("Gy/s")
                .dataType(DataType.FLOAT)
                .addQuality(isa.capabilities.resolution(0.01, "Bq/m3"))
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("harmful", isa.createBoolean()
                .definition(ISAHelper.getIsaUri("Harmful"))
                .label("Harmful")
                .description("Whether or not this event has the potential to cause harm."))
            .build();
                
        // default output encoding
        dataEnc = isa.newTextEncoding(",", "\n");
    }
    
    
    @Override
    public double getAverageSamplingPeriod()
    {
        return 2.0;
    }


    @Override
    protected void sendSimulatedMeasurement()
    {
        var now = parentSensor.getCurrentTime();
        if (nextRecordTime > now)
            return;
        
        nextRecordTime = now + (long)(getAverageSamplingPeriod()*1000);
        if (!triggered)
            return;
        triggered = false;
        
        int i = 0;
        var dataBlk = dataStruct.createDataBlock();
        dataBlk.setDoubleValue(i++, ((double)now)/1000.);
        dataBlk.setStringValue(i++, radiationCode); // primary_code
        dataBlk.setStringValue(i++, materialClass); // material_category
        dataBlk.setIntValue(i++, atomicNumber); // isotope_atomic_number
        dataBlk.setIntValue(i++, massNumber); // isotope_mass_number
        dataBlk.setFloatValue(i++, activity); // activity
        dataBlk.setFloatValue(i++, Float.NaN); // deposition_concentration
        dataBlk.setFloatValue(i++, Float.NaN); // concentration
        dataBlk.setFloatValue(i++, Float.NaN); // dose
        dataBlk.setFloatValue(i++, Float.NaN); // dose_rate
        dataBlk.setBooleanValue(i++, true); // harmful
        
        latestRecordTime = now;
        latestRecord = dataBlk;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlk));
    }
    
    
    void addTriggerSource(String systemUID, String outputName, String srcMaterialClass, double srcActivity)
    {
        getParentProducer().getSimulation().addTriggerSource(systemUID, outputName, t -> {
            
            var foi = getParentProducer().getFeatureOfInterest();
            var geom = (Point)foi.getGeometry();
            var range = foi instanceof SamplingSphere ? ((SamplingSphere)foi).getRadius() : 10;
            var dist = distance(t.pos, geom);
            //getLogger().debug("{}: t={}, p1={}, p2={}, dist={}m", getParentProducer().getName(), t.time, t.pos, geom, dist);
            
            if (dist < range) {
                radiationCode = RADIO_MATERIAL_RADIATION_CODE[4];
                materialClass = srcMaterialClass;
                activity = (float)(srcActivity / (0.1*dist*dist));
                triggered = true;
                getLogger().debug("{}: triggered at t={}, pos={}", getParentProducer().getName(), t.time, t.pos);
            }
        });
    }
    
    
    /* compute distance using equirectangular approximation */
    double distance(Point p1, Point p2)
    {
        double lat1r = Math.toRadians(p1.getX());
        double lon1r = Math.toRadians(p1.getY());
        double lat2r = Math.toRadians(p2.getX());
        double lon2r = Math.toRadians(p2.getY());
        
        double dLat = lat1r - lat2r;
        double dLon = lon1r - lon2r;
        
        double x = dLon * Math.cos((lat1r + lat2r) / 2.);
        double y = dLat;
        
        return Math.sqrt(x*x + y*y) * 6371000;
    }
}
