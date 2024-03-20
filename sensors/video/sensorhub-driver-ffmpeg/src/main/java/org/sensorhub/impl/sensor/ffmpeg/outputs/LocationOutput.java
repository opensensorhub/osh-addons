package org.sensorhub.impl.sensor.ffmpeg.outputs;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.helper.GeoPosHelper;


public class Location<FFMPEGConfigType extends FFMPEGConfig>   extends AbstractSensorOutput<FFMPEGSensorBase<FFMPEGConfigType>> {
    private static final String SENSOR_OUTPUT_NAME = "Location";

    private static final Logger logger = LoggerFactory.getLogger(Location.class);

    protected DataRecord dataStruct;
    protected DataEncoding dataEncoding;
    protected DataBlock dataBlock;

    public Location(FFMPEGSensorBase<FFMPEGConfigType> parentSensor){
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    protected void init() {
        GeoPosHelper geoPosHelper = new GeoPosHelper();

        dataStruct = geoPosHelper.createRecord()
                .name(getName())
                .label("Location")
                .definition(GeoPosHelper.getPropertyUri("location-output"))
                .addField("Sampling Time", geoPosHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .name("time")
                        .description("time stamp: when the message was received"))
                .addField("Sensor Location", geoPosHelper.createLocationVectorLLA())
                .build();

        dataEncoding = new TextEncodingImpl(",", "\n");

    }

    public void setLocation(LLALocation gpsLocation) {

        if (latestRecord == null) {

            dataBlock = dataStruct.createDataBlock();

        } else {

            dataBlock = latestRecord.renew();
        }


        latestRecordTime = System.currentTimeMillis() / 1000;

        dataBlock.setLongValue(0, latestRecordTime);
        dataBlock.setDoubleValue(1, gpsLocation.lat);
        dataBlock.setDoubleValue(2, gpsLocation.lon);
        dataBlock.setDoubleValue(3, gpsLocation.alt);

        eventHandler.publish(new DataEvent(latestRecordTime, Location.this, dataBlock));
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        return 0;
    }
}
