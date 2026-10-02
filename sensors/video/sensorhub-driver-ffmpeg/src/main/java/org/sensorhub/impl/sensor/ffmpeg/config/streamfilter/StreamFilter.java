package org.sensorhub.impl.sensor.ffmpeg.config.streamfilter;

import org.sensorhub.mpegts.StreamCollection;
import org.sensorhub.mpegts.StreamContext;

import java.util.HashSet;
import java.util.Set;

public abstract class StreamFilter {
    public abstract boolean filter(StreamContext stream);

    public abstract void resetState();

    public Set<StreamContext> getFilteredStreams(StreamCollection streams) {
        resetState();

        Set<StreamContext> contexts = new HashSet<>();
        for (StreamContext context : streams.getStreamContexts()) {
            if (filter(context)) {
                contexts.add(context);
            }
        }
        return contexts;
    }
}
