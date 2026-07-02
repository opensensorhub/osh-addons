package org.sensorhub.impl.sensor.rtmpcam.connection;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.sensorhub.impl.sensor.rtmpcam.event.RtmpConnectEvent;
import org.sensorhub.impl.sensor.rtmpcam.event.RtmpDisconnectEvent;
import org.sensorhub.impl.sensor.rtmpcam.event.RtmpStreamEvent;
import org.sensorhub.impl.sensor.rtmpcam.stream.StreamInfo;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.StreamContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

/**
 * Handles one RTMP connection:
 *   1. Handshake
 *   2. AMF0 negotiation → {@link RtmpConnectionContext}
 *   3. Route to a matching {@link RtmpListener}
 *   4. RTMP chunks → FLV pipe → FFmpeg custom AVIO
 *   5. Deliver encoded packets via {@link RtmpListener#publish}
 */
class RtmpConnectionHandler {

    private static final int AVIO_BUF = 64 * 1024;
    private static final Logger logger = LoggerFactory.getLogger(RtmpConnectionHandler.class);

    private final Socket              socket;
    private final int                 port;
    private final RtmpListenerManager manager;

    private final StreamContext videoStreamContext = new StreamContext();
    private final StreamContext audioStreamContext = new StreamContext();
    private final StreamContext dataStreamContext = new StreamContext();

    private final Map<Integer, StreamContext> streamContextMap = new HashMap<>();

    RtmpConnectionHandler(Socket socket, int port, RtmpListenerManager manager) {
        this.socket  = socket;
        this.port    = port;
        this.manager = manager;
    }

    void handle() {
        try (socket) {
            var in  = new DataInputStream(socket.getInputStream());
            var out = new DataOutputStream(socket.getOutputStream());

            // One negotiator instance owns all state across all three phases
            RtmpNegotiator negotiator = new RtmpNegotiator(in, out, port);

            negotiator.doHandshake();
            RtmpConnectionContext ctx = negotiator.negotiate();

            Optional<RtmpListener> match = manager.route(ctx);
            if (match.isEmpty()) {
                // No listener registered for this combination — drop silently
                System.out.printf("[RTMP:%d] No listener for path='%s' key='%s'%n",
                        port, ctx.path(), ctx.streamKey());
                return;
            }

            RtmpListener listener = match.get();
            RtmpConnectEvent connectEvent = new RtmpConnectEvent(ctx);
            listener.onConnected(connectEvent);

            try {
                pipeToFfmpeg(negotiator.buildFlvStream(), listener);
            } finally {
                listener.onDisconnected(new RtmpDisconnectEvent(ctx));
            }

        } catch (Exception e) {
            logger.error("[RTMP:{}] Error handling RTMP connection", port, e);
        }
    }

    // ── FFmpeg pipeline ────────────────────────────────────────────────────

    private void pipeToFfmpeg(InputStream flvStream, RtmpListener listener) {

        // Both must stay reachable for the pipeline's lifetime:
        //   readCb  — stored as a raw native function pointer inside AVIOContext
        //   avioBuf — FFmpeg takes ownership; free via ctx.buffer(), not this reference
        Read_packet_Pointer_BytePointer_int readCb  = buildReadCb(flvStream);
        BytePointer avioBuf = new BytePointer(av_malloc(AVIO_BUF)).capacity(AVIO_BUF);

        AVIOContext avioCtx = avio_alloc_context(
                avioBuf, AVIO_BUF,
                0,       // read-only
                (Pointer) null,    // opaque
                (Read_packet_Pointer_BytePointer_int) readCb, (Write_packet_Pointer_BytePointer_int) null, (Seek_Pointer_long_int) null); // no write, no seek (live stream)

        AVFormatContext fmtCtx = avformat_alloc_context();
        fmtCtx.pb(avioCtx);  // must be set before avformat_open_input

        int ret = avformat_open_input(fmtCtx, (String) null,
                av_find_input_format("flv"), null);

        if (ret < 0) { logError("avformat_open_input", ret); freeAVIO(avioCtx); return; }

        logger.debug("Here 1");
        avformat_find_stream_info(fmtCtx, (AVDictionary) null);

        logger.debug("Here 2");
        streamContextSetup(fmtCtx, listener);
        logger.debug("Here 3");

        packetLoop(fmtCtx, listener);

        avformat_close_input(fmtCtx);
        freeAVIO(avioCtx);
        // readCb and avioBuf are now safe to collect
    }

    private StreamInfo queryEmbeddedStreams(AVFormatContext avFormatContext) {
        streamContextMap.clear();

        int[] videoDimensions = new int[2];
        String videoCodec = null;
        int audioSampleRate = 0;
        String audioCodec = null;

        for (int streamId = 0; streamId < avFormatContext.nb_streams(); ++streamId) {
            var stream = avFormatContext.streams(streamId);
            var codecpar = stream.codecpar();
            int codecType = codecpar.codec_type();

            AVRational timeBase = avFormatContext.streams(streamId).time_base();
            double timeBaseUnits = (double) timeBase.num() / timeBase.den();

            if (!videoStreamContext.hasStream() && codecType == AVMEDIA_TYPE_VIDEO) {
                logger.debug("Video stream present with id: {}", streamId);

                try (AVCodec avCodec = avcodec_find_decoder(codecpar.codec_id())) {
                    if (avCodec == null) {
                        logger.error("Unsupported codec: {}", codecpar.codec_id());
                        continue;
                    } else {
                        videoCodec = avCodec.name().getString();
                    }
                }

                videoDimensions[0] = codecpar.width();
                videoDimensions[1] = codecpar.height();

                videoStreamContext.setStreamId(streamId);
                videoStreamContext.setStreamTimeBase(timeBaseUnits);
                streamContextMap.put(streamId, videoStreamContext);
            } else if (!audioStreamContext.hasStream() && codecType == AVMEDIA_TYPE_AUDIO) {
                logger.debug("Audio stream present with id: {}", streamId);

                try (AVCodec avCodec = avcodec_find_decoder(codecpar.codec_id())) {
                    if (avCodec == null) {
                        logger.error("Unsupported codec: {}", codecpar.codec_id());
                        continue;
                    } else {
                        audioCodec = avCodec.name().getString();
                    }
                }
                audioSampleRate = codecpar.sample_rate();

                audioStreamContext.setStreamId(streamId);
                audioStreamContext.setStreamTimeBase(timeBaseUnits);
                streamContextMap.put(streamId, audioStreamContext);
            } else if (!dataStreamContext.hasStream() && codecType == AVMEDIA_TYPE_DATA) {
                logger.debug("Data stream present with id: {}", streamId);

                dataStreamContext.setStreamId(streamId);
                dataStreamContext.setStreamTimeBase(timeBaseUnits);
                streamContextMap.put(streamId, dataStreamContext);
            }
        }

        return new StreamInfo(videoDimensions, videoCodec, audioSampleRate, audioCodec);
    }

    private void streamContextSetup(AVFormatContext avFormatContext, RtmpListener listener) {
        var streamInfo = queryEmbeddedStreams(avFormatContext);

        videoStreamContext.setInjectingExtradata(true);
        videoStreamContext.openCodecContext(avFormatContext);
        audioStreamContext.openCodecContext(avFormatContext);
        dataStreamContext.openCodecContext(avFormatContext);

        RtmpStreamEvent streamEvent = new RtmpStreamEvent(streamInfo);
        listener.onStreamConnected(streamEvent);

        if (listener.getVideoOutput() != null)
            videoStreamContext.setDataBufferListener(listener.getVideoOutput());
        if (listener.getAudioOutput() != null)
            audioStreamContext.setDataBufferListener(listener.getAudioOutput());
    }

    public void setVideoBufferListener(@Nonnull DataBufferListener videoDataBufferListener) {
        videoStreamContext.setDataBufferListener(videoDataBufferListener);
    }

    public void setAudioBufferListener(@Nonnull DataBufferListener audioDataBufferListener) {
        audioStreamContext.setDataBufferListener(audioDataBufferListener);
    }

    public void setDataBufferListener(@Nonnull DataBufferListener dataBufferListener) {
        dataStreamContext.setDataBufferListener(dataBufferListener);
    }

    private void packetLoop(AVFormatContext fmtCtx, RtmpListener listener) {
        AVPacket pkt = av_packet_alloc();
        try {
            int ret;
            while ((ret = av_read_frame(fmtCtx, pkt)) >= 0 && listener.doStreamProcessing()) {
                StreamContext streamContext = streamContextMap.get(pkt.stream_index());
                if (streamContext != null) {
                    streamContext.processPacket(pkt);
                }
                av_packet_unref(pkt);
            }
            if (ret != AVERROR_EOF) logError("av_read_frame", ret);
        } finally {
            av_packet_free(pkt);
        }
    }

    private Read_packet_Pointer_BytePointer_int buildReadCb(InputStream src) {
        byte[] tmp = new byte[AVIO_BUF];
        return new Read_packet_Pointer_BytePointer_int() {
            @Override
            public int call(Pointer opaque, BytePointer dst, int requested) {
                try {
                    int n = src.read(tmp, 0, Math.min(requested, tmp.length));
                    if (n <= 0) return AVERROR_EOF;
                    dst.put(tmp, 0, n);
                    return n;
                } catch (IOException e) {
                    return AVERROR_EOF;
                }
            }
        };
    }

    private static void freeAVIO(AVIOContext ctx) {
        if (ctx == null || ctx.isNull()) return;
        avio_context_free(ctx);
    }

    private static void logError(String fn, int code) {
        try (BytePointer buf = new BytePointer(128)) {
            av_strerror(code, buf, buf.capacity());
            logger.warn("FFmpeg returned error code {} from {}: {}", code, fn, buf.getString());
        }
    }
}