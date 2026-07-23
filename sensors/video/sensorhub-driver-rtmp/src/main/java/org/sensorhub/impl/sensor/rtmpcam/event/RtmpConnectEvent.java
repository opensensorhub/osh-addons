package org.sensorhub.impl.sensor.rtmpcam.event;

import org.sensorhub.impl.sensor.rtmpcam.connection.RtmpConnectionContext;

public class RtmpConnectEvent implements RtmpEvent<RtmpConnectionContext> {

    private final RtmpConnectionContext payload;

    public RtmpConnectEvent(RtmpConnectionContext payload) {
        this.payload = payload;
    }

    @Override
    public RtmpConnectionContext getPayload() {
        return payload;
    }
}
