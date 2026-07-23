package org.sensorhub.impl.sensor.rtmpcam.connection;

import org.sensorhub.impl.sensor.rtmpcam.config.ConnectionConfig;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Manages the registration and lifecycle of RTMP listeners and port servers.
 *
 * This class provides a singleton instance for handling RTMP listeners, ensuring that
 * multiple registrations with the same composite key will overwrite the prior registration.
 * It also manages the mapping between RTMP port servers and their associated listeners,
 * ensuring resources allocated for specific ports are initialized or cleaned up as
 * necessary.
 */
public class RtmpListenerManager {

    private static final RtmpListenerManager INSTANCE = new RtmpListenerManager();

    public static RtmpListenerManager getInstance() { return INSTANCE; }

    private final ConcurrentHashMap<String, RtmpListener> listeners =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Integer, RtmpPortServer> portServers =
            new ConcurrentHashMap<>();

    private final Object portLock = new Object();

    public void addListener(RtmpListener listener) {
        listeners.put(listener.config().compositeKey(), listener);

        synchronized (portLock) {
            portServers.computeIfAbsent(listener.config().port, port -> {
                RtmpPortServer srv = new RtmpPortServer(port, this);
                srv.start();
                return srv;
            });
        }
    }

    public void removeListener(RtmpListener listener) {
        // Two-arg remove: only deletes if the value still matches this exact listener,
        // so a replacement registered under the same key isn't accidentally removed.
        listeners.remove(listener.config().compositeKey(), listener);

        int port = listener.config().port;
        synchronized (portLock) {
            boolean anyRemaining = listeners.values().stream()
                    .anyMatch(l -> l.config().port == port);
            if (!anyRemaining) {
                RtmpPortServer srv = portServers.remove(port);
                if (srv != null) srv.stop();
            }
        }
    }

    Optional<RtmpListener> route(RtmpConnectionContext ctx) {
        return Optional.of(listeners.get(ConnectionConfig.compositeKey(ctx)));
    }
}