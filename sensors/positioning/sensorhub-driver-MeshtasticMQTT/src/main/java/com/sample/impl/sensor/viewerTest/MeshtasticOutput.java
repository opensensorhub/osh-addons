/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.viewerTest;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.ogc.gml.IFeature;
import org.vast.ogc.om.MovingFeature;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Random;

/**
 * Output specification and provider for {@link MeshtasticSensor}.
 */
public class MeshtasticOutput extends AbstractSensorOutput<MeshtasticSensor> {
    static final String SENSOR_OUTPUT_NAME = "SensorOutput";
    static final String SENSOR_OUTPUT_LABEL = "Sensor Output";
    static final String SENSOR_OUTPUT_DESCRIPTION = "Sensor output data";

    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
    private final Object histogramLock = new Object();
    private final Object processingLock = new Object();

    private DataRecord dataRecord;
    private DataEncoding dataEncoding;

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentMeshtasticSensor Sensor driver providing this output.
     */
    MeshtasticOutput(MeshtasticSensor parentMeshtasticSensor) {
        super(SENSOR_OUTPUT_NAME, parentMeshtasticSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    void doInit() {
        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper geoFac = new GeoPosHelper();
        SWEHelper sweFactory = new SWEHelper();
        // Create the data record description
        dataRecord = geoFac.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", geoFac.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("mqtt_topic", sweFactory.createText()
                        .label("MQTT Topic")
                        .description("the topic sent my meshtastic")
                        .definition(SWEHelper.getPropertyUri("mqtt_topic")))
                .addField("channel_id", sweFactory.createText()
                        .label("Channel ID")
                        .description("The channel id provided by the meshtastic node")
                        .definition(SWEHelper.getPropertyUri("channel_id")))
                .addField("gateway_id", sweFactory.createText()
                        .label("Gateway ID")
                        .description("The gateway id provided by the meshtastic node")
                        .definition(SWEHelper.getPropertyUri("gateway_id")))
                .addField("packet_id", sweFactory.createText()
                        .label("Packet ID")
                        .description("the packet id provided by the individual packet sent from the meshtastic node")
                        .definition(SWEHelper.getPropertyUri("packet_id")))
                .addField("packet_from", sweFactory.createText()
                        .label("From")
                        .description("the packet id of the which node the packet sent the message")
                        .definition(SWEHelper.getPropertyUri("packet_from")))
                .addField("packet_time", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Packet Rx Time")
                        .description("the tje time the packet was sent from the meshtastic node")
                        .definition(SWEHelper.getPropertyUri("packet_time")))
                .addField("location", geoFac.createLocationVectorLLA().label(SWEHelper.getPropertyUri("location"))
                        .label("Location")
                )

                .build();

        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataRecord;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        synchronized (histogramLock) {
            double sum = 0;
            for (double sample : intervalHistogram)
                sum += sample;

            return sum / intervalHistogram.size();
        }
    }

    /**
     * Sets the data for the output and publishes it.
     */
    public void setData(String topic, String channelId, String gatewayId, String packet_ID, String packet_to, String packet_from, Instant packet_time, double lat, double lon, double alt) {

        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? dataRecord.createDataBlock() : latestRecord.renew();

            updateIntervalHistogram();


            // CREATE FOI UID
            String foiUID = parentSensor.addFoi(packet_from);



            // Populate the data block
            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setStringValue(1, topic);
            dataBlock.setStringValue(2, channelId);
            dataBlock.setStringValue(3, gatewayId);
            dataBlock.setStringValue(4, packet_ID);
            dataBlock.setStringValue(5, packet_from);
            dataBlock.setTimeStamp(6, packet_time);
            dataBlock.setDoubleValue(7, lat);
            dataBlock.setDoubleValue(8, lon);
            dataBlock.setDoubleValue(9, alt);


            // Publish the data block
            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publish(new DataEvent(latestRecordTime, MeshtasticOutput.this, foiUID, dataBlock));
        }

    }

    /**
     * Updates the interval histogram with the time between the latest record and the current time
     * for calculating the average sampling period.
     */
    private void updateIntervalHistogram() {
        synchronized (histogramLock) {
            if (latestRecord != null && latestRecordTime != Long.MIN_VALUE) {
                long interval = System.currentTimeMillis() - latestRecordTime;
                intervalHistogram.add(interval / 1000d);

                if (intervalHistogram.size() > MAX_NUM_TIMING_SAMPLES) {
                    intervalHistogram.remove(0);
                }
            }
        }
    }
}
