package org.sensorhub.impl.process.video.transcoder.coders;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;
import org.sensorhub.impl.process.video.transcoder.helpers.CodecInfo;
import org.sensorhub.impl.process.video.transcoder.helpers.CodecOptions;
import org.sensorhub.impl.process.video.transcoder.helpers.FullCodecEnum;
import org.sensorhub.impl.process.video.transcoder.helpers.FullPixelEnum;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Encoder extends Codec<AVFrame, AVPacket> {

    volatile long pts = 0;
    long timebaseNum, timebaseDen, framerateNum, framerateDen;
    final int GOP_SIZE = 10;
    volatile long frameSinceGop = 0;
    final Object timestampLock = new Object();

    public Encoder(CodecInfo inFormatInfo, CodecInfo outFormatInfo, CodecOptions options) {
        super(inFormatInfo, outFormatInfo, AVFrame.class, AVPacket.class, options);
    }

    // Override so we can get the timebase
    @Override
    public FullPixelEnum init() {
        var pixfmt = super.init();
        timebaseNum = codec_ctx.time_base().num();
        timebaseDen = codec_ctx.time_base().den();
        framerateNum = codec_ctx.framerate().num();
        framerateDen = codec_ctx.framerate().den();
        //GOP_SIZE = codec_ctx.gop_size();
        return pixfmt;
    }

    @Override
    protected void initContext() {
        synchronized (contextLock) {
            pts = 0;

            // For H264, prefer x264 over OpenH264 — better option compatibility
            if (outputFormat.codec == FullCodecEnum.H264) {
                codec = avcodec_find_encoder_by_name("libx264");
            }

            // Fall back to the default encoder for this codec ID
            if (codec == null || codec.isNull()) {
                codec = avcodec_find_encoder(outputFormat.codec.ffmpegId);
            }

            if (codec == null || codec.isNull()) {
                throw new IllegalStateException("Could not find encoder for: " + outputFormat.codec);
            }

            codec_ctx = avcodec_alloc_context3(codec);

            if (codec_ctx == null || codec_ctx.isNull()) {
                throw new IllegalStateException("Could not allocate encoder context for: " + codec.name().getString());
            }

            codec_ctx.gop_size(GOP_SIZE);
            codec_ctx.max_b_frames(0);

            setCodecPixFmt(codec_ctx, inputFormat.pixelFmt);

            logger.debug("Using encoder: {}", codec.name().getString());
        }
    }

    @Override
    public void deallocateInputPacket(AVFrame packet) {
        if (packet != null) {
            av_frame_free(packet);
        }
    }

    @Override
    public void deallocateOutputPacket(AVPacket packet) {
        if (packet != null) {
            av_packet_free(packet);
        }
    }

    @Override
    protected AVPacket cloneOutput(AVPacket packet) {
        if (packet != null) {
            return av_packet_clone(packet);
        } else {
            return null;
        }
    }

    @Override
    protected synchronized void processInputPacket(AVFrame inputPacket) {
        if (inputPacket != null && !inputPacket.isNull()) {
            int ret;
            if (inputPacket.format() != codec_ctx.pix_fmt()) {
                throw new IllegalArgumentException("AVFrame pixel format: " + FullPixelEnum.fromId(inputPacket.format())
                        + " incompatible with codec pixel format: " + FullPixelEnum.fromId(codec_ctx.pix_fmt()));
            }

            if (frameSinceGop++ >= GOP_SIZE) {
                frameSinceGop = 0;
                inputPacket.pict_type(AV_PICTURE_TYPE_I);
                inputPacket.flags(inputPacket.flags() | AVFrame.AV_FRAME_FLAG_KEY);
            }

            if ((ret = avcodec_send_frame(codec_ctx, inputPacket)) < 0) {
                //logger.warn("Error sending packet to encoder");
                //logFFmpeg(ret);
                //avcodec_flush_buffers(codec_ctx);
                return;
            }

            AVPacket outputPacket = av_packet_alloc();

            while (avcodec_receive_packet(codec_ctx, outputPacket) >= 0) {
                if (!outputPacket.isNull()) {
                    // Add headers if necessary/available
                    if (((outputPacket.flags() & AV_PKT_FLAG_KEY) != 0) && headers != null) {
                        int originalSize = outputPacket.size();
                        byte[] original = new byte[originalSize];
                        outputPacket.data().capacity(originalSize).position(0).get(original);
                        av_grow_packet(outputPacket, headers.length);
                        BytePointer dst = outputPacket.data().capacity((long) headers.length + originalSize);
                        dst.position(0).put(headers);
                        dst.position(headers.length).put(original);
                    }
                    addOutPacket(outputPacket);
                }
                outputPacket = av_packet_alloc();
            }

            av_packet_free(outputPacket);
        }
    }
}
