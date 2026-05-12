package org.sensorhub.impl.process.video.transcoder.coders;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVPixFmtDescriptor;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.sensorhub.impl.process.video.transcoder.helpers.CodecInfo;
import org.sensorhub.impl.process.video.transcoder.helpers.CodecOptions;
import org.sensorhub.impl.process.video.transcoder.helpers.FullCodecEnum;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Encoder extends Codec<AVFrame, AVPacket> {

    public Encoder(CodecInfo inFormatInfo, CodecInfo outFormatInfo, CodecOptions options) {
        super(inFormatInfo, outFormatInfo, AVFrame.class, AVPacket.class, options);
    }

    @Override
    protected void initContext() {
        synchronized (contextLock) {
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

            if ((ret = avcodec_send_frame(codec_ctx, inputPacket)) < 0) {
                //logger.warn("Error sending packet to encoder");
                //logFFmpeg(ret);
                //avcodec_flush_buffers(codec_ctx);
                return;
            }

            AVPacket outputPacket = av_packet_alloc();

            while (avcodec_receive_packet(codec_ctx, outputPacket) >= 0) {
                if (!outputPacket.isNull()) {
                    addOutFrame(outputPacket);
                }
                outputPacket = av_packet_alloc();
            }

            av_packet_free(outputPacket);
        }
    }
}
