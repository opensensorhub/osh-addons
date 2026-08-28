package org.sensorhub.impl.sensor.rtmpcam.event;

public interface RtmpEventCallback {
    void call(RtmpEvent<?> event);
}
