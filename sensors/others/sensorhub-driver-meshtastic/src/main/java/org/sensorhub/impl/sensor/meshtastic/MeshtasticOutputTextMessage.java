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
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.DataEvent;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

/**
 * Output specification and provider for {@link MeshtasticSensor}.
 */
public class MeshtasticOutputTextMessage extends MeshtasticOutputPacketInfo implements MeshtasticOutputInterface{
    private static final String OUTPUT_NAME = "MeshtasticText";
    private static final String OUTPUT_LABEL = "meshtastic Text Message";
    private static final String OUTPUT_DESCRIPTION = "Text provided by a meshtastic Device";

    private final Object processingLock = new Object();

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentMeshtasticSensor Sensor driver providing this output.
     */
    MeshtasticOutputTextMessage(MeshtasticSensor parentMeshtasticSensor) {
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
        SWEHelper sweFactory = new SWEHelper();
        // Create the data record description
        packetRecord.addField("textMessage", sweFactory.createText()
                        .label("Message")
                        .description("Message from a meshtastic Device")
                        .definition(SWEHelper.getPropertyUri("TextMessage"))
                        .build()
        );

        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }


    /**
     * Sets the data for the output and publishes it.
     */
    @Override
    public void setData(MeshProtos.MeshPacket packetData, ByteString payload) {
        synchronized (processingLock) {
            // Set PacketInfo in Parent Class
            setPacketData(packetData);

            DataBlock dataBlock = latestRecord == null ? packetRecord.createDataBlock() : latestRecord.renew();

            // Populate Parent Class Packet Data
            populatePacketDataStructure(dataBlock);

            //Extract Message Payload
            String messageData = payload.toStringUtf8();

            // Populate Message Data
            dataBlock.setStringValue(packetRecordSize + 1, messageData);

            // CREATE FOI UID
            String foiUID = parentSensor.addFoi(packetFrom);

            // Publish the data block
            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, MeshtasticOutputTextMessage.this, foiUID, dataBlock));
        }

    }
}
