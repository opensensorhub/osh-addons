/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.meshtastic;

import org.meshtastic.proto.MeshProtos;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.DataEvent;
import org.vast.swe.helper.GeoPosHelper;

/**
 * Output specification and provider for {@link MeshtasticSensor}.
 */
public class MeshtasticOutputPosition extends MeshtasticOutputPacketInfo implements MeshtasticOutputInterface{
    private static final String OUTPUT_NAME = "MeshtasticPosition";
    private static  final String OUTPUT_LABEL = "meshtastic Position Packet";
    private static  final String OUTPUT_DESCRIPTION = "Output data for a meshtastic Device's position";

    private final Object processingLock = new Object();

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentMeshtasticSensor Sensor driver providing this output.
     */
    MeshtasticOutputPosition(MeshtasticSensor parentMeshtasticSensor) {
        super(OUTPUT_NAME, parentMeshtasticSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    void doInit() {
        // INITIALIZE THE PACKET PARENT CLASS
        super.doInit(OUTPUT_NAME, OUTPUT_LABEL, OUTPUT_DESCRIPTION);

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper geoFac = new GeoPosHelper();

        packetRecord.addField("location", geoFac.createLocationVectorLLA()
                .label("Location")
                .build()
        );

        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    /**
     * Sets the data for the output and publishes it.
     */
//    public void setData(String packet_id, String packet_from, double lat, double lon, double alt) {
    @Override
    public void setData(MeshProtos.MeshPacket packetData, ByteString payload) {
        synchronized (processingLock) {
            // Set PacketInfo in Parent Class
            setPacketData(packetData);

            try {
                // Parse ByteString to Position Protobuf
                MeshProtos.Position posData = MeshProtos.Position.parseFrom(payload);

                // Extract Position Values
                double lat = posData.getLatitudeI()/ 1e7;
                double lon = posData.getLongitudeI()/ 1e7;
                double alt = posData.getAltitude();

                DataBlock dataBlock = latestRecord == null ? packetRecord.createDataBlock() : latestRecord.renew();

                // POPULATE PACKET FIELDS
                populatePacketDataStructure(dataBlock);

                // POPULATE POSITION FIELDS
                dataBlock.setDoubleValue(packetRecordSize + 1, lat);
                dataBlock.setDoubleValue(packetRecordSize + 2, lon);
                dataBlock.setDoubleValue(packetRecordSize + 3, alt);

                // CREATE FOI UID
                String foiUID = parentSensor.addFoi(packetFrom);
                // Publish the data block
                latestRecord = dataBlock;
                latestRecordTime = System.currentTimeMillis();
                updateSamplingPeriod(latestRecordTime);
                eventHandler.publish(new DataEvent(latestRecordTime, MeshtasticOutputPosition.this, foiUID, dataBlock));


            } catch (InvalidProtocolBufferException e) {
                parentSensor.getLogger().error("Failed to parse Position payload: {}", e.getMessage());
            }

        }
    }

}
