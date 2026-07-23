package org.sensorhub.impl.sensor.rtmpcam.event;

public interface RtmpEvent<T> {
    public T getPayload();
}
