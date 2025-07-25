package org.sensorhub.impl.process.video.transcoder.formatters;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

public class RgbFormatter extends AVByteFormatter<AVFrame> {

    protected final int width, height, size;
    protected final int pixFormat;

    public RgbFormatter(int width, int height) {
        this.width = width;
        this.height = height;
        this.size = calcSize();
        this.pixFormat = getPixFormat();
    }

    public int getPixFormat() {
        return AV_PIX_FMT_RGB24;
    }

    /**
     * Converts an array of bytes from uncompressed video into an {@link AVFrame}.
     * @param inputData Uncompressed video data.
     * @return An {@link AVFrame} with default settings and underlying data equal to {@code inputData}.
     */
    @Override
    public AVFrame convertInput(byte[] inputData) {
        AVFrame newFrame = generateFrame();

        setFrameData(newFrame, inputData);

        return newFrame;
    }

    private AVFrame generateFrame() {
        AVFrame newFrame = av_frame_alloc();
        newFrame.format(pixFormat);
        newFrame.width(width);
        newFrame.height(height);
        av_frame_get_buffer(newFrame, 32);
        return newFrame;
    }

    // Override this in subclasses
    protected void setFrameData(AVFrame frame, byte[] inputData) {
        frame.data(0).position(0);
        frame.data(0).put(inputData.clone(), 0, inputData.length);
    }

    /**
     * Returns the uncompressed video data from an {@link AVFrame}.
     * @param outFrame The uncompressed {@link AVFrame}.
     * @return Byte array of uncompressed data stored in {@code outFrame}.
     */
    @Override
    public byte[] convertOutput(AVFrame outFrame) {
        byte[] outData = new byte[size];
        outFrame.data(0).get(outData);
        return outData;
    }

    // Override this for YUV420P (and any other uncompressed formats)
    // Total number of bytes across all planes
    protected int calcSize() {
        return width * height * 3;
    }
}