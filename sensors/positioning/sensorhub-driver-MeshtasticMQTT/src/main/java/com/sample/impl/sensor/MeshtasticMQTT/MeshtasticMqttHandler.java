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
import java.time.Instant;

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

    public MeshtasticMqttHandler(
            MeshtasticOutputTextMessage output_text,
            MeshtasticOutputPosition output_position
    ){
        this.output_text = output_text;
        this.output_position = output_position;
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

                // GET ALL PACKET INFO AND CONVERT TO APPROPRIATE DATA TYPE:
                String packet_to = convert_32int_to_string(packet.getTo());
                String packet_from = convert_32int_to_string(packet.getFrom());
                String packet_ID = convert_32int_to_string(packet.getId());
                Instant packet_time = convert_32int_to_Instant(packet.getRxTime());

                int packet_RxRssi = packet.getRxRssi();
                int packet_HopLimit = packet.getHopLimit();
                String packet_RelayNode = convert_32int_to_string(packet.getRelayNode());
                int packet_hopStart = packet.getHopStart();

                // DETERMINE PACKET MESSAGE TYPE BASED ON PORTNUM PROVIDED
                if (packet.hasDecoded()){
                    Data data = packet.getDecoded();
                    int portVal = data.getPortnumValue();
                    switch (portVal){
                        case 1: // 1 = TEXT MESSAGE
                            String msg = data.getPayload().toStringUtf8();
                            output_text.setData(packet_from, msg);
                            break;
                        case 3: // 3 = POSITION
                            System.out.println("This is position");
                            MeshProtos.Position pos = MeshProtos.Position.parseFrom(data.getPayload());
                            double lat = pos.getLatitudeI()/ 1e7;
                            double lon = pos.getLongitudeI()/ 1e7;
                            double alt = pos.getAltitude();

                            output_position.setData(channelId, gatewayId, packet_ID, packet_to, packet_from, packet_time, lat, lon, alt);
                            break;
                        case 4: // 4 = NODEINFO_APP_VALUE
                            MeshProtos.User node_info = MeshProtos.User.parseFrom(data.getPayload());
                            System.out.println("\\e[1m--------------Node Info--------------------\\e[0m");
                            System.out.println("\t[TYPE]: " + data.getPortnum());
                            System.out.println("\t[NOde INFO]: " + node_info);
                        case 5: // 5 = ROUTING (THIS IS USED TO DETERMINE IF ERRORS WERE DISCOVERED IN MESSAGING
//                            MeshProtos.Routing route = MeshProtos.Routing.parseFrom(data.getPayload());
//                            if(route.hasErrorReason()){
//                                System.out.println("[ROUTING ERROR]: " + route);
//                            }
                            break;
                        case 67: // 67 = TELEMETRY
                            TelemetryProtos.Telemetry telemetry = TelemetryProtos.Telemetry.parseFrom(data.getPayload());
                            System.out.println("\\e[1m--------------TELEMETRY--------------------\\e[0m");
                            System.out.println("MQTT INFO:");
                            System.out.println("\t[Channel ID]: " +  mqttProto.getChannelId());
                            System.out.println("\t[Gateway ID]: " +  mqttProto.getGatewayId());
                            System.out.println("PACKET INFO:");
                            System.out.println("\t[Packet ID]: " + packet_ID);
                            System.out.println("\t[To]: " + packet_to);
                            System.out.println("\t[From]: " + packet_from);
                            System.out.println("\t[Rx Time]: " + packet_time);
                            System.out.println("PAYLOAD INFO:");
                            System.out.println("\t[TYPE]: " + data.getPortnum());
                            System.out.println("\t[telemetry data]: " + telemetry);
                            break;
                        default:
                            System.out.println("unknonwn portnum: " + portVal);
                            break;
                    }
                }
            }
        } catch (InvalidProtocolBufferException e) {
            System.err.println("ERROR parsing ServiceEnvelope: " + e.getMessage());
        }

    }

    private String convert_32int_to_string(int data){
        long unsigned_num = Integer.toUnsignedLong(data);
        return String.format("!%08x", unsigned_num);
    }
    private Instant convert_32int_to_Instant(int data){
        long unsigned_num = Integer.toUnsignedLong(data);
        return Instant.ofEpochSecond(unsigned_num);
    }
}
