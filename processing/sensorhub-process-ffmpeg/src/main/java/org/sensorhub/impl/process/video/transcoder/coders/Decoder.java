package org.sensorhub.impl.process.video.transcoder.coders;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.sensorhub.impl.process.video.transcoder.helpers.CodecInfo;
import org.sensorhub.impl.process.video.transcoder.helpers.CodecOptions;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Decoder extends Codec<AVPacket, AVFrame> {

    public Decoder(CodecInfo inFormatInfo, CodecInfo outFormatInfo, CodecOptions options) {
        super(inFormatInfo, outFormatInfo, AVPacket.class, AVFrame.class, options);
    }

    @Override
    protected void initContext() {
        synchronized (contextLock) {
            codec = avcodec_find_decoder(inputFormat.codec.ffmpegId);
            codec_ctx = avcodec_alloc_context3(codec);
            codec_ctx.codec_id(inputFormat.codec.ffmpegId);
            setCodecPixFmt(codec_ctx, outputFormat.pixelFmt);
            //codec_ctx.pix_fmt(outputFormat.pixelFmt().ffmpegId);
        }
    }

    @Override
    public void deallocateInputPacket(AVPacket packet) {
        if (packet != null) {
            av_packet_free(packet);
        }
    }

    @Override
    public void deallocateOutputPacket(AVFrame packet) {
        if (packet != null) {
            av_frame_free(packet);
        }
    }

    @Override
    protected AVFrame cloneOutput(AVFrame packet) {
        if (packet != null) {
            return av_frame_clone(packet);
        } else {
            return null;
        }
    }

    @Override
    protected synchronized void processInputPacket(AVPacket inputPacket) {
        if (inputPacket != null && !inputPacket.isNull()) {
            int ret;
            if ((ret = avcodec_send_packet(codec_ctx, inputPacket)) < 0) {
                //logger.warn("Error sending packet to decoder");
                //logFFmpeg(ret);
                //avcodec_flush_buffers(codec_ctx);
                return;
            }

            AVFrame outputPacket = av_frame_alloc();

            while (avcodec_receive_frame(codec_ctx, outputPacket) >= 0) {
                if (!outputPacket.isNull()) {
                    outQueue.add(outputPacket);
                }
                outputPacket = av_frame_alloc();
            }

            av_frame_free(outputPacket);
        }
    }
}
