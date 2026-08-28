package org.sensorhub.impl.sensor.rtmpcam.connection;

public record RtmpConnectionContext(
        int port,
        String username,
        String password,
        String path,
        String streamKey
) {}
