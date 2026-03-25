/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.comm;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static org.bytedeco.ffmpeg.global.avutil.av_malloc;

/**
 * Writes video + KLV metadata into an FFmpeg-managed MPEG-TS stream.
 * Video PID: 0x0100
 * KLV PID:   0x0101
 * <p>
 * Usage:
 * <pre>
 * OutputStream httpOut = response.getOutputStream(); // Jetty, servlet, socket, etc.
 *
 * FfmpegTsStreamWriter writer =
 *         new FfmpegTsStreamWriter(httpOut, 1920, 1080, 30);
 *
 * while (streaming) {
 *
 *     // 1. Build KLV metadata for this frame
 *     byte[] klv = new KlvFrameWriter()
 *             .uasPrecisionTimeStamp(ptsMicros)
 *             .uasSensorLatLon(lat, lon)
 *             .security(new SecurityLocalSetWriter()
 *                     .classification(0x01)
 *                     .classifyingCountry("USA"))
 *             .vmtiFromTargets(ptsMicros, 1920, 1080, 20.0, 11.0, targets)
 *             .encodeFrameMetadata();
 *
 *     // 2. Write video frame
 *     writer.writeVideoFrame(encodedH264Frame, ptsNanos);
 *
 *     // 3. Write KLV metadata
 *     writer.writeKlv(klv, ptsNanos);
 * }
 *
 * writer.close();
 * </pre>
 */
public class MpegTsWriter {

    private final OutputStream out;
    private final AVFormatContext fmtCtx;
    private final AVStream videoStream;
    private final KlvMuxer klvMuxer = new KlvMuxer();
    private final AVIOContext ioCtx;
    private final BytePointer ioBuffer;

    /**
     * Handle to the actual stream writer, must be maintained as a class member
     */
    private Write_packet_Pointer_BytePointer_int writeCallback;

    public MpegTsWriter(OutputStream out, int width, int height, int fps) {
        this.out = out;

        avformat.avformat_alloc_output_context2(null, null, "mpegts", null);
        fmtCtx = avformat.avformat_alloc_context();
        fmtCtx.oformat(avformat.av_guess_format("mpegts", null, null));

        // Allocate IO buffer
        int bufferSize = 32 * 1024;
        ioBuffer = new BytePointer(av_malloc(bufferSize));

        writeCallback = new Write_packet_Pointer_BytePointer_int() {
            @Override
            public int call(Pointer opaquePtr, BytePointer data, int size) {
                try {
                    byte[] bytes = new byte[size];
                    data.get(bytes);
                    out.write(bytes);
                    return size;
                } catch (IOException e) {
                    return -1;
                }
            }
        };

        // Create custom write callback
        ioCtx = avformat.avio_alloc_context(
                ioBuffer,
                bufferSize,
                1,
                null,
                null,
                writeCallback,
                null
        );

        fmtCtx.pb(ioCtx);

        // Create video stream
        videoStream = avformat.avformat_new_stream(fmtCtx, null);
        videoStream.id(0x0100);

        // Set codec parameters (H.264 passthrough)
        AVCodecParameters params = videoStream.codecpar();
        params.codec_type(avutil.AVMEDIA_TYPE_VIDEO);
        params.codec_id(avcodec.AV_CODEC_ID_H264);
        params.width(width);
        params.height(height);

        // Write TS header
        avformat.avformat_write_header(fmtCtx, (AVDictionary) null);
    }

    /**
     * Writes a single encoded video frame (H.264 NAL units).
     */
    public void writeVideoFrame(byte[] encodedFrame, long ptsNanos) {
        AVPacket pkt = avcodec.av_packet_alloc();
        avcodec.av_new_packet(pkt, encodedFrame.length);

        pkt.data().put(encodedFrame);
        pkt.size(encodedFrame.length);
        pkt.stream_index(videoStream.index());

        long pts90k = ptsNanos * 90_000L / 1_000_000_000L;
        pkt.pts(pts90k);
        pkt.dts(pts90k);

        avformat.av_interleaved_write_frame(fmtCtx, pkt);

        avcodec.av_packet_unref(pkt);
        avcodec.av_packet_free(pkt);
    }

    /**
     * Writes a KLV metadata blob for this frame.
     */
    public void writeKlv(byte[] klvData, long ptsNanos) throws IOException {
        long pts90k = ptsNanos * 90_000L / 1_000_000_000L;

        List<byte[]> tsPackets = klvMuxer.muxKlvToTs(klvData, pts90k);

        for (byte[] ts : tsPackets) {
            writeRawTsPacket(ts);
        }
    }

    private void writeRawTsPacket(byte[] ts) throws IOException {
        out.write(ts);
    }

    public void close() {
        avformat.av_write_trailer(fmtCtx);
        avformat.avio_context_free(ioCtx);
        avformat.avformat_free_context(fmtCtx);
    }
}
