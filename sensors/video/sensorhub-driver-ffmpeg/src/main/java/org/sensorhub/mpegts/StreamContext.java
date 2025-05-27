package org.sensorhub.mpegts;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

public class StreamContext {

    private static final Logger logger = LoggerFactory.getLogger(StreamContext.class);
    /**
     * ID of invalid sub streams within the media stream.
     */
    private static final int INVALID_STREAM_ID = -1;

    /**
     * Codec context associated with the stream.
     */
    private AVCodecContext codecContext;

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

    private byte[] extraData = null;

    private boolean isInjectingExtradata = false;

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

    /**
     * Closes the codec context and cleans up its associated resources.
     * This method must be called when the codec context is no longer needed to clean up resources.
     */
    public void closeCodecContext() {
        if (codecContext == null) return;

        avcodec.avcodec_close(codecContext);
        avcodec.avcodec_free_context(codecContext);

        codecContext = null;
    }

    /**
     * Opens the codec context, and sets it up according to the {@link StreamContext#streamId}.
     * This method must be called before any packets are decoded.
     *
     * @throws IllegalStateException if the codec is unsupported or cannot be opened.
     */
    public void openCodecContext(AVFormatContext avFormatContext) throws IllegalStateException {
        if (!hasStream()) return;

        codecContext = avcodec.avcodec_alloc_context3(null);

        // Store the codec parameters in the codec context
        avcodec.avcodec_parameters_to_context(codecContext, avFormatContext.streams(getStreamId()).codecpar());

        // Get the associated codec from the ID stored in the context
        AVCodec codec = avcodec.avcodec_find_decoder(codecContext.codec_id());

        if (codec == null) {
            throw new IllegalStateException("Unsupported codec");
        }

        // Store the codec name
        setCodecName(codec.name().getString());
        setCodecId(codec.id());

        // Attempt to open the codec
        int returnCode = avcodec.avcodec_open2(codecContext, codec, (PointerPointer<?>) null);
        if (returnCode < 0) {
            throw new IllegalStateException("Cannot open codec");
        }

        if (isInjectingExtradata && codecId == avcodec.AV_CODEC_ID_H264) {
            BytePointer extra = avFormatContext.streams(getStreamId()).codecpar().extradata();
            int extraLen = avFormatContext.streams(getStreamId()).codecpar().extradata_size();
            extraData = getAnnexBExtradata(extra, extraLen);
        } else {
            extraData = null;
        }
    }

    /**
     * Processes the given packet, extracting the data buffer and passing it to the listener.
     * The packet will be processed only if a listener is set and the packet is associated with this stream.
     *
     * @param avPacket The packet to process
     */
    public void processPacket(AVPacket avPacket) {
        if (getStreamId() == INVALID_STREAM_ID) return;
        if (getDataBufferListener() == null) return;
        if (avPacket.stream_index() != getStreamId()) return;

        // Extract the data buffer from the packet
        byte[] dataBuffer = new byte[avPacket.size()];
        avPacket.data().get(dataBuffer);

        // Add extradata if the packet has an h264 keyframe
        if (extraData != null && (avPacket.flags() & avcodec.AV_PKT_FLAG_KEY) != 0) {
            getDataBufferListener().onDataBuffer(new DataBufferRecord(avPacket.pts() * getStreamTimeBase(), extraData));
        }
        // Pass data buffer to the interested listener
        getDataBufferListener().onDataBuffer(new DataBufferRecord(avPacket.pts() * getStreamTimeBase(), dataBuffer));
    }

    /**
     * Converts the given extradata from AVCC format to Annex B format.
     * If the data is already in Annex B format, it is returned directly.
     *
     * @param extradata A BytePointer containing the codec extradata. Must not be null.
     * @param size The size of the extradata in bytes.
     * @return A byte array containing the Annex B formatted extradata, or {@code null} if the data
     *         is invalid, cannot be processed, or if an error occurs during processing.
     */
    private byte[] getAnnexBExtradata(BytePointer extradata, int size) {
        if (extradata == null || size < 7) return null;

        byte[] data = new byte[size];
        extradata.get(data);

        // Check for Annex B start code, either 0x000001 or 0x00000001
        if (data[0] == 0x00 && data[1] == 0x00 && (data[2] == 0x01 || (data[2] == 0x00 && data[3] == 0x01))) {
            // Already in Annex B format, use directly
            return data;
        }

        // Otherwise, we need to convert AVCC to Annex B format
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int pos = 5;
        int numSps = data[pos++] & 0x1F;
        try {
            for (int i = 0; i < numSps; i++) {
                if (pos + 2 > data.length) return null;
                int spsLen = ((data[pos++] & 0xFF) << 8) | (data[pos++] & 0xFF);
                if (pos + spsLen > data.length) return null;
                out.write(new byte[]{0x00, 0x00, 0x00, 0x01});
                out.write(data, pos, spsLen);
                pos += spsLen;
            }

            int numPps = data[pos++] & 0xFF;
            for (int i = 0; i < numPps; i++) {
                if (pos + 2 > data.length) return null;
                int ppsLen = ((data[pos++] & 0xFF) << 8) | (data[pos++] & 0xFF);
                if (pos + ppsLen > data.length) return null;
                out.write(new byte[]{0x00, 0x00, 0x00, 0x01});
                out.write(data, pos, ppsLen);
                pos += ppsLen;
            }
        } catch (Exception e) {
            logger.error("Error extracting SPS and PPS from AVCC extradata", e);
        }
        return out.toByteArray();
    }
}
