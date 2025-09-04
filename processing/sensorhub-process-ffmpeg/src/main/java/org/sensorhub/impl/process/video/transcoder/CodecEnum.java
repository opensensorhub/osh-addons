package org.sensorhub.impl.process.video.transcoder;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MPEG2TS;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MPEG4;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_VP8;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_VP9;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGB24;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;

public enum CodecEnum {
    //AUTO("auto"),
    H264(AV_CODEC_ID_H264),
    H265(AV_CODEC_ID_H265),
    HEVC(AV_CODEC_ID_HEVC), // HEVC and H265 are the same. Having both in this enum helps with auto codec detection.
    MJPEG(AV_CODEC_ID_MJPEG),
    VP8(AV_CODEC_ID_VP8),
    VP9(AV_CODEC_ID_VP9),
    MPEG2(AV_CODEC_ID_MPEG2TS),
    MPEG4(AV_CODEC_ID_MPEG4),
    RGB(AV_PIX_FMT_RGB24),
    YUV(AV_PIX_FMT_YUV420P);

    int ffmpegId;

    CodecEnum(int ffmpegId)
    {
        this.ffmpegId = ffmpegId;
    }
}