package org.sensorhub.impl.process.video;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.process.video.FFMpegDecoder;

public class FFmpegDecoderConfig extends FFmpegProcessConfig {
    public enum OutFormat {
        RGB,
        YUV;
    }

    @DisplayInfo(label="Input Video Codec")
    public FFMpegDecoder.CodecEnum inCodec = FFMpegDecoder.CodecEnum.H264;

    //@DisplayInfo(label="Decoded Image Format", desc="Uncompressed frame format.")
    //public OutFormat decodeFormat = OutFormat.RGB;

    public FFmpegDecoderConfig() {
        super();
        execProcess = FFMpegDecoder.class;
    }
}
