/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package com.sample.impl.sensor.MeshtasticMQTT;


import com.geeksville.mesh.*;
import com.google.protobuf.InvalidProtocolBufferException;
import org.sensorhub.api.comm.mqtt.IMqttServer;

import com.geeksville.mesh.MeshProtos.MeshPacket;
import com.geeksville.mesh.MeshProtos.Data;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * <p>
 * This class handles communication with the embedded MQTT server and transfers
 * messages to/from the Connected Systems API servlet for processing.
 * </p>
 *
 * @author Alex Robin
 * @since May 9, 2023
 */
public class MeshtasticMqttHandler implements IMqttServer.IMqttHandler {
    // DEFINE OUTPUTS
    MeshtasticOutputTextMessage output_text;
    MeshtasticOutputPosition output_position;
    MeshtasticOutputNodeInfo output_nodeInfo;
    MeshtasticOutputGeneric output_generic;

    public MeshtasticMqttHandler(
            MeshtasticOutputTextMessage output_text,
            MeshtasticOutputPosition output_position,
            MeshtasticOutputNodeInfo output_nodeInfo,
            MeshtasticOutputGeneric output_generic
    ){
        this.output_text = output_text;
        this.output_position = output_position;
        this.output_nodeInfo = output_nodeInfo;
        this.output_generic = output_generic;
    }


    @Override
    public void onSubscribe(String userID, String topic, IMqttServer server)
    {
        System.out.println("Client subscribed: " + userID + " Topic: " + topic);
    }


    @Override
    public void onUnsubscribe(String userID, String topic, IMqttServer server)
    {
        System.out.println("Client unsubscribed: " + userID + " Topic: " + topic);
    }


    @Override
    public void onPublish(String userID, String topic, ByteBuffer payload, ByteBuffer correlData)
    {

        // READ THE RAW BYTES FROM MESHTASTIC MQTT SERVER
        byte[] bytes = new byte[payload.remaining()];
            payload.get(bytes);

        try {
            // DECODE THE MESHTASTIC SERVICE ENVELOPE PROTOBUFS
            MQTTProtos.ServiceEnvelope mqttProto = MQTTProtos.ServiceEnvelope.parseFrom(bytes);
            String channelId = mqttProto.getChannelId();
            String gatewayId = mqttProto.getGatewayId();

            // RETRIEVE PACKET IF AVAILABLE
            if(mqttProto.hasPacket()){
                MeshPacket packet = mqttProto.getPacket();

                // DETERMINE PACKET MESSAGE TYPE BASED ON PROVIDED PORTNUM
                if (packet.hasDecoded()){
                    Data data = packet.getDecoded();
                    int portVal = data.getPortnumValue();
                    switch (portVal){
                        case 1: // 1 = TEXT MESSAGE
                            String msg = data.getPayload().toStringUtf8();

                            // Update Text Output
                            output_text.setData(packet, msg);
                            break;
                        case 3: // 3 = POSITION
                            try{
                                MeshProtos.Position pos = MeshProtos.Position.parseFrom(data.getPayload());

                                // Update Position Output
                                output_position.setData(packet, pos);

                            }catch (InvalidProtocolBufferException e) {
                                System.err.println("ERROR parsing Position: " + e.getMessage());
                            }
                            break;
                        case 4: // 4 = NODEINFO_APP_VALUE
                            try{
                                MeshProtos.User node_info = MeshProtos.User.parseFrom(data.getPayload());
                                output_nodeInfo.setData(packet, node_info);

                            }catch (InvalidProtocolBufferException e) {
                                System.err.println("ERROR parsing Node Info: " + e.getMessage());
                            }
                            break;
                        default:
                            output_generic.setData(packet);
                            break;
                    }
                }
            }
        } catch (InvalidProtocolBufferException e) {
            System.err.println("ERROR parsing ServiceEnvelope: " + e.getMessage());
        }

    }
}
