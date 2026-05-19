/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtmp;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.ffmpeg.outputs.AudioOutput;
import org.sensorhub.impl.sensor.ffmpeg.outputs.VideoOutput;
import org.sensorhub.impl.sensor.rtmp.config.RtmpConfig;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.sensorhub.utils.Async;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.bytedeco.ffmpeg.global.avutil.av_log_set_callback;

/**
 * OpenSensorHub sensor module that listens for and processes RTMP streams.
 * <p>
 * The driver creates an FFmpeg-backed MPEG-TS processor configured as an RTMP
 * listener. Once a publisher connects, the driver detects available audio and
 * video streams, creates matching OpenSensorHub outputs, and forwards stream
 * data through those outputs.
 * </p>
 * <p>
 * Only one RTMP driver instance may use a given port at a time. Port ownership
 * is tracked through a shared {@link RtmpPortSingleton}.
 * </p>
 */
public class RtmpDriver extends AbstractSensorModule<RtmpConfig> {
    private static final String COMMAND_LINE_ARGS = "-timeout 0 -listen 1 -username test -password test";
    private static final int EXECUTOR_JOIN_TIMEOUT = 10;
    private static final TimeUnit EXECUTOR_JOIN_TIME_UNIT = TimeUnit.SECONDS;
    private static final int HEARTBEAT_INTERVAL = 5;
    private static final TimeUnit HEARTBEAT_TIME_UNIT = TimeUnit.SECONDS;
    private static final int MAX_STARTUP_WAIT_TIME_MS = 5000;

    private final RtmpPortSingleton portSingleton = RtmpPortSingleton.getInstance();
    private ExecutorService executorService;
    private ExecutorService videoExecutorService;
    private ExecutorService audioExecutorService;
    private ScheduledExecutorService heartbeatExecutorService;

    final AtomicReference<MpegTsProcessor> mpegTsProcessor = new AtomicReference<>();
    final AtomicReference<VideoOutput<RtmpDriver>> videoOutput = new AtomicReference<>();
    final AtomicReference<AudioOutput<RtmpDriver>> audioOutput = new AtomicReference<>();

    int connectionPort = -1;
    String connectionUrl = "";
    //String path = "";

    /**
     * Indicates whether the driver has successfully connected to an RTMP stream at least once since starting.
     */
    volatile boolean hasConnected = false;

    /**
     * Indicates whether the driver is currently connected to an RTMP stream.
     */
    volatile boolean isConnected = false;

    /**
     * Initializes the driver configuration and generated identifiers.
     * <p>
     * If no unique identifier has been assigned, this method generates both the
     * OpenSensorHub unique identifier and XML identifier from the configured
     * serial number. It also releases any previously tracked port and rebuilds
     * the RTMP listener URL from the current configuration.
     * </p>
     *
     * @throws SensorHubException if initialization fails
     */
    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        if (getUniqueIdentifier() == null) {
            generateUniqueID("urn:osh:sensor:rtmp:", config.serialNumber);
            generateXmlID("RTMP_", config.serialNumber);
        }

        portSingleton.removeConnection(connectionPort);

        setConnectionUrl();

        //createMpegTsProcessor();
    }

    private static String generateStreamKey() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Creates and stores the FFmpeg-backed MPEG-TS processor for the configured
     * RTMP listener URL.
     */
    private void createMpegTsProcessor() {
        var mpegts = new MpegTsProcessor(connectionUrl, COMMAND_LINE_ARGS/* + " -rtmp_app /live -rtmp_playpath " + path*/);
        mpegts.setInjectVideoExtradata(true);
        mpegTsProcessor.set(mpegts);
    }

    /**
     * Builds the RTMP listener URL from the configured host and port.
     *
     * @throws SensorException if the connection configuration is invalid
     */
    private void setConnectionUrl() throws SensorException {

        var connectionConfig = config.connectionConfig;
        StringBuilder sb = new StringBuilder("rtmp://");


        /*
        if (connectionConfig.host == HostType.OVERRIDE) {
            if (connectionConfig.hostOverride == null || connectionConfig.hostOverride.isBlank()) {
                throw new SensorException("Domain override is not set");
            }
            sb.append(connectionConfig.hostOverride);
        } else {
            sb.append(connectionConfig.host.host);
        }

         */

        sb.append(connectionConfig.host.host);

        sb.append(":").append(connectionConfig.port);

        /*
        if (connectionConfig.generateRandomStreamKey) {
            connectionConfig.generateRandomStreamKey = false;
            String streamKey = generateStreamKey();
            if (!connectionConfig.path.isBlank() && !connectionConfig.path.endsWith("/")) {
                connectionConfig.path += "/";
            }
            connectionConfig.path += streamKey;
        }

        if (connectionConfig.path != null && !connectionConfig.path.isBlank()) {
            if (!connectionConfig.path.startsWith("/")) {
                connectionConfig.path = "/" + connectionConfig.path;
            }
            sb.append(connectionConfig.path);
        }

        path = connectionConfig.path;

         */
        connectionUrl = sb.toString();
        connectionPort = connectionConfig.port;
    }

    /**
     * Starts the driver by reserving the configured RTMP port.
     *
     * @throws SensorHubException if the configured port is already in use by
     * another RTMP driver module
     */
    @Override
    protected void doStart() throws SensorHubException {
        String moduleUid;
        if ((moduleUid = portSingleton.addConnection(connectionPort, this.getUniqueIdentifier())) != null) {
            throw new SensorException("Port "+ connectionPort + " already in use by module: " + moduleUid);
        }
    }

    /**
     * Performs post-start setup for RTMP listening and heartbeat monitoring.
     * <p>
     * This method creates a fresh MPEG-TS processor, stops any previous executor
     * services, reports the listening URL, starts the stream listener thread, and
     * schedules periodic heartbeat checks.
     * </p>
     *
     * @throws SensorHubException if executor shutdown is interrupted
     */
    @Override
    protected void afterStart() throws SensorHubException {
        super.afterStart();
        //stopStream();
        hasConnected = false;

        synchronized (mpegTsProcessor) {
            var mpegts = mpegTsProcessor.get();
            if (mpegts != null && mpegts.getState() != Thread.State.NEW) {
                stopStream();
            }
            createMpegTsProcessor();
        }

        try {
            stopExecutors();
        } catch (InterruptedException e) {
            throw new SensorHubException("Interrupted while stopping executors", e);
        }

        reportStatus("Listening on: " + connectionUrl);
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::startStream);
        heartbeatExecutorService = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutorService.scheduleAtFixedRate(this::heartbeat, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, HEARTBEAT_TIME_UNIT);
        heartbeatExecutorService.submit(this::heartbeat);
    }

    /**
     * Determines whether the module is currently stopping or stopped.
     *
     * @return {@code true} if the module is stopping or stopped; otherwise {@code false}
     */
    private boolean isStopping () {
        return getCurrentState() == ModuleEvent.ModuleState.STOPPING || getCurrentState() == ModuleEvent.ModuleState.STOPPED;
    }

    /*
    private boolean isMatchingPath(String url) {
        boolean isMatching = url != null && url.trim().contains(path);
        if (!isMatching) {
            logger.warn("Received stream on: {} but expected: {}", url, config.connectionConfig.path);
        }
        return isMatching;
    }

     */

    /**
     * Opens the RTMP listener, waits for an incoming stream, creates audio/video
     * outputs for detected streams, and begins processing stream data.
     * <p>
     * The method waits until the module reaches the {@code STARTED} state before
     * adding outputs. If the stream cannot be opened, an error is reported and
     * processing is not started.
     * </p>
     */
    private void startStream() {
        // Need to wait for STARTED state so that outputs can be added
        try {
            Async.waitForCondition(() -> getCurrentState() == ModuleEvent.ModuleState.STARTED, MAX_STARTUP_WAIT_TIME_MS);
        } catch (TimeoutException e) {
            reportError("Failed to start stream; timed out waiting for startup", e);
            return;
        }

        boolean status;

        var mpegts = mpegTsProcessor.get();

        if (mpegts == null) {
            logger.error("Could not start; stream processor is null");
            return;
        }

        /*
        // Reject connections that don't match the configured path
        // Thread will sit here until a matching connection is made
        do {
            if (Thread.currentThread().isInterrupted() || isStopping()) {
                return;
            }
            mpegts.closeStream();
            status = mpegts.openStream();
            String path = mpegts.getPrivDataString("rtmp_app") + "/" + mpegts.getPrivDataString("rtmp_playpath");
        } while (!isMatchingPath(path));

         */

        mpegts.closeStream();
        status = mpegts.openStream();

        if (isStopping()) {
            return;
        }

        if (!status) {
            String error = "Failed to connect to " + connectionUrl;
            reportError(error, new SensorException(error));
            return;
        }

        synchronized (mpegTsProcessor) {
            mpegts = mpegTsProcessor.get();

            if (mpegts == null) {
                reportError("Stream could not be opened", new SensorException("MpegTs processor is null"));
                return;
            }

            if (mpegts.isStreamOpened()) {
                if (mpegts.hasVideoStream()) {
                    createVideoOutput(mpegts.getVideoStreamFrameDimensions(), mpegts.getVideoCodecName());
                    mpegts.setVideoDataBufferListener(videoOutput.get());
                }

                if (mpegts.hasAudioStream()) {
                    createAudioOutput(mpegts.getAudioSampleRate(), mpegts.getAudioCodecName());
                    mpegts.setAudioDataBufferListener(audioOutput.get());
                }

            } else {
                reportError("Stream could not be opened", new SensorException("RTMP stream connected but not opened"));
                return;
            }
            clearStatus();
            reportStatus("RTMP stream for " + connectionUrl + " opened.");
            hasConnected = true;
            isConnected = true;
            mpegts.processStream();
            /*
            executorService.submit(() -> {
                MpegTsProcessor processor;
                while ((processor = mpegTsProcessor.get()) != null) {

                    while (processor.isStreamOpened()) {
                        processor.processP();
                    }
                    if (!Thread.currentThread().isInterrupted()) {
                        reportStatus("RTMP stream " + connectionUrl + " lost connection. Reconnecting...");
                        processor.openStream();
                    } else {
                        return;
                    }
                }
                reportStatus("RTMP stream closed.");

            });

             */
            //mpegts.processStream();
        }
    }

    /**
     * Checks the active stream and attempts to reconnect if a previously
     * connected stream has been lost.
     */
    private void heartbeat() {
        var mpegts = mpegTsProcessor.get();
        if (mpegts == null || !hasConnected) { return; }

        if (!mpegts.isStreamOpened()) {
            reportStatus("RTMP stream " + connectionUrl + " lost connection. Reconnecting...");
            isConnected = false;
            createMpegTsProcessor();
            startStream();
        }
    }

    /**
     * Creates and registers the video output for the detected RTMP video stream.
     *
     * @param videoDims video frame dimensions, usually width and height
     * @param codecName name of the detected video codec
     */
    protected void createVideoOutput(int[] videoDims, String codecName) {
        synchronized (videoOutput) {
            var videoOut = new VideoOutput<>(this, videoDims, codecName);
            videoOutput.set(videoOut);

            if (videoExecutorService != null) {
                videoExecutorService.shutdown();
            }

            videoExecutorService = Executors.newSingleThreadExecutor();
            videoOut.setExecutor(videoExecutorService);
            videoOut.doInit();
            addOutput(videoOut, false);
        }
    }

    /**
     * Creates and registers the audio output for the detected RTMP audio stream.
     *
     * @param sampleRate detected audio sample rate in hertz
     * @param codecName name of the detected audio codec
     */
    protected void createAudioOutput(int sampleRate, String codecName) {
        synchronized (audioOutput) {
            var audioOut = new AudioOutput<>(this, sampleRate, codecName);
            audioOutput.set(audioOut);

            if (audioExecutorService != null) {
                audioExecutorService.shutdown();
            }
            audioExecutorService = Executors.newSingleThreadExecutor();
            audioOut.setExecutor(audioExecutorService);
            audioOut.doInit();
            addOutput(audioOut, false);
        }
    }

    /**
     * Stops the driver and releases all RTMP stream resources.
     *
     * @throws SensorHubException if shutdown fails
     */
    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
        shutdown();
    }

    /**
     * Releases the reserved port, stops the stream, and terminates executor services.
     *
     * @throws SensorHubException if executor shutdown is interrupted
     */
    private void shutdown() throws SensorHubException {
        portSingleton.removeConnection(config.connectionConfig.port);
        stopStream();
        try {
            stopExecutors();
        } catch (InterruptedException e) {
            throw new SensorHubException("Interrupted while stopping executors", e);
        }

    }

    /**
     * Stops stream processing, waits for the MPEG-TS processor thread to finish,
     * closes the stream, and clears the processor reference.
     */
    private void stopStream() {
        isConnected = false;
        synchronized (mpegTsProcessor) {
            var mpegts = mpegTsProcessor.get();
            if (mpegts != null) {
                mpegts.stopProcessingStream();
                if (mpegts.isAlive()) {
                    try {
                        logger.info("Waiting for stream to stop.");
                        mpegts.join();
                    } catch (InterruptedException e) {
                        logger.error("Interrupted while waiting for stream to stop.", e);
                    }
                }
                mpegts.closeStream();
            }
            mpegTsProcessor.set(null);
        }
    }

    /**
     * Stops all executor services used by the driver and waits for termination.
     *
     * @throws InterruptedException if interrupted while waiting for executor termination
     */
    private void stopExecutors() throws InterruptedException {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService.awaitTermination(EXECUTOR_JOIN_TIMEOUT, EXECUTOR_JOIN_TIME_UNIT);
        }
        if (videoExecutorService != null) {
            videoExecutorService.shutdownNow();
            videoExecutorService.awaitTermination(EXECUTOR_JOIN_TIMEOUT, EXECUTOR_JOIN_TIME_UNIT);
        }
        if (audioExecutorService != null) {
            audioExecutorService.shutdownNow();
            audioExecutorService.awaitTermination(EXECUTOR_JOIN_TIMEOUT, EXECUTOR_JOIN_TIME_UNIT);
        }
        if (heartbeatExecutorService != null) {
            heartbeatExecutorService.shutdownNow();
            heartbeatExecutorService.awaitTermination(EXECUTOR_JOIN_TIMEOUT, EXECUTOR_JOIN_TIME_UNIT);
        }
    }

    /**
     * Cleans up module resources before disposal.
     *
     * @throws SensorHubException if cleanup or shutdown fails
     */
    @Override
    public void cleanup() throws SensorHubException {
        super.cleanup();
        shutdown();
    }

    /**
     * Indicates whether the driver is currently started and has an open RTMP stream.
     *
     * @return {@code true} if the module is started and the RTMP stream is open;
     * otherwise {@code false}
     */
    @Override
    public boolean isConnected() {
        return isConnected;
    }
}