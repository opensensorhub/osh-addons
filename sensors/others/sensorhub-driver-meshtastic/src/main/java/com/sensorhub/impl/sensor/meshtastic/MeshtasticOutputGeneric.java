/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.sensorhub.impl.sensor.meshtastic;

import com.geeksville.mesh.MeshProtos;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.DataEvent;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.util.ArrayList;

/**
 * Output specification and provider for {@link MeshtasticSensor}.
 */
public class MeshtasticOutputGeneric extends MeshtasticOutputPacketInfo {
    private static final String OUTPUT_NAME = "MeshtasticGeneric";
    private static  final String OUTPUT_LABEL = "meshtastic Generic Packet";
    private static  final String OUTPUT_DESCRIPTION = "This output data is from a packet currently not registered in the meshtastic handler";
    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
    private final Object histogramLock = new Object();
    private final Object processingLock = new Object();

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentMeshtasticSensor Sensor driver providing this output.
     */
    MeshtasticOutputGeneric(MeshtasticSensor parentMeshtasticSensor) {
        super(parentMeshtasticSensor, OUTPUT_NAME);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    void doInit() {
        // INITIALIZE THE PACKET PARENT CLASS
        super.doInit(OUTPUT_NAME, OUTPUT_LABEL, OUTPUT_DESCRIPTION);
        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper geoFac = new GeoPosHelper();

        packetRecord.addField("hasDecoded", geoFac.createBoolean()
                .label("hasDecoded")
                .description("Does the packet have data associated with it")
                .definition(SWEHelper.getPropertyUri("HasDecoded"))
                .build()
        );
        packetRecord.addField("hasEncrypted", geoFac.createBoolean()
                .label("hasEncrypted")
                .description("Does the packet have encrypted data associated with it")
                .definition(SWEHelper.getPropertyUri("HasEncrypted"))
                .build()
        );

        dataEncoding = geoFac.newTextEncoding(",", "\n");
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
//    public void setData(String packet_id, String packet_from, double lat, double lon, double alt) {
    public void setData(MeshProtos.MeshPacket packet_data) {
        synchronized (processingLock) {
            // Set PacketInfo in Parent Class
            setPacketData(packet_data);

            DataBlock dataBlock = latestRecord == null ? packetRecord.createDataBlock() : latestRecord.renew();

            // Populate Parent Class Packet Data
            populatePacketDataStructure(dataBlock);

            boolean hasDecoded = packet_data.hasDecoded();
            boolean hasEncrypted = packet_data.hasEncrypted();

            // Populate position fields
            dataBlock.setBooleanValue(packetRecordSize + 1, packet_data.hasDecoded());
            dataBlock.setBooleanValue(packetRecordSize + 2, packet_data.hasEncrypted());

            updateIntervalHistogram();

            // CREATE FOI UID
            String foiUID = parentSensor.addFoi(packetFrom);
            // Publish the data block
            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publish(new DataEvent(latestRecordTime, MeshtasticOutputGeneric.this, foiUID, dataBlock));
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
