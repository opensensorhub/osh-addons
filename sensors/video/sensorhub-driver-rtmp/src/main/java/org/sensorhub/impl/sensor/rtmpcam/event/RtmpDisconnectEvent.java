package org.sensorhub.impl.sensor.rtmpcam.event;

import org.sensorhub.impl.sensor.rtmpcam.connection.RtmpConnectionContext;

public class RtmpDisconnectEvent implements RtmpEvent<RtmpConnectionContext> {
    RtmpConnectionContext payload;

    public RtmpDisconnectEvent(RtmpConnectionContext payload) {
        this.payload = payload;
    }

    @Override
    public RtmpConnectionContext getPayload() {
        return payload;
    }
}
