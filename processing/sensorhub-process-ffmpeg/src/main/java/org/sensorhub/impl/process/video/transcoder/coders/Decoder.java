package org.sensorhub.impl.process.video.transcoder.coders;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.PointerPointer;

import java.util.HashMap;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Decoder extends Coder<AVPacket, AVFrame> {
    public Decoder(int codecId, HashMap<String, Integer> options) {
        super(codecId, AVPacket.class, AVFrame.class, options);
    }

    @Override
    protected void initContext() {
        AVCodec codec = avcodec_find_decoder(codecId);
        codec_ctx = avcodec_alloc_context3(codec);

        initOptions(codec_ctx);

        if (avcodec_open2(codec_ctx, codec, (PointerPointer<?>)null) < 0) {
            throw new IllegalStateException("Error initializing " + codec.name().getString() + " decoder");
        }
    }

    // Get compressed packet, send to decoder
    @Override
    protected void sendInPacket() {
        inPacket = inPackets.poll();
        //logger.debug("decode send:");
        //logger.debug("  data[0]: {}", inPacket.data());
        //logger.debug("Sent frame to encoder");
        avcodec_send_packet(codec_ctx, av_packet_clone(inPacket));
        //av_packet_free(inPacket);
    }

    // Receive uncompressed frame from decoder
    @Override
    protected void receiveOutPacket() {
        synchronized (outPackets) {
            while (avcodec_receive_frame(codec_ctx, outPacket) >= 0) {
                //av_packet_free(inPacket);
                outPackets.add(av_frame_clone(outPacket));
                //av_frame_free(outPacket);
                //logger.debug("Decode Packet added");
            }
        }
    }

    @Override
    protected void deallocateInputPacket(AVPacket packet) {
        av_packet_free(packet);
        packet = null;
    }

    @Override
    protected void deallocateOutputPacket(AVFrame packet) {
        av_frame_free(packet);
        packet = null;
    }

    @Override
    protected void deallocateOutQueue() {
        outPackets.clear();
    }

    @Override
    protected void allocatePackets() {
        inPacket = new AVPacket();
        inPacket = av_packet_alloc();
        av_init_packet(inPacket);
        outPacket = new AVFrame();
        outPacket = av_frame_alloc();
    }

    @Override
    protected void deallocatePackets() {
        if (inPacket != null) {
            av_packet_free(inPacket);
        }
        if (outPacket != null) {
            av_frame_free(outPacket);
        }
        if (outPackets != null) {
            for (AVFrame frame : outPackets) {
                av_frame_free(frame);
            }
            outPackets.clear();
        }
        if (inPackets != null) {
            for (AVPacket packet : inPackets) {
                av_packet_free(packet);

            }
            inPackets.clear();
        }
    }
}
