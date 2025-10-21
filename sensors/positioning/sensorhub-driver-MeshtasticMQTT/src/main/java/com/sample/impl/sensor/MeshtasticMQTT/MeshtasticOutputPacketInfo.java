/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.MeshtasticMQTT;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Output specification and provider for {@link MeshtasticSensor}.
 */
public class MeshtasticOutputPacketInfo extends AbstractSensorOutput<MeshtasticSensor> {
    static final String SENSOR_OUTPUT_NAME = "MeshtasticPacket";
    static final String SENSOR_OUTPUT_LABEL = "Meshtastic Packet Info";
    static final String SENSOR_OUTPUT_DESCRIPTION = "Packet info from Meshtastic";

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
    MeshtasticOutputPacketInfo(MeshtasticSensor parentMeshtasticSensor) {
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
                .addField("packet_id", sweFactory.createText()
                        .label("ID")
                        .description("the id of a meshtastic packet")
                        .definition(SWEHelper.getPropertyUri("packet_id")))
                .addField("packet_to", sweFactory.createText()
                        .label("To")
                        .description("node meshtastic packet is being sent to")
                        .definition(SWEHelper.getPropertyUri("packet_to")))
                .addField("packet_from", sweFactory.createText()
                        .label("From")
                        .description("node meshtastic packet is being sent from")
                        .definition(SWEHelper.getPropertyUri("packet_from")))
                .addField("packet_time", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Packet Rx Time")
                        .description("the time the packet was sent from the meshtastic node")
                        .definition(SWEHelper.getPropertyUri("packet_time")))
                .addField("packet_hasData", sweFactory.createBoolean()
                        .label("Does Packet have data")
                        .description("Does the packet provided actually contain data")
                        .definition(SWEHelper.getPropertyUri("packet_hasData")))
                .addField("packet_type", sweFactory.createText()
                        .label("Type")
                        .description("What is the PortNum of the packet")
                        .definition(SWEHelper.getPropertyUri("packet_type")))
                .addField("packet_RxRssi", sweFactory.createQuantity()
                        .label("Rx RSSI")
                        .description("Received Signal Strength Indicator")
                        .definition(SWEHelper.getPropertyUri("packet_RxRssi")))
                .addField("packet_HopLimit", sweFactory.createQuantity()
                        .label("Hop Limit")
                        .description("Hop limit of a meshtastic packet")
                        .definition(SWEHelper.getPropertyUri("packet_HopLimit")))
                .addField("packet_hopStart", sweFactory.createQuantity()
                        .label("Hop Start")
                        .description("Hop Start of a meshtastic packet")
                        .definition(SWEHelper.getPropertyUri("packet_hopStart")))
                .addField("packet_RelayNode", sweFactory.createText()
                        .label("Relay Node")
                        .description("What is the RelayNode of the packet")
                        .definition(SWEHelper.getPropertyUri("packet_RelayNode")))
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
    public void setData(String packet_id, String packet_to, String packet_from, Instant packet_time, boolean packet_hasData, String packet_type, int packet_RxRssi, int packet_HopLimit, int packet_hopStart, String packet_RelayNode) {

        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? dataRecord.createDataBlock() : latestRecord.renew();

            updateIntervalHistogram();


            // CREATE FOI UID
            String foiUID = parentSensor.addFoi(packet_from);



            // Populate the data block
            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setStringValue(1, packet_id);
            dataBlock.setStringValue(2, packet_to);
            dataBlock.setStringValue(3, packet_from);
            dataBlock.setTimeStamp(4, packet_time);
            dataBlock.setBooleanValue(5, packet_hasData);
            dataBlock.setStringValue(6, packet_type);
            dataBlock.setDoubleValue(7, packet_RxRssi);
            dataBlock.setDoubleValue(8, packet_HopLimit);
            dataBlock.setDoubleValue(9, packet_hopStart);
            dataBlock.setStringValue(10, packet_RelayNode);

            // Publish the data block
            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publish(new DataEvent(latestRecordTime, MeshtasticOutputPacketInfo.this, foiUID, dataBlock));
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
