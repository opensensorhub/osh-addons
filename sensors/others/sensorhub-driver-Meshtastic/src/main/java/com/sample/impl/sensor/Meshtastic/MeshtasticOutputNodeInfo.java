/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.Meshtastic;

import com.geeksville.mesh.ConfigProtos;
import com.geeksville.mesh.MeshProtos;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.DataEvent;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.util.ArrayList;
import java.util.Base64;

/**
 * Output specification and provider for {@link MeshtasticSensor}.
 */
public class MeshtasticOutputNodeInfo extends MeshtasticOutputPacketInfo {
    static final String OUTPUT_NAME = "NodeInfo";
    static final String OUTPUT_LABEL = "Meshtastic Node Information Packet";
    static final String OUTPUT_DESCRIPTION = "Output data for the Node Info";

    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
    private final Object histogramLock = new Object();
    private final Object processingLock = new Object();


    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentMeshtasticSensor Sensor driver providing this output.
     */
    MeshtasticOutputNodeInfo(MeshtasticSensor parentMeshtasticSensor) {
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
        SWEHelper sweFactory = new SWEHelper();
        // Create the data record description
        packetRecord.addField("node_id", sweFactory.createText()
                        .label("ID")
                        .description("the id of a meshtastic node")
                        .definition(SWEHelper.getPropertyUri("node_id"))
                        .build()
        );
        packetRecord.addField("node_shortName", sweFactory.createText()
                .label("Long Name")
                .description("the long name of a meshtastic node")
                .definition(SWEHelper.getPropertyUri("node_longName"))
                .build()
        );
        packetRecord.addField("node_longName", sweFactory.createText()
                        .label("Short Name")
                        .description("the short name of a meshtastic node")
                        .definition(SWEHelper.getPropertyUri("node_shortName"))
                        .build()
        );
        packetRecord.addField("node_pk", sweFactory.createText()
                        .label("Public Key")
                        .description("the public key of a meshtastic node")
                        .definition(SWEHelper.getPropertyUri("node_pk"))
                        .build()
                );
        packetRecord.addField("node_HwModel", sweFactory.createText()
                        .label("Hardware Model")
                        .description("the Hardware Model of a meshtastic node")
                        .definition(SWEHelper.getPropertyUri("node_HwModel"))
                        .build()
                );
        packetRecord.addField("node_role", sweFactory.createText()
                        .label("Role")
                        .description("the Role of a meshtastic node")
                        .definition(SWEHelper.getPropertyUri("node_role"))
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
    public void setData(MeshProtos.MeshPacket packet_data, MeshProtos.User node_info_data) {
        synchronized (processingLock) {
            //SET PACKET INFO IN PARENT CLASS
            setPacketData(packet_data);

            String node_id = node_info_data.getId();
            String node_shortName = node_info_data.getShortName();
            String node_LongName = node_info_data.getLongName();
            String node_PK = Base64.getEncoder().encodeToString(node_info_data.getPublicKey().toByteArray());
            String node_hwModel =  node_info_data.getHwModel().toString();
            String node_role = (node_info_data.getRole() == ConfigProtos.Config.DeviceConfig.Role.UNRECOGNIZED) ? "Unknown Role" : node_info_data.getRole().name();
//            boolean isUnmessageable =  node_info.getIsUnmessagable();
//            boolean can_be_messaged = node_info.hasIsUnmessagable();

            DataBlock dataBlock = latestRecord == null ? packetRecord.createDataBlock() : latestRecord.renew();

            // Populate Parent Class Packet Data
            populatePacketDataStructure(dataBlock);

            // Populate the Node Info Data
            dataBlock.setStringValue(packet_record_size + 1, node_id);
            dataBlock.setStringValue(packet_record_size + 2, node_shortName);
            dataBlock.setStringValue(packet_record_size + 3, node_LongName);
            dataBlock.setStringValue(packet_record_size + 4, node_PK);
            dataBlock.setStringValue(packet_record_size + 5, node_hwModel);
            dataBlock.setStringValue(packet_record_size + 6, node_role);

            updateIntervalHistogram();

            // CREATE FOI UID
            String foiUID = parentSensor.addFoi(packet_from);

            // Publish the data block
            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publish(new DataEvent(latestRecordTime, MeshtasticOutputNodeInfo.this, foiUID, dataBlock));
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
