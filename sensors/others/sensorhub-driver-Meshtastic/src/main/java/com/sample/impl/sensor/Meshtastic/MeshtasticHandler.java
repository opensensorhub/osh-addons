package com.sample.impl.sensor.Meshtastic;

import com.geeksville.mesh.MQTTProtos;
import com.geeksville.mesh.MeshProtos;
import com.google.protobuf.InvalidProtocolBufferException;

public class MeshtasticHandler {
    byte[] payload;

    MeshtasticSensor sensorDriver;

    public MeshtasticHandler(MeshtasticSensor sensorDriver){
        this.sensorDriver = sensorDriver;
    }


    public void handlePacket(MeshProtos.MeshPacket packet){
        // DETERMINE PACKET MESSAGE TYPE BASED ON PROVIDED PORTNUM VALUE
        if (packet.hasDecoded()){
            MeshProtos.Data data = packet.getDecoded();
            int portVal = data.getPortnumValue();
            switch (portVal){
                case 1: // 1 = TEXT MESSAGE
                    String msg = data.getPayload().toStringUtf8();

                    // Update Text Output
                    sensorDriver.textOutput.setData(packet, msg);
                    break;
                case 3: // 3 = POSITION
                    try{
                        MeshProtos.Position pos = MeshProtos.Position.parseFrom(data.getPayload());

                        // Update Position Output
                        sensorDriver.posOutput.setData(packet, pos);

                    }catch (InvalidProtocolBufferException e) {
                        System.err.println("ERROR parsing Position: " + e.getMessage());
                    }
                    break;
                case 4: // 4 = NODEINFO_APP_VALUE
                    try{
                        MeshProtos.User node_info = MeshProtos.User.parseFrom(data.getPayload());
                        sensorDriver.nodeInfoOutput.setData(packet, node_info);

                    }catch (InvalidProtocolBufferException e) {
                        System.err.println("ERROR parsing Node Info: " + e.getMessage());
                    }
                    break;
                case 73: // 73 = MAP REPORT
                    try{
                        System.out.println("[RETRIEVED A MAP REPORT]");
                        MQTTProtos.MapReport map_report = MQTTProtos.MapReport.parseFrom(data.getPayload());
                        System.out.println(map_report);
//                                output_nodeInfo.setData(packet, node_info);

                    }catch (InvalidProtocolBufferException e) {
                        System.err.println("ERROR parsing MAP REPORT: " + e.getMessage());
                    }
                    break;
                default:
                    sensorDriver.genericOutput.setData(packet);
                    break;
            }
        }
    }


}
