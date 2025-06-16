package org.sensorhub.impl.process.video.transcoder;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.PointerPointer;

import java.util.HashMap;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Decoder extends Coder<AVPacket, AVFrame> {
    public Decoder(int codecId, HashMap<String, String> options) {
        super(codecId, AVPacket.class, AVFrame.class, options);
    }

    @Override
    protected void initCodec() {
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
        avcodec_send_packet(codec_ctx, inPacket);
    }

    // Receive uncompressed frame from decoder
    @Override
    protected void receiveOutPacket() {
        synchronized (outPackets) {
            while (avcodec_receive_frame(codec_ctx, outPacket) >= 0) {
                //av_packet_free(inPacket);
                outPackets.add(av_frame_clone(outPacket));


                logger.debug("Decode Packet added");
            }
        }
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
        av_packet_free(inPacket);
        av_frame_free(outPacket);
    }
}
