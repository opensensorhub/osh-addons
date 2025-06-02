package org.sensorhub.impl.process.video.transcoder;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Decoder extends Coder<AVPacket, AVFrame> {
    public Decoder(String codecName) {
        super(codecName, AVPacket.class, AVFrame.class);
    }

    @Override
    protected void initCodec(AVCodec codec, AVCodecContext ctx) {
        codec = avcodec_find_decoder_by_name(codecName);
        ctx = avcodec_alloc_context3(codec);
        if (avcodec_open2(ctx, codec, (PointerPointer<?>)null) < 0) {
            throw new IllegalStateException("Error initializing " + codec.name().getString() + " decoder");
        }
    }

    // Get compressed packet, send to decoder
    @Override
    protected void sendPacket(AVCodecContext codec_ctx, AVPacket packet) {
        av_packet_unref(packet);
        packet = inPackets.poll();
        avcodec_send_packet(codec_ctx, packet);
    }

    // Receive uncompressed frame from decoder
    @Override
    protected void receivePacket(AVCodecContext codec_ctx, AVFrame packet) {
        while (avcodec_receive_frame(codec_ctx, packet) >= 0) {
            outPackets.add(packet);
        }
    }

    @Override
    protected void allocatePackets(AVPacket inPacket, AVFrame outPacket) {
        inPacket = av_packet_alloc();
        av_init_packet(inPacket);
        outPacket = av_frame_alloc();
    }

    @Override
    protected void deallocatePackets(AVPacket inPacket, AVFrame outPacket) {
        av_packet_free(inPacket);
        av_frame_free(outPacket);
    }
}
