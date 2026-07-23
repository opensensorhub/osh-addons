package org.sensorhub.impl.sensor.rtmpcam.connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles RTMP (Real-Time Messaging Protocol) connections on a specific port.
 * <p>
 * This class provides functionality to listen for and manage incoming client connections
 * on a specified port. Once started, it creates a server socket to accept connections
 * and delegates each connection to a separate thread for handling.
 * <p>
 * The class is designed to operate as a lightweight and efficient RTMP connection server,
 * capable of handling multiple connections concurrently using a cached thread pool.
 * <p>
 * Lifecycle:
 * 1. The server is initialized with a port and a reference to an RtmpListenerManager.
 * 2. Once started, it begins listening for incoming connections on the specified port.
 * 3. Each accepted client connection is processed using an RtmpConnectionHandler.
 * 4. The server can be gracefully stopped, releasing resources like the server socket
 *    and shutting down the thread pool.
 */
class RtmpPortServer {

    private final int                 port;
    private final RtmpListenerManager manager;
    private final ExecutorService     connectionExecutor;

    private volatile ServerSocket serverSocket;
    private volatile boolean      running;

    RtmpPortServer(int port, RtmpListenerManager manager) {
        this.port    = port;
        this.manager = manager;

        // Each accepted connection gets its own daemon thread from a cached pool.
        // AtomicInteger gives unique names without synchronisation overhead.
        AtomicInteger counter = new AtomicInteger();
        this.connectionExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "rtmp-conn-" + port + "-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    void start() {
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "rtmp-accept-" + port);
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException ignored) {}
        connectionExecutor.shutdown();
    }

    private void acceptLoop() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[RTMP] Listening on port " + port);
            while (running) {
                Socket client = serverSocket.accept();
                connectionExecutor.execute(
                        () -> new RtmpConnectionHandler(client, port, manager).handle());
            }
        } catch (IOException e) {
            if (running) System.err.println("[RTMP] Port " + port + " fault: " + e.getMessage());
        }
    }
}