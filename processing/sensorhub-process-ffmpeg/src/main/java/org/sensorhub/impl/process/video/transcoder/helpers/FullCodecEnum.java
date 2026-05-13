package org.sensorhub.impl.process.video.transcoder.helpers;

import org.bytedeco.ffmpeg.global.avcodec;

import java.util.HashMap;
import java.util.Map;

public enum FullCodecEnum {
    H264(avcodec.AV_CODEC_ID_H264),
    HEVC(avcodec.AV_CODEC_ID_HEVC),
    MJPEG(avcodec.AV_CODEC_ID_MJPEG),
    VP8(avcodec.AV_CODEC_ID_VP8),
    VP9(avcodec.AV_CODEC_ID_VP9),
    MPEG2(avcodec.AV_CODEC_ID_MPEG2VIDEO),
    MPEG4(avcodec.AV_CODEC_ID_MPEG4),
    AV1(avcodec.AV_CODEC_ID_AV1),
    THEORA(avcodec.AV_CODEC_ID_THEORA),
    MPEG1VIDEO(avcodec.AV_CODEC_ID_MPEG1VIDEO),
    WMV1(avcodec.AV_CODEC_ID_WMV1),
    WMV2(avcodec.AV_CODEC_ID_WMV2),
    WMV3(avcodec.AV_CODEC_ID_WMV3),
    VC1(avcodec.AV_CODEC_ID_VC1),
    FLV1(avcodec.AV_CODEC_ID_FLV1),
    FLASHSV(avcodec.AV_CODEC_ID_FLASHSV),
    FLASHSV2(avcodec.AV_CODEC_ID_FLASHSV2),
    RV10(avcodec.AV_CODEC_ID_RV10),
    RV20(avcodec.AV_CODEC_ID_RV20),
    RV30(avcodec.AV_CODEC_ID_RV30),
    RV40(avcodec.AV_CODEC_ID_RV40),
    CINEPAK(avcodec.AV_CODEC_ID_CINEPAK),
    INDEO2(avcodec.AV_CODEC_ID_INDEO2),
    INDEO3(avcodec.AV_CODEC_ID_INDEO3),
    INDEO4(avcodec.AV_CODEC_ID_INDEO4),
    INDEO5(avcodec.AV_CODEC_ID_INDEO5),
    MSMPEG4V1(avcodec.AV_CODEC_ID_MSMPEG4V1),
    MSMPEG4V2(avcodec.AV_CODEC_ID_MSMPEG4V2),
    MSMPEG4V3(avcodec.AV_CODEC_ID_MSMPEG4V3),
    H261(avcodec.AV_CODEC_ID_H261),
    H263(avcodec.AV_CODEC_ID_H263),
    H263I(avcodec.AV_CODEC_ID_H263I),
    H263P(avcodec.AV_CODEC_ID_H263P),
    SNOW(avcodec.AV_CODEC_ID_SNOW),
    SVQ1(avcodec.AV_CODEC_ID_SVQ1),
    SVQ3(avcodec.AV_CODEC_ID_SVQ3),
    DVVIDEO(avcodec.AV_CODEC_ID_DVVIDEO),
    HUFFYUV(avcodec.AV_CODEC_ID_HUFFYUV),
    FFVHUFF(avcodec.AV_CODEC_ID_FFVHUFF),
    FFV1(avcodec.AV_CODEC_ID_FFV1),
    ASV1(avcodec.AV_CODEC_ID_ASV1),
    ASV2(avcodec.AV_CODEC_ID_ASV2),
    VCR1(avcodec.AV_CODEC_ID_VCR1),
    CLJR(avcodec.AV_CODEC_ID_CLJR),
    MDEC(avcodec.AV_CODEC_ID_MDEC),
    ROQ(avcodec.AV_CODEC_ID_ROQ),
    INTERPLAY_VIDEO(avcodec.AV_CODEC_ID_INTERPLAY_VIDEO),
    XAN_WC3(avcodec.AV_CODEC_ID_XAN_WC3),
    XAN_WC4(avcodec.AV_CODEC_ID_XAN_WC4),
    RPZA(avcodec.AV_CODEC_ID_RPZA),
    SMC(avcodec.AV_CODEC_ID_SMC),
    GIF(avcodec.AV_CODEC_ID_GIF),
    RAWVIDEO(avcodec.AV_CODEC_ID_RAWVIDEO),
    PNG(avcodec.AV_CODEC_ID_PNG),
    PPM(avcodec.AV_CODEC_ID_PPM),
    PBM(avcodec.AV_CODEC_ID_PBM),
    PGM(avcodec.AV_CODEC_ID_PGM),
    PAM(avcodec.AV_CODEC_ID_PAM),
    BMP(avcodec.AV_CODEC_ID_BMP),
    TIFF(avcodec.AV_CODEC_ID_TIFF),
    SGI(avcodec.AV_CODEC_ID_SGI),
    ALIAS_PIX(avcodec.AV_CODEC_ID_ALIAS_PIX),
    DPX(avcodec.AV_CODEC_ID_DPX),
    EXR(avcodec.AV_CODEC_ID_EXR),
    WEBP(avcodec.AV_CODEC_ID_WEBP),
    DIRAC(avcodec.AV_CODEC_ID_DIRAC),
    DNXHD(avcodec.AV_CODEC_ID_DNXHD),
    PRORES(avcodec.AV_CODEC_ID_PRORES),
    JPEG2000(avcodec.AV_CODEC_ID_JPEG2000),
    JPEGLS(avcodec.AV_CODEC_ID_JPEGLS),
    HAP(avcodec.AV_CODEC_ID_HAP);

    public int ffmpegId;

    private static final Map<Integer, FullCodecEnum> BY_ID;

    static {
        BY_ID = new HashMap<>();
        for (FullCodecEnum fmt : values()) {
            BY_ID.put(fmt.ffmpegId, fmt);
        }
    }

    public static FullCodecEnum fromId(int ffmpegId) {
        return BY_ID.getOrDefault(ffmpegId, RAWVIDEO);
    }

    FullCodecEnum(int ffmpegId)
    {
        this.ffmpegId = ffmpegId;
    }
}
