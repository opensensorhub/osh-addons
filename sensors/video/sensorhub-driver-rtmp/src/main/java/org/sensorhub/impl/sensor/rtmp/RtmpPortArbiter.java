package org.sensorhub.impl.sensor.rtmp;

import java.util.HashMap;
import java.util.Map;

public class RtmpPortArbiter {
    private final Map<Integer, String> urls = new HashMap<>();

    // If successful, returns null, otherwise returns the moduleUid of the existing connection
    public synchronized String addConnection(int url, String moduleUid) {
        if (urls.containsKey(url)) {
            return urls.get(url);
        } else {
            urls.put(url, moduleUid);
            return null;
        }
    }

    public synchronized void removeConnection(int url) {
        urls.remove(url);
    }
}
