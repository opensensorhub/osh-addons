package org.sensorhub.impl.sensor.meshtastic;

import org.meshtastic.proto.MeshProtos;
import com.google.protobuf.ByteString;

public interface MeshtasticOutputInterface {
    void setData(MeshProtos.MeshPacket packet, ByteString payload);
}
