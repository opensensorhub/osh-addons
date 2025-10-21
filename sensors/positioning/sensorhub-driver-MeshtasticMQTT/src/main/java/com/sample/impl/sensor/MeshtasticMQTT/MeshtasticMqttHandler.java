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
    MeshtasticOutputNodeInfo output_nodeInfo;

    public MeshtasticMqttHandler(
            MeshtasticOutputTextMessage output_text,
            MeshtasticOutputPosition output_position,
            MeshtasticOutputNodeInfo output_nodeInfo
    ){
        this.output_text = output_text;
        this.output_position = output_position;
        this.output_nodeInfo = output_nodeInfo;
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

                            // Update Text Output
                            output_text.setData(packet_from, msg);
                            break;
                        case 3: // 3 = POSITION
                            try{
                                MeshProtos.Position pos = MeshProtos.Position.parseFrom(data.getPayload());
                                double lat = pos.getLatitudeI()/ 1e7;
                                double lon = pos.getLongitudeI()/ 1e7;
                                double alt = pos.getAltitude();

                                // Update Position Output
                                output_position.setData(packet_from, lat, lon, alt);

                            }catch (InvalidProtocolBufferException e) {
                                System.err.println("ERROR parsing Position: " + e.getMessage());
                            }
                            break;
                        case 4: // 4 = NODEINFO_APP_VALUE
                            try{
                                MeshProtos.User node_info = MeshProtos.User.parseFrom(data.getPayload());

                                String node_id = node_info.getId();
                                String node_hwModel =  node_info.getHwModel().toString();
                                String node_LongName = node_info.getLongName();
                                String node_PK = node_info.getPublicKey().toStringUtf8();
                                String node_shortName = node_info.getShortName();
                                String node_role = (node_info.getRole() == ConfigProtos.Config.DeviceConfig.Role.UNRECOGNIZED) ? "Unknown Role" : node_info.getRole().name();
//                                boolean isUnmessageable =  node_info.getIsUnmessagable();
//                                boolean can_be_messaged = node_info.hasIsUnmessagable();

                                output_nodeInfo.setData(packet_from, node_id, node_shortName, node_LongName, node_PK, node_hwModel, node_role);

                            }catch (InvalidProtocolBufferException e) {
                                System.err.println("ERROR parsing Node Info: " + e.getMessage());
                            }
                            break;
                        case 5: // 5 = ROUTING (THIS IS USED TO DETERMINE IF ERRORS WERE DISCOVERED IN MESSAGING
//                            MeshProtos.Routing route = MeshProtos.Routing.parseFrom(data.getPayload());
//                            if(route.hasErrorReason()){
//                                System.out.println("[ROUTING ERROR]: " + route);
//                            }
                            break;
                        case 67: // 67 = TELEMETRY

                            try{
                                TelemetryProtos.Telemetry telemetry = TelemetryProtos.Telemetry.parseFrom(data.getPayload());
                                System.out.println("[TELEMETRY DATA WAS PROVIDED");
                            } catch (InvalidProtocolBufferException e) {
                                System.err.println("ERROR parsing telemetry data: " + e.getMessage());
                            }

                            break;
                        default:
                            System.out.println("\n[UNKNOWN PORTNUM]: " + portVal + "\n");
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
