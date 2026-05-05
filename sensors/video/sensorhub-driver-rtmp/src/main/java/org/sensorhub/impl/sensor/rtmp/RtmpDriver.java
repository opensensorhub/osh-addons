package org.sensorhub.impl.sensor.rtmp;
import org.bytedeco.ffmpeg.avutil.Callback_Pointer_int_BytePointer_Pointer;
import org.bytedeco.ffmpeg.avutil.Callback_Pointer_int_String_Pointer;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.ffmpeg.outputs.AudioOutput;
import org.sensorhub.impl.sensor.ffmpeg.outputs.VideoOutput;
import org.sensorhub.impl.sensor.rtmp.config.HostType;
import org.sensorhub.impl.sensor.rtmp.config.RtmpConfig;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.sensorhub.utils.Async;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_WARNING;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_callback;

public class RtmpDriver extends AbstractSensorModule<RtmpConfig> {
    private static final String COMMAND_LINE_ARGS = "-timeout 0 -listen 1 -username test -password test";
    private static final int EXECUTOR_JOIN_TIMEOUT = 10;
    private static final TimeUnit EXECUTOR_JOIN_TIME_UNIT = TimeUnit.SECONDS;
    private static final int HEARTBEAT_INTERVAL = 5;
    private static final TimeUnit HEARTBEAT_TIME_UNIT = TimeUnit.SECONDS;
    private static final int MAX_STARTUP_WAIT_TIME_MS = 5000;

    private static final RtmpPortArbiter portArbiter = new RtmpPortArbiter();
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
    volatile boolean hasConnected = false;


    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        if (getUniqueIdentifier() == null) {
            generateUniqueID("urn:osh:sensor:rtmp:", config.serialNumber);
            generateXmlID("RTMP_", config.serialNumber);
        }

        portArbiter.removeConnection(connectionPort);

        setConnectionUrl();

        //createMpegTsProcessor();
    }

    private static String generateStreamKey() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private void createMpegTsProcessor() {
        var mpegts = new MpegTsProcessor(connectionUrl, COMMAND_LINE_ARGS/* + " -rtmp_app /live -rtmp_playpath " + path*/);
        mpegts.setInjectVideoExtradata(true);
        mpegTsProcessor.set(mpegts);
    }

    private void setConnectionUrl() throws SensorException {

        var connectionConfig = config.connectionConfig;
        StringBuilder sb = new StringBuilder("rtmp://");


        if (connectionConfig.host == HostType.OVERRIDE) {
            if (connectionConfig.hostOverride == null || connectionConfig.hostOverride.isBlank()) {
                throw new SensorException("Domain override is not set");
            }
            sb.append(connectionConfig.hostOverride);
        } else {
            sb.append(connectionConfig.host.host);
        }

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

    @Override
    protected void doStart() throws SensorHubException {
        String moduleUid;
        if ((moduleUid = portArbiter.addConnection(connectionPort, this.getUniqueIdentifier())) != null) {
            throw new SensorException("Port "+ connectionPort + " already in use by module: " + moduleUid);
        }
    }


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

    private void heartbeat() {
        var mpegts = mpegTsProcessor.get();
        if (mpegts == null || !hasConnected) { return; }

        if (!mpegts.isStreamOpened()) {
            reportStatus("RTMP stream " + connectionUrl + " lost connection. Reconnecting...");
            createMpegTsProcessor();
            startStream();
        }
    }

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


    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
        shutdown();
    }

    private void shutdown() throws SensorHubException {
        portArbiter.removeConnection(config.connectionConfig.port);
        stopStream();
        try {
            stopExecutors();
        } catch (InterruptedException e) {
            throw new SensorHubException("Interrupted while stopping executors", e);
        }

    }

    private void stopStream() {
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


    @Override
    public void cleanup() throws SensorHubException {
        super.cleanup();
        shutdown();
    }

    @Override
    public boolean isConnected() {
        return isStarted() && mpegTsProcessor.get() != null && mpegTsProcessor.get().isStreamOpened();
    }
}