package org.sensorhub.impl.process.video.transcoder;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Encoder extends Coder<AVFrame, AVPacket> {
    public Encoder(String codecName) {
        super(codecName, AVFrame.class, AVPacket.class);
    }

    @Override
    protected void initCodec(AVCodec codec, AVCodecContext ctx) {
        codec = avcodec_find_encoder_by_name(codecName);
        ctx = avcodec_alloc_context3(codec);
        if (avcodec_open2(ctx, codec, (PointerPointer<?>)null) < 0) {
            throw new IllegalStateException("Error initializing " + codec.name().getString() + " encoder");
        }
    }

    @Override
    protected void sendPacket(AVCodecContext codec_ctx, AVFrame packet) {
        av_frame_unref(packet);
        packet = inPackets.poll();
        avcodec_send_frame(codec_ctx, packet);
    }

    @Override
    protected void receivePacket(AVCodecContext codec_ctx, AVPacket packet) {
        while (avcodec_receive_packet(codec_ctx, packet) >= 0) {
            outPackets.add(packet);
        }
    }

    @Override
    protected void allocatePackets(AVFrame inPacket, AVPacket outPacket) {
        inPacket = av_frame_alloc();
        outPacket = av_packet_alloc();
        av_init_packet(outPacket);
    }

    @Override
    protected void deallocatePackets(AVFrame inPacket, AVPacket outPacket) {
        av_frame_free(inPacket);
        av_packet_free(outPacket);
    }
}
