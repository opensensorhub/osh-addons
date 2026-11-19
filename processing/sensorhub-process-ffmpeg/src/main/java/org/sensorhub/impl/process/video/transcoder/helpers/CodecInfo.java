package org.sensorhub.impl.process.video.transcoder.helpers;

public class CodecInfo implements Cloneable {
    public FullCodecEnum codec;
    public FullPixelEnum pixelFmt;

    public static CodecInfo newCodecInfoFromName(String name) {
        FullCodecEnum codec;
        FullPixelEnum pixel;
        try {
            codec = Enum.valueOf(FullCodecEnum.class, name);
            pixel = FullPixelEnum.YUVJ420P;
        } catch (Exception e) {
            codec = FullCodecEnum.RAWVIDEO;
            pixel = Enum.valueOf(FullPixelEnum.class, name);
        }

        return new CodecInfo(codec, pixel);
    }

    public CodecInfo(FullCodecEnum codec, FullPixelEnum pixelFmt) {
        this.codec = codec;
        this.pixelFmt = pixelFmt;
    }

    @Override
    public CodecInfo clone() {
        try {
            CodecInfo clone = (CodecInfo) super.clone();
            clone.codec = codec;
            clone.pixelFmt = pixelFmt;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
