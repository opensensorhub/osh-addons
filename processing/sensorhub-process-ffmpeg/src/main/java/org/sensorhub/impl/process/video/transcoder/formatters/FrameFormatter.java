package org.sensorhub.impl.process.video.transcoder.formatters;

import org.bytedeco.ffmpeg.avutil.*;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;

import java.util.HashSet;
import java.util.Set;

import static org.bytedeco.ffmpeg.global.avutil.*;

// This class is intended to replace both RgbFormatter and YuvFormatter as a generic solution
public class FrameFormatter implements AVByteFormatter<AVFrame> {

    private final int width;
    private final int height;
    private final int pixFmt;
    private final AVPixFmtDescriptor desc;
    private final int planeCount;
    private final int[] planeWidths;
    private final int[] planeHeights;
    private final int[] planeSizes;
    private final int totalSize;
    boolean isPlanar;

    public FrameFormatter(int width, int height, int pixFmt) {
        this.width = width;
        this.height = height;
        this.pixFmt = pixFmt;
        this.desc = avutil.av_pix_fmt_desc_get(pixFmt);
        isPlanar = (desc.flags() & AV_PIX_FMT_FLAG_PLANAR) != 0;

        if (isPlanar) {
            this.planeCount = countPlanes(desc);

            planeWidths = new int[planeCount];
            planeHeights = new int[planeCount];
            planeSizes = new int[planeCount];

            calculatePlaneSizes(desc, width, height, planeWidths, planeHeights, planeSizes);
        } else {
            planeCount = 1;
            planeWidths = new int[1];
            planeHeights = new int[1];
            planeSizes = new int[1];
            planeWidths[0] = width;
            planeHeights[0] = height;
            planeSizes[0] = width * height * ((av_get_bits_per_pixel(desc) + 7) / 8);
        }

        totalSize = calcByteSize(planeSizes);
    }

    public int getPixFmt() {
        return pixFmt;
    }

    public int getTotalSize() {
        return totalSize;
    }

    private static int calcByteSize(int[] planeSizes) {
        int sum = 0;
        for (int s : planeSizes) sum += s;
        return sum;
    }

    @Override
    public AVFrame convertInput(byte[] inputData) {
        AVFrame newFrame = generateFrame();

        if (isPlanar)
            setFrameDataPlanar(newFrame, inputData);
        else
            setFrameDataPacked(newFrame, inputData);

        return newFrame;
    }

    private void setFrameDataPlanar(AVFrame newFrame, byte[] inputData) {
        int offset = 0;

        for (int p = 0; p < planeCount; p++) {
            int w = planeWidths[p];
            int h = planeHeights[p];
            int stride = newFrame.linesize(p);

            BytePointer dst = newFrame.data(p).position(0);

            int rowBytes = w;

            for (int y = 0; y < h; y++) {
                dst.position(y * stride);
                dst.put(inputData, offset + y * rowBytes, rowBytes);
            }

            offset += planeSizes[p];
        }
    }

    private void setFrameDataPacked(AVFrame newFrame, byte[] inputData) {
        BytePointer dst = newFrame.data(0);
        int linesize = newFrame.linesize(0);

        int bytesPerPixel = (av_get_bits_per_pixel(av_pix_fmt_desc_get(pixFmt)) + 7) / 8;

        int rowBytes = width * bytesPerPixel;

        int offset = 0;

        for (int y = 0; y < height; y++) {
            dst.position((long) y * linesize)
                    .put(inputData, offset, rowBytes);

            offset += rowBytes;
        }
    }

    private AVFrame generateFrame() {
        AVFrame newFrame = av_frame_alloc();
        newFrame.format(pixFmt);
        newFrame.width(width);
        newFrame.height(height);
        int ret = av_frame_get_buffer(newFrame, 0);
        if (ret < 0) {
            av_frame_free(newFrame);
            throw new IllegalStateException("Could not allocate AVFrame buffer, ffmpeg error: " + ret);
        }
        return newFrame;
    }

    @Override
    public byte[] convertOutput(AVFrame outputFrame) {
        if (outputFrame.format() != pixFmt) {
            throw new IllegalArgumentException(
                    "Unexpected frame pixel format: " + outputFrame.format() + ", expected " + pixFmt);
        }

        byte[] out = new byte[totalSize];
        int offset = 0;

        if (isPlanar) {
            for (int plane = 0; plane < planeCount; plane++) {
                int w = planeWidth(plane);
                int h = planeHeight(plane);
                int srcStride = outputFrame.linesize(plane);

                BytePointer src = outputFrame.data(plane).position(0);

                for (int y = 0; y < h; y++) {
                    src.position(y * srcStride);
                    src.get(out, offset, w);
                    offset += w;
                }
            }
        } else {
            BytePointer src = outputFrame.data(0);
            src.get(out, 0, totalSize);
        }
        return out;
    }

    private int planeWidth(int plane) {
        if (plane == 0)
            return width;
        else
            return width >> desc.log2_chroma_w();
    }

    private int planeHeight(int plane) {
        if (plane == 0)
            return height;
        else
            return height >> desc.log2_chroma_h();
    }

    // Helper functions for PLANAR formats. Untested with packed formats.
    private static int countPlanes(AVPixFmtDescriptor desc) {
        Set<Integer> planes = new HashSet<>();
        for (int i = 0; i < desc.nb_components(); i++) {
            planes.add(desc.comp(i).plane());
        }
        return planes.size();
    }

    private static void calculatePlaneSizes(AVPixFmtDescriptor desc, int width, int height, int[] planeWidths, int[] planeHeights, int[] planeSizes) {
        for (int c = 0; c < desc.nb_components(); c++) {
            AVComponentDescriptor comp = desc.comp(c);
            int p = comp.plane();

            int shiftW = (p == 0) ? 0 : desc.log2_chroma_w();
            int shiftH = (p == 0) ? 0 : desc.log2_chroma_h();

            planeWidths[p] = width >> shiftW;
            planeHeights[p] = height >> shiftH;
        }

        for (int p = 0; p < planeSizes.length; p++) {
            planeSizes[p] = planeWidths[p] * planeHeights[p];
        }
    }
}
