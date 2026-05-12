package org.sensorhub.impl.process.video.transcoder.coders;

import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.DoublePointer;
import org.sensorhub.impl.process.video.transcoder.helpers.CodecInfo;
import org.sensorhub.impl.process.video.transcoder.helpers.FullPixelEnum;

import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

public class SwScaler extends Codec<AVFrame, AVFrame> {
    long pts = 0;
    SwsContext swsContext;
    final int inWidth, inHeight, outWidth, outHeight;

    public SwScaler(CodecInfo inputFormat, CodecInfo outputFormat, int inWidth, int inHeight, int outWidth, int outHeight) {
        super(inputFormat, outputFormat, AVFrame.class, AVFrame.class, null);
        this.inWidth = inWidth;
        this.inHeight = inHeight;
        this.outWidth = outWidth;
        this.outHeight = outHeight;
    }

    @Override
    protected void initContext() {
        swsContext = sws_getContext(inWidth, inHeight, inputFormat.pixelFmt.ffmpegId,
                outWidth, outHeight, outputFormat.pixelFmt.ffmpegId,
                SWS_BICUBIC, null, null, (DoublePointer) null);
    }

    @Override
    protected void initOptions() {} // no options for swscaler

    @Override
    protected FullPixelEnum openContext() {
        return outputFormat.pixelFmt;
    } // no codec to open

    @Override
    public void deallocateInputPacket(AVFrame packet) {
        if (packet != null) {
            av_frame_free(packet);
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
    protected synchronized void processInputPacket(AVFrame inputPacket) {
        if (inputPacket != null) {
            AVFrame outputPacket = av_frame_alloc();
            outputPacket.format(outputFormat.pixelFmt.ffmpegId);
            outputPacket.width(outWidth);
            outputPacket.height(outHeight);
            av_frame_get_buffer(outputPacket, 0);
            sws_scale_frame(swsContext, outputPacket, inputPacket);
            addOutFrame(outputPacket);
        }
    }

    @Override
    public void close() {
        synchronized (contextLock) {
            super.close();
            sws_freeContext(swsContext);
        }
    }
}
