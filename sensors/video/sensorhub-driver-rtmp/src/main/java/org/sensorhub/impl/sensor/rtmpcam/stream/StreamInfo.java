package org.sensorhub.impl.sensor.rtmpcam.stream;

public record StreamInfo(
        int[] videoDimensions,
        String videoCodec,
        int audioSampleRate,
        String audioCodec
) {}
