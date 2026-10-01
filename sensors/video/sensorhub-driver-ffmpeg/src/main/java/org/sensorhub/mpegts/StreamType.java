package org.sensorhub.mpegts;

import org.bytedeco.ffmpeg.global.avutil;

public enum StreamType {
    UNKNOWN, VIDEO, AUDIO, DATA, SUBTITLE, ATTACHMENT;

    static StreamType fromFFmpeg(int streamType) {
        return switch (streamType) {
            case avutil.AVMEDIA_TYPE_VIDEO -> VIDEO;
            case avutil.AVMEDIA_TYPE_AUDIO -> AUDIO;
            case avutil.AVMEDIA_TYPE_DATA -> DATA;
            case avutil.AVMEDIA_TYPE_SUBTITLE -> SUBTITLE;
            case avutil.AVMEDIA_TYPE_ATTACHMENT -> ATTACHMENT;
            default -> UNKNOWN;
        };
    }

    static int toFFmpeg(StreamType streamType) {
        return switch (streamType) {
            case VIDEO -> avutil.AVMEDIA_TYPE_VIDEO;
            case AUDIO -> avutil.AVMEDIA_TYPE_AUDIO;
            case DATA -> avutil.AVMEDIA_TYPE_DATA;
            case SUBTITLE -> avutil.AVMEDIA_TYPE_SUBTITLE;
            case ATTACHMENT -> avutil.AVMEDIA_TYPE_ATTACHMENT;
            case UNKNOWN -> avutil.AVMEDIA_TYPE_UNKNOWN;
        };
    }
}
