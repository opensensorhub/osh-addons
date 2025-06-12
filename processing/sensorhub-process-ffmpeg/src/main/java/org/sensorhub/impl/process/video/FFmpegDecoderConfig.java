package org.sensorhub.impl.process.video;

import org.sensorhub.api.config.DisplayInfo;

public class FFmpegDecoderConfig extends FFmpegProcessConfig {
    public enum OutFormat {
        RGB,
        YUV;
    }

    @DisplayInfo(label="Decoded Image Format", desc="Uncompressed frame format.")
    public OutFormat decodeFormat = OutFormat.RGB;
}
