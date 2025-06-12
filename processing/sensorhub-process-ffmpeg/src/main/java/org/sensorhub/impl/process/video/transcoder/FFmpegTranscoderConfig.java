package org.sensorhub.impl.process.video.transcoder;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.process.video.FFmpegProcessConfig;
import org.sensorhub.impl.process.video.transcoder2.FFMpegTranscoder2;

public class FFmpegTranscoderConfig extends FFmpegProcessConfig {

    @DisplayInfo.Required
    @DisplayInfo(label="Input Codec")
    public FFMpegTranscoder.CodecEnum inCodec = FFMpegTranscoder.CodecEnum.H264;

    @DisplayInfo.Required
    @DisplayInfo(label="Output Codec")
    public FFMpegTranscoder.CodecEnum outCodec = FFMpegTranscoder.CodecEnum.H264;

    public FFmpegTranscoderConfig() {
        super();
        execProcess = FFMpegTranscoder.class;
    }

}
