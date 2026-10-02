package org.sensorhub.impl.sensor.ffmpeg.config.streamfilter;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.mpegts.StreamContext;

public class MaxCountFilter extends StreamFilter {
    @DisplayInfo.Required
    @DisplayInfo(label = "Maximum Video Streams", desc = "Maximum number of video streams to output.")
    public int maxVideoStreams = 1;

    @DisplayInfo.Required
    @DisplayInfo(label = "Maximum Audio Streams", desc = "Maximum number of audio streams to output.")
    public int maxAudioStreams = 1;

    @DisplayInfo.Required
    @DisplayInfo(label = "Maximum Data Streams", desc = "Maximum number of data streams to output.")
    public int maxDataStreams = 0;

    private int videoStreams = 0, audioStreams = 0, dataStreams = 0;

    @Override
    public boolean filter(StreamContext stream) {
        return switch (stream.getStreamType()) {
            case VIDEO -> videoStreams++ < maxVideoStreams;
            case AUDIO -> audioStreams++ < maxAudioStreams;
            case DATA -> dataStreams++ < maxDataStreams;
            default -> false;
        };
    }

    @Override
    public void resetState() {
        videoStreams = 0;
        audioStreams = 0;
        dataStreams = 0;
    }
}
