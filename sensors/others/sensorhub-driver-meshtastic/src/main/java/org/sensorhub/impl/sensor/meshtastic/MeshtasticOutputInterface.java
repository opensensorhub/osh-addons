package org.sensorhub.impl.sensor.meshtastic;

import com.geeksville.mesh.MeshProtos;
import com.google.protobuf.ByteString;

public interface MeshtasticOutputInterface {
    void setData(MeshProtos.MeshPacket packet, ByteString payload);
}
