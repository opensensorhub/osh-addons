package org.sensorhub.impl.process.video.transcoder;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;

import java.util.HashMap;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Encoder extends Coder<AVFrame, AVPacket> {
    long pts = 0;

    public Encoder(int codecId, HashMap<String, String> options) {
        super(codecId, AVFrame.class, AVPacket.class, options);
    }

    @Override
    protected void deallocateOutQueue() {
       outPackets.clear();
    }

    @Override
    protected void initCodec() {
        pts = 0;
        AVCodec codec = avcodec_find_encoder(codecId);
        codec_ctx = avcodec_alloc_context3(codec);

        initOptions(codec_ctx);

        if (avcodec_open2(codec_ctx, codec, (PointerPointer<?>)null) < 0) {
            throw new IllegalStateException("Error initializing " + codec.name().getString() + " encoder");
        }
    }

    @Override
    protected void sendInPacket() {
        inPacket = inPackets.poll();
        //pts++;
        //inPacket.pts(pts);
        logger.debug("add frame in encoder:");
        logger.debug("  format: {}", inPacket.format());
        logger.debug("  width: {}", inPacket.width());
        logger.debug("  height: {}", inPacket.height());
        logger.debug("  data[0]: {}", inPacket.data(0));
        logger.debug("Sent frame to encoder");
        avcodec_send_frame(codec_ctx, av_frame_clone(inPacket));
        //av_frame_free(inPacket);

    }

    @Override
    protected void receiveOutPacket() {
        /*
        synchronized (outPackets) {

        }

         */
        int ret = 0;
        while (( ret = avcodec_receive_packet(codec_ctx, outPacket) ) >= 0) {
            //av_frame_free(inPacket);
            logger.debug("Packet received from encoder");
            outPackets.add(av_packet_clone(outPacket));
            //av_packet_free(outPacket);
        }
        BytePointer errorBuffer = new BytePointer(AV_ERROR_MAX_STRING_SIZE);

        //av_strerror(ret, errorBuffer, AV_ERROR_MAX_STRING_SIZE);
        //logger.debug("Receive Error: {}", errorBuffer.getString());
    }

    @Override
    protected void allocatePackets() {
        inPacket = av_frame_alloc();
        outPacket = av_packet_alloc();
        av_init_packet(outPacket);
    }

    @Override
    protected void deallocatePackets() {
        av_frame_free(inPacket);
        av_packet_free(outPacket);
        for (AVPacket packet : outPackets) {
            av_packet_free(packet);
        }
        for (AVFrame frame : inPackets) {
            av_frame_free(frame);
        }
        outPackets.clear();
        inPackets.clear();
    }
}
