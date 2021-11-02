package org.sensorhub.impl.sensor.isa;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.isa.ISASimulation.RandomWalk;
import org.vast.swe.SWEConstants;


public class AtmosWindOutput extends ISAOutput
{
    RandomWalk windSpeedValue = new RandomWalk(15, 10.0, 0.1, 0, 40);
    RandomWalk windDirectionValue = new RandomWalk(0, 180.0, 0.1, -180, 180);
    
    
    static final String[] WIND_CLASSES = {
        "UNKNOWN", "CONSTANT", "GUST", "LIGHT TURBULENCE",
        "MODERATE TURBULENCE", "SEVERE TURBULENCE", "EXTREME TURBULENCE",
        "SQUALL", "VARIABLE", "UNSPECIFIED"};
    
    
    public AtmosWindOutput(ISASensor parentSensor)
    {
        super("wind", parentSensor);
        ISAHelper isa = new ISAHelper();
        
        // output structure
        dataStruct = isa.createRecord()
            .name(this.name)
            .definition(ISAHelper.getIsaUri("Atmospheric_Wind"))
            .label("Atmospheric Wind")
            .addSamplingTimeIsoUTC("time")
            .addField("category", isa.createCategory()
                .definition(ISAHelper.getIsaUri("Wind_Category"))
                .description("The type of precipitation observed.")
                .addAllowedValues(WIND_CLASSES)
                .addNilValue(WIND_CLASSES[0], SWEConstants.NIL_UNKNOWN))
            .addField("speed", isa.createQuantity()
                .definition(ISAHelper.getCfUri("wind_speed"))
                .description("Measured prevailing speed of the wind. If not provided, the default value is 0.")
                .uomCode("m/s")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .addField("direction", isa.createQuantity()
                .description("Direction of the wind in the x,y axis using the reference frame NOLL (North Oriented, Local-Level).")
                .refFrame(SWEConstants.REF_FRAME_NED)
                .definition(ISAHelper.getCfUri("wind_from_direction"))
                .uomCode("deg")
                .addNilValue(Double.NaN, SWEConstants.NIL_UNKNOWN))
            .build();
                
        // default output encoding
        dataEnc = isa.newTextEncoding(",", "\n");
    }
    
    
    @Override
    public double getAverageSamplingPeriod()
    {
        return 15.;
    }


    @Override
    protected void sendSimulatedMeasurement()
    {
        var now = parentSensor.getCurrentTime();
        if (nextRecordTime > now)
            return;
        
        var dataBlk = dataStruct.createDataBlock();
        
        int i = 0;        
        dataBlk.setDoubleValue(i++, ((double)now)/1000.);
        dataBlk.setStringValue(i++, WIND_CLASSES[(int)(Math.random()*WIND_CLASSES.length)]); // wind class
        dataBlk.setDoubleValue(i++, windSpeedValue.next()); // speed
        dataBlk.setDoubleValue(i++, windDirectionValue.next()); // dir
        
        nextRecordTime = now + (long)(getAverageSamplingPeriod()*1000);
        latestRecordTime = now;
        latestRecord = dataBlk;
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlk));
    }
}
