package org.sensorhub.mpegts;

import org.bytedeco.ffmpeg.avcodec.*;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.BytePointer;
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

        // Best guess at useful BSFs. If a video format does not work, may
        // need to add a corresponding BSF here.
        return switch (codecId) {
            // filter_units=pass_types=1|5: Allow IDR and non-IDR slices to pass. All other units are removed. Parameter sets are added in-stream with dump_extra.
            case avcodec.AV_CODEC_ID_H264 -> "h264_mp4toannexb,filter_units=pass_types=1|5,dump_extra";
            // filter_units=pass_types=0-31: Allow slices to pass. All other units are removed. Parameter sets are added in-stream with dump_extra.
            case avcodec.AV_CODEC_ID_HEVC -> "hevc_mp4toannexb,filter_units=pass_types=0-31,dump_extra";
            case avcodec.AV_CODEC_ID_VVC -> "vvc_mp4toannexb,dump_extra";
            case avcodec.AV_CODEC_ID_EVC -> "evc_mp4toannexb,dump_extra";
            case avcodec.AV_CODEC_ID_MPEG4 -> "mpeg4_unpack_bframes,dump_extra";
            case avcodec.AV_CODEC_ID_MPEG2VIDEO, avcodec.AV_CODEC_ID_VC1, avcodec.AV_CODEC_ID_WMV3 -> "dump_extra";
            case avcodec.AV_CODEC_ID_MJPEG -> "mjpeg2jpeg";
            case avcodec.AV_CODEC_ID_VP9 -> "vp9_superframe_split";
            case avcodec.AV_CODEC_ID_AV1 -> "av1_frame_split";
            default -> null;
        };
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
                String bsfNames = selectBsfName(codecId, avFormatContext);

                // Initialize BSFs if needed
                if (bsfNames != null) {
                    bsfContext = new AVBSFContext(null);
                    if (avcodec.av_bsf_list_parse_str(bsfNames, bsfContext) < 0) {
                        throw new IllegalStateException("Failed to parse BSF list: " + bsfNames);

                    }
                    if (avcodec.avcodec_parameters_copy(bsfContext.par_in(), params) < 0) {
                        throw new IllegalStateException("Failed to copy codec parameters");
                    }

                    bsfContext.time_base_in(avFormatContext.streams(getStreamId()).time_base());

                    if (avcodec.av_bsf_init(bsfContext) < 0) {
                        throw new IllegalStateException("Failed to initialize BSF: " + bsfNames);
                    }
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
                AVPacket clonePacket = avcodec.av_packet_clone(avPacket);
                try {
                    avcodec.av_bsf_send_packet(bsfContext, clonePacket);
                    while (avcodec.av_bsf_receive_packet(bsfContext, filtered) >= 0) {
                        notifyPacketListener(filtered);
                        avcodec.av_packet_unref(filtered);
                    }
                } finally {
                    avcodec.av_packet_free(clonePacket);
                    avcodec.av_packet_free(filtered);
                }
            } else {
                // Pass packet data straight to listener
                notifyPacketListener(avPacket);
            }
        }
    }

    /**
     * Notify the listener with the data buffer extracted from the packet.
     * @param avPacket The packet containing the data buffer
     */
    private void notifyPacketListener(AVPacket avPacket) {
        byte[] dataBuffer = new byte[avPacket.size()];
        avPacket.data().get(dataBuffer);
        getDataBufferListener().onDataBuffer(new DataBufferRecord(avPacket.pts() * getStreamTimeBase(), dataBuffer));
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
