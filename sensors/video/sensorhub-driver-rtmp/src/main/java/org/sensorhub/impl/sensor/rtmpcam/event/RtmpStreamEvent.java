package org.sensorhub.impl.sensor.rtmpcam.event;

import org.sensorhub.impl.sensor.rtmpcam.stream.StreamInfo;

public class RtmpStreamEvent implements RtmpEvent<StreamInfo> {

    private final StreamInfo payload;

    public RtmpStreamEvent(StreamInfo payload) {
        this.payload = payload;
    }
    @Override
    public StreamInfo getPayload() {
        return payload;
    }
}
