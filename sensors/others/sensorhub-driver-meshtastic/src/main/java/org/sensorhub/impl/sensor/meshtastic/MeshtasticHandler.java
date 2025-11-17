package org.sensorhub.impl.sensor.meshtastic;

import com.geeksville.mesh.MQTTProtos;
import com.geeksville.mesh.MeshProtos;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.HashMap;
import java.util.Map;

public class MeshtasticHandler {
    private final MeshtasticSensor sensorDriver;

    //Map port numbers to output instances:
    private final Map<Integer, MeshtasticOutputInterface> outputs = new HashMap<>();

    public MeshtasticHandler(MeshtasticSensor sensorDriver){
        this.sensorDriver = sensorDriver;

        // REGISTER OUTPUTS (Meshtastic Portnum value, Output Class associated with portnum)
        outputs.put(1,sensorDriver.textOutput);     // TEXT
        outputs.put(3, sensorDriver.posOutput);     // POSITION
        outputs.put(4,sensorDriver.nodeInfoOutput); // NODE INFO (USER)
        // Add future outputs here
    }


    public void handlePacket(MeshProtos.MeshPacket packet){

        // DETERMINE PACKET MESSAGE TYPE BASED ON PROVIDED PORTNUM VALUE
        if (packet.hasDecoded()){
            MeshProtos.Data data = packet.getDecoded();
            int portVal = data.getPortnumValue();

            // LOOK UP THE OUTPUT FOR THIS PORT
            MeshtasticOutputInterface output = outputs.get(portVal);

            if(output != null){
                output.setData(packet, data.getPayload());
            } else {
                sensorDriver.genericOutput.setData(packet, null);
            }

        }
    }


}
