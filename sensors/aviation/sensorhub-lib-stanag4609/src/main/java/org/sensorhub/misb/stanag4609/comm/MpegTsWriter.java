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
 * High‑level writer for producing an MPEG‑TS stream containing:
 * <ul>
 *     <li>H.264 video on PID {@code 0x0100}</li>
 *     <li>MISB KLV metadata on PID {@code 0x0101}</li>
 * </ul>
 *
 * <p>This class wraps FFmpeg's muxer and a custom {@link AVIOContext} so that
 * all TS output is written directly to a user‑provided {@link OutputStream}
 * (Jetty HTTP response, socket, file, etc.).</p>
 *
 * <h3>Pipeline Overview</h3>
 * <ol>
 *     <li>FFmpeg writes video packets normally via {@code av_interleaved_write_frame()}.</li>
 *     <li>KLV metadata is muxed manually using {@link KlvMuxer} and injected as raw TS packets.</li>
 *     <li>The output stream receives a fully interleaved MPEG‑TS stream.</li>
 * </ol>
 *
 * <h3>Intended Use</h3>
 * Designed for real‑time UAS / ISR streaming where each video frame has a
 * corresponding MISB 0601/0102/0903 metadata block.
 */
public class MpegTsWriter {

    /** Destination for all MPEG‑TS bytes (HTTP, file, socket, etc.). */
    private final OutputStream out;

    /** FFmpeg muxer context for MPEG‑TS. */
    private final AVFormatContext fmtCtx;

    /** Video stream entry inside the TS container. */
    private final AVStream videoStream;

    /** Internal KLV → TS muxer for PID 0x0101. */
    private final KlvMuxer klvMuxer = new KlvMuxer();

    /** Custom IO context used by FFmpeg to write TS packets. */
    private final AVIOContext ioCtx;

    /** Backing buffer for the AVIOContext. */
    private final BytePointer ioBuffer;

    /**
     * Must be held as a field so FFmpeg does not lose the callback reference.
     * If this is garbage‑collected, FFmpeg will segfault.
     */
    private Write_packet_Pointer_BytePointer_int writeCallback;

    /**
     * Creates a new MPEG‑TS writer that outputs directly to the given stream.
     *
     * @param out    destination for TS bytes
     * @param width  video width (H.264 passthrough)
     * @param height video height
     * @param fps    frame rate (informational; no encoder is used)
     */
    public MpegTsWriter(OutputStream out, int width, int height, int fps) {
        this.out = out;

        // Allocate a fresh output context for MPEG‑TS
        avformat.avformat_alloc_output_context2(null, null, "mpegts", null);
        fmtCtx = avformat.avformat_alloc_context();
        fmtCtx.oformat(avformat.av_guess_format("mpegts", null, null));

        // Allocate IO buffer for FFmpeg's write callback
        int bufferSize = 32 * 1024;
        ioBuffer = new BytePointer(av_malloc(bufferSize));

        // Custom write callback: FFmpeg → OutputStream
        writeCallback = new Write_packet_Pointer_BytePointer_int() {
            @Override
            public int call(Pointer opaquePtr, BytePointer data, int size) {
                try {
                    byte[] bytes = new byte[size];
                    data.get(bytes);
                    out.write(bytes);
                    return size; // success
                } catch (IOException e) {
                    return -1; // signal write failure to FFmpeg
                }
            }
        };

        // Create AVIOContext using our callback
        ioCtx = avformat.avio_alloc_context(
                ioBuffer,
                bufferSize,
                1,          // writeable
                null,       // opaque
                null,       // read callback
                writeCallback,
                null        // seek callback
        );

        fmtCtx.pb(ioCtx);

        // Create video stream (PID 0x0100)
        videoStream = avformat.avformat_new_stream(fmtCtx, null);
        videoStream.id(0x0100);

        // Configure codec parameters for H.264 passthrough
        AVCodecParameters params = videoStream.codecpar();
        params.codec_type(avutil.AVMEDIA_TYPE_VIDEO);
        params.codec_id(avcodec.AV_CODEC_ID_H264);
        params.width(width);
        params.height(height);

        // Emit MPEG‑TS header (PAT/PMT + stream descriptors)
        avformat.avformat_write_header(fmtCtx, (AVDictionary) null);
    }

    /**
     * Writes a single encoded H.264 frame into the MPEG‑TS stream.
     * <p>
     * The frame must already be encoded (Annex B or AVCC NAL units). No
     * transcoding occurs; FFmpeg simply packetizes it into TS.
     *
     * @param encodedFrame raw H.264 NAL units
     * @param ptsNanos     presentation timestamp in nanoseconds
     */
    public void writeVideoFrame(byte[] encodedFrame, long ptsNanos) {
        AVPacket pkt = avcodec.av_packet_alloc();
        avcodec.av_new_packet(pkt, encodedFrame.length);

        pkt.data().put(encodedFrame);
        pkt.size(encodedFrame.length);
        pkt.stream_index(videoStream.index());

        // Convert nanoseconds → 90 kHz clock
        long pts90k = ptsNanos * 90_000L / 1_000_000_000L;
        pkt.pts(pts90k);
        pkt.dts(pts90k);

        // FFmpeg handles muxing and interleaving
        avformat.av_interleaved_write_frame(fmtCtx, pkt);

        avcodec.av_packet_unref(pkt);
        avcodec.av_packet_free(pkt);
    }

    /**
     * Writes a MISB KLV metadata block for the current frame.
     * <p>
     * KLV is muxed manually because FFmpeg does not natively support MISB
     * metadata streams. The {@link KlvMuxer} produces fully‑formed TS packets
     * on PID 0x0101, which are written directly to the output stream.
     *
     * @param klvData raw KLV (UL + BER length + value)
     * @param ptsNanos timestamp matching the associated video frame
     */
    public void writeKlv(byte[] klvData, long ptsNanos) throws IOException {
        long pts90k = ptsNanos * 90_000L / 1_000_000_000L;

        // Convert KLV → PES → TS packets
        List<byte[]> tsPackets = klvMuxer.muxKlvToTs(klvData, pts90k);

        // Inject directly into the TS stream
        for (byte[] ts : tsPackets) {
            writeRawTsPacket(ts);
        }
    }

    /**
     * Writes a raw 188‑byte TS packet to the output stream.
     * Used only for KLV PID 0x0101.
     */
    private void writeRawTsPacket(byte[] ts) throws IOException {
        out.write(ts);
    }

    /**
     * Finalizes the MPEG‑TS stream and releases all FFmpeg resources.
     * Must be called exactly once when streaming ends.
     */
    public void close() {
        avformat.av_write_trailer(fmtCtx);
        avformat.avio_context_free(ioCtx);
        avformat.avformat_free_context(fmtCtx);
    }
}
