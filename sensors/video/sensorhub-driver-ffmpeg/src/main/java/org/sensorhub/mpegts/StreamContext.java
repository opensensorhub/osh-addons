package org.sensorhub.mpegts;

import org.bytedeco.ffmpeg.avcodec.*;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamContext {

    private static final Logger logger = LoggerFactory.getLogger(StreamContext.class);
    /**
     * ID of invalid sub streams within the media stream.
     */
    private static final int INVALID_STREAM_ID = -1;

    /**
     * ID of the sub stream within the media stream.
     */
    private int streamId = INVALID_STREAM_ID;

    /**
     * Time base units for stream timing used to compute a timestamp for each packet extracted.
     */
    private double streamTimeBase;

    /**
     * Listener for buffers extracted from the stream.
     */
    private DataBufferListener dataBufferListener;

    /**
     * Name of the codec associated with the stream.
     */
    private String codecName;

    private int codecId;

    private AVBSFContext bsfContext = null;

    private boolean isInjectingExtradata = false;

    private volatile boolean isOpen = false;

    private final Object lock = new Object();

    /**
     * Returns the ID of the stream associated with this context.
     *
     * @return The stream ID.
     */
    public int getStreamId() {
        return streamId;
    }

    /**
     * Sets the ID of the stream associated with this context.
     *
     * @param streamId The stream ID.
     */
    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    /**
     * Returns the time base units for stream timing used to compute a timestamp for each packet extracted.
     *
     * @return The stream time base.
     */
    public double getStreamTimeBase() {
        return streamTimeBase;
    }

    /**
     * Sets the time base units for stream timing used to compute a timestamp for each packet extracted.
     *
     * @param streamTimeBase The stream time base.
     */
    public void setStreamTimeBase(double streamTimeBase) {
        this.streamTimeBase = streamTimeBase;
    }

    /**
     * Returns the listener for buffers extracted from the stream.
     *
     * @return The data buffer listener.
     */
    public DataBufferListener getDataBufferListener() {
        return dataBufferListener;
    }

    /**
     * Sets the listener for buffers extracted from the stream.
     *
     * @param dataBufferListener The data buffer listener.
     */
    public void setDataBufferListener(DataBufferListener dataBufferListener) {
        this.dataBufferListener = dataBufferListener;
    }

    /**
     * Returns the name of the codec associated with the stream.
     *
     * @return The codec name.
     */
    public String getCodecName() {
        return codecName;
    }

    /**
     * Sets the name of the codec associated with the stream.
     *
     * @param codecName The codec name.
     */
    private void setCodecName(String codecName) {
        this.codecName = codecName;
    }

    private void setCodecId(int codecId) { this.codecId = codecId; }

    public void setInjectingExtradata(boolean isInjectingExtradata) { this.isInjectingExtradata = isInjectingExtradata; }

    /**
     * Returns whether this context has a valid stream ID.
     *
     * @return {@code true} if the stream ID is valid, {@code false} otherwise
     */
    public boolean hasStream() {
        return streamId != INVALID_STREAM_ID;
    }

    private String selectBsfName(int codecId, AVFormatContext avFormatContext) {
        boolean needsMp4Bsf = needsMp4Bsf(avFormatContext);
        boolean isAvi = isAviContainer(avFormatContext);

        // Best guess at useful BSFs. If a video format does not work, may
        // need to add a corresponding BSF here.
        return switch (codecId) {
            case avcodec.AV_CODEC_ID_H264 ->
                    needsMp4Bsf ? "h264_mp4toannexb" : "dump_extra";
            case avcodec.AV_CODEC_ID_HEVC ->
                    needsMp4Bsf ? "hevc_mp4toannexb" : "dump_extra";
            case avcodec.AV_CODEC_ID_VVC ->
                    needsMp4Bsf ? "vvc_mp4toannexb" : "dump_extra";
            case avcodec.AV_CODEC_ID_EVC ->
                    needsMp4Bsf ? "evc_mp4toannexb" : "dump_extra";
            case avcodec.AV_CODEC_ID_MPEG4 ->
                    isAvi ? "mpeg4_unpack_bframes" : "dump_extra";
            case avcodec.AV_CODEC_ID_MPEG2VIDEO ->
                    "dump_extra";
            case avcodec.AV_CODEC_ID_VC1, avcodec.AV_CODEC_ID_WMV3 ->
                    "dump_extra";
            case avcodec.AV_CODEC_ID_MJPEG ->
                    needsMp4Bsf ? "mjpeg2jpeg" : null;
            case avcodec.AV_CODEC_ID_VP9 ->
                    needsMp4Bsf ? "vp9_superframe_split" : null;
            case avcodec.AV_CODEC_ID_AV1 ->
                    "av1_frame_split";
            default -> null;
        };
    }

    private boolean isAviContainer(AVFormatContext avFormatContext) {
        if (avFormatContext.iformat() == null) return false;
        String name = avFormatContext.iformat().name().getString();
        return name != null && name.equals("avi");
    }

    private boolean needsMp4Bsf(AVFormatContext avFormatContext) {
        if (avFormatContext.iformat() == null) return false;
        String name = avFormatContext.iformat().name().getString();
        if (name == null) return false;
        // All containers that use AVCC/HVCC length-prefix framing
        return name.contains("mov")        // mp4, mov, m4a, 3gp, 3g2, mj2
                || name.contains("matroska")   // mkv, webm
                || name.equals("flv")          // flv, rtmp
                || name.equals("mxf");         // professional broadcast format
    }

    /**
     * Opens the codec context, and sets it up according to the {@link StreamContext#streamId}.
     * This method must be called before any packets are decoded.
     *
     * @throws IllegalStateException if the codec is unsupported or cannot be opened.
     */
    public void openCodecContext(AVFormatContext avFormatContext) throws IllegalStateException {
        synchronized (lock) {
            if (isOpen) return;
            if (!hasStream()) return;

            AVCodecParameters params = avFormatContext.streams(getStreamId()).codecpar();

            // Get the associated codec from the ID stored in the context
            AVCodec codec = avcodec.avcodec_find_decoder(params.codec_id());

            if (codec == null) {
                throw new IllegalStateException("Unsupported codec");
            }

            // Store the codec name
            setCodecName(codec.name().getString());
            setCodecId(codec.id());

            if (isInjectingExtradata) {
                String bsfName = selectBsfName(codecId, avFormatContext);

                if (bsfName != null) {
                    AVBitStreamFilter filter = avcodec.av_bsf_get_by_name(bsfName);
                    if (filter == null) {
                        throw new IllegalStateException("BSF not found: " + bsfName);
                    }
                    PointerPointer<AVBSFContext> bsfPtr = new PointerPointer<>(1);
                    avcodec.av_bsf_alloc(filter, bsfPtr);
                    bsfContext = bsfPtr.get(AVBSFContext.class);
                    avcodec.avcodec_parameters_copy(bsfContext.par_in(), params);
                    bsfContext.time_base_in(avFormatContext.streams(getStreamId()).time_base());
                    if (avcodec.av_bsf_init(bsfContext) < 0) {
                        throw new IllegalStateException("Failed to initialize BSF: " + bsfName);
                    }
                    logger.debug("Using BSF {} for codec {} (format: {})",
                            bsfName, codecName, avFormatContext.iformat().name().getString());
                } else {
                    logger.debug("No BSF needed for codec {}", codecName);
                }
            }
            isOpen = true;
        }
    }

    /**
     * Processes the given packet, extracting the data buffer and passing it to the listener.
     * The packet will be processed only if a listener is set and the packet is associated with this stream.
     *
     * @param avPacket The packet to process
     */
    public void processPacket(AVPacket avPacket) {
        synchronized (lock) {
            if (!isOpen) return;
            if (getStreamId() == INVALID_STREAM_ID) return;
            if (getDataBufferListener() == null) return;
            if (avPacket.stream_index() != getStreamId()) return;


            if (bsfContext != null) {
                AVPacket filtered = avcodec.av_packet_alloc();
                try {
                    avcodec.av_bsf_send_packet(bsfContext, avPacket);
                    while (avcodec.av_bsf_receive_packet(bsfContext, filtered) >= 0) {
                        byte[] dataBuffer = new byte[filtered.size()];
                        filtered.data().get(dataBuffer);
                        getDataBufferListener().onDataBuffer(
                                new DataBufferRecord(filtered.pts() * getStreamTimeBase(), dataBuffer));
                        avcodec.av_packet_unref(filtered);
                    }
                } finally {
                    avcodec.av_packet_free(filtered);
                }
            } else {
                // Extract the data buffer from the packet
                byte[] dataBuffer = new byte[avPacket.size()];
                avPacket.data().get(dataBuffer);
                getDataBufferListener().onDataBuffer(new DataBufferRecord(avPacket.pts() * getStreamTimeBase(), dataBuffer));
            }
        }
    }

    public void close() {
        synchronized (lock) {
            if (!isOpen) return;
            isOpen = false;

            if (bsfContext != null) {
                avcodec.av_bsf_free(bsfContext);
                bsfContext = null;
            }
        }
    }
}
