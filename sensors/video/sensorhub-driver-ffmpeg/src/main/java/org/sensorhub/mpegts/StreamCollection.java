package org.sensorhub.mpegts;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StreamCollection {
    private static final Logger logger = LoggerFactory.getLogger(StreamCollection.class);

    final StreamContext[] streams;
    final Map<StreamType, LinkedHashSet<StreamContext>> streamTypeMap;

    public StreamCollection(int streamCount) {
        streams = new StreamContext[streamCount];
        streamTypeMap = generateStreamTypeMap();
    }

    private static Map<StreamType, LinkedHashSet<StreamContext>> generateStreamTypeMap() {
        HashMap<StreamType, LinkedHashSet<StreamContext>> map = new HashMap<>();

        for (StreamType streamType : StreamType.values()) {
            map.put(streamType, new LinkedHashSet<>());
        }

        return map;
    }

    /**
     *
     * @param context A context with a valid unique stream ID and a valid stream type.
     */
    void addStreamContext(StreamContext context) {
        streams[context.getStreamId()] = context;
        streamTypeMap.get(context.getStreamType()).add(context);
    }

    void removeStreamContext(StreamContext context) {
        streams[context.getStreamId()] = null;
        streamTypeMap.get(context.getStreamType()).remove(context);
    }

    public LinkedHashSet<StreamContext> getStreamContextsByType(StreamType streamType) {
        return new LinkedHashSet<>(streamTypeMap.get(streamType));
    }

    public List<StreamContext> getStreamContexts() {
        return Arrays.asList(streams);
    }

    public StreamContext getStreamContextById(int streamId) {
        return streams[streamId];
    }

    public boolean hasStreamContextType(StreamType streamType) {
        return !streamTypeMap.get(streamType).isEmpty();
    }

    public void openStreamCodecs(AVFormatContext formatContext) {
        for (StreamContext streamContext : streams) {
            if (streamContext != null) {
                streamContext.openCodecContext(formatContext);
            }
        }
    }

    /**
     * Releases the native resources held by each stream, leaving the stream contexts themselves in place.
     * <p>
     * The contexts are deliberately retained rather than discarded: everything a context holds beyond its
     * codec state is still valid for the same source, including any {@link DataBufferListener} a client
     * registered on it. Keeping them means a re-opened stream resumes delivering buffers to the same
     * listeners, instead of coming back with every stream unlistened.
     * <p>
     * {@link StreamContext#close()} and {@link StreamContext#openCodecContext(AVFormatContext)} are both
     * idempotent, so the streams in this collection can be cycled closed and open repeatedly.
     */
    public void closeStreams() {
        for (StreamContext streamContext : streams) {
            if (streamContext != null) {
                streamContext.close();
            }
        }
    }

    void processPacket(AVPacket packet) {
        int streamIndex = packet.stream_index();

        // Guard against a packet for a stream this collection does not describe, which can happen if the
        // source came back from a re-open with a different stream layout.
        if (streamIndex < 0 || streamIndex >= streams.length || streams[streamIndex] == null) {
            logger.warn("Dropping packet for unknown stream {}. Re-initialize the driver if the source layout changed.", streamIndex);
            return;
        }

        streams[streamIndex].processPacket(packet);
    }
}
