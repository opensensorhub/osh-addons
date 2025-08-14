package org.sensorhub.impl.process.video.transcoder.formatters;

import org.bytedeco.ffmpeg.avutil.AVFrame;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class YuvFormatter extends RgbFormatter {
    private final int yPlaneSize, uvPlaneSize;

    public YuvFormatter(int width, int height) {
        super(width, height);
        yPlaneSize = width * height;
        uvPlaneSize = yPlaneSize / 2;
    }

    @Override
    public int getPixFormat() {
        return AV_PIX_FMT_YUV420P;
    }

    @Override
    protected void setFrameData(AVFrame frame, byte[] inputData) {
        frame.data(0).position(0);
        frame.data(0).put(Arrays.copyOfRange(inputData, 0, yPlaneSize));
        frame.data(1).put(Arrays.copyOfRange(inputData, yPlaneSize, yPlaneSize + uvPlaneSize));
        frame.data(2).put(Arrays.copyOfRange(inputData, yPlaneSize + uvPlaneSize, yPlaneSize + uvPlaneSize * 2));
    }

    @Override
    protected int calcSize() {
        return width * height * 2; // (Y)Area + (U)1/2 Area + (V)1/2 Area
    }

    @Override
    public byte[] convertOutput(AVFrame outFrame) {
        ByteBuffer frameData = ByteBuffer.allocate(outFrame.data().sizeof());
        byte[] temp = new byte[outFrame.data(0).sizeof()];
        // TODO verify this works
        for (int i = 0; i < 3; i++) {
            outFrame.data(i).get(temp);
            frameData.put(temp);
        }
        return frameData.array();
    }
}
