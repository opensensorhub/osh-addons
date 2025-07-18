package org.sensorhub.impl.process.video.transcoder;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.process.video.FFmpegProcessConfig;
//import org.sensorhub.impl.process.video.transcoder2.FFMpegTranscoder2;

public class FFmpegTranscoderConfig extends FFmpegProcessConfig {

    @DisplayInfo.Required
    @DisplayInfo(label="Input Codec")
    public FFMpegTranscoder.CodecEnum inCodec = FFMpegTranscoder.CodecEnum.H264;

    @DisplayInfo(label="Automatically Detect Input Codec", desc="If enabled, process will attempt to determine the input codec."
    + " If a codec could not be determined from the input data, it will fall back to the selected Input Codec.")
    public boolean detectInput = false;

    @DisplayInfo.Required
    @DisplayInfo(label="Output Codec")
    public FFMpegTranscoder.CodecEnum outCodec = FFMpegTranscoder.CodecEnum.H264;

    @DisplayInfo(label="Output Width")
    public Integer outputWidth = null;

    @DisplayInfo(label="Output Height")
    public Integer outputHeight = null;

    public FFmpegTranscoderConfig() {
        super();
        execProcess = FFMpegTranscoder.class;
    }

}
