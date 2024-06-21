package org.sensorhub.mpegts;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.PointerPointer;

public class StreamContext {
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

        // Attempt to open the codec
        int returnCode = avcodec.avcodec_open2(codecContext, codec, (PointerPointer<?>) null);
        if (returnCode < 0) {
            throw new IllegalStateException("Cannot open codec");
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

        // Pass data buffer to the interested listener
        getDataBufferListener().onDataBuffer(new DataBufferRecord(avPacket.pts() * getStreamTimeBase(), dataBuffer));
    }
}
