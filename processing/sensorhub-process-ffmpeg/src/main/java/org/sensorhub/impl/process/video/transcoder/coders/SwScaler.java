package org.sensorhub.impl.process.video.transcoder.coders;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.PointerPointer;

import java.util.HashMap;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

public class SwScaler extends Coder<AVFrame, AVFrame> {
    long pts = 0;
    SwsContext swsContext;
    final int inPixFmt, outPixFmt, inWidth, inHeight, outWidth, outHeight;

    public SwScaler(int inPixFmt, int outPixFmt, int inWidth, int inHeight, int outWidth, int outHeight) {
        super(0, AVFrame.class, AVFrame.class, new HashMap<String, Integer>());
        this.inPixFmt = inPixFmt;
        this.outPixFmt = outPixFmt;
        this.inWidth = inWidth;
        this.inHeight = inHeight;
        this.outWidth = outWidth;
        this.outHeight = outHeight;
    }

    @Override
    protected void deallocateOutQueue() {
       outPackets.clear();
    }

    @Override
    protected void initContext() {
        pts = 0;
        swsContext = sws_getContext(inWidth, inHeight, inPixFmt,
                outWidth, outHeight, outPixFmt,
                SWS_BICUBIC, null, null, (DoublePointer) null);

    }

    @Override
    protected void sendInPacket() {
        synchronized (inPackets) {
            inPacket = inPackets.poll();
            if (inPacket == null || outPacket == null)
                return;
            //pts++;
            //inPacket.pts(pts);
            sws_scale_frame(swsContext, outPacket, inPacket);
            //avcodec_send_frame(codec_ctx, av_frame_clone(inPacket));
            //av_frame_free(inPacket);
        }
    }

    @Override
    protected void receiveOutPacket() {
        // We already have outPacket at this point (one method needed for scaling rather than two)
        // Just use this to add the output to the out queue and make the buffers writable
        outPackets.add(av_frame_clone(outPacket));
        av_frame_make_writable(outPacket);
    }

    @Override
    protected void deallocateInputPacket(AVFrame packet) {
        av_frame_free(packet);
        packet = null;
    }

    @Override
    protected void deallocateOutputPacket(AVFrame packet) {
        av_frame_free(packet);
        packet = null;
    }

    @Override
    protected void allocatePackets() {
        inPacket = av_frame_alloc();

        outPacket = av_frame_alloc();
        outPacket.format(outPixFmt);
        outPacket.width(outWidth);
        outPacket.height(outHeight);
        av_image_alloc(outPacket.data(), outPacket.linesize(),
                outWidth, outHeight, outPixFmt, 1);
    }

    @Override
    protected void deallocatePackets() {
        if (swsContext != null) {
            sws_freeContext(swsContext);
        }
        if (inPacket != null) {
            av_frame_free(inPacket);
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
            for (AVFrame frame : inPackets) {
                av_frame_free(frame);
            }
            inPackets.clear();
        }
    }
}
