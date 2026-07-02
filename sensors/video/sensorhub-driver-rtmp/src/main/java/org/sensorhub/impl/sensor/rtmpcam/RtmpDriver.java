/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtmpcam;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.ffmpeg.outputs.AudioOutput;
import org.sensorhub.impl.sensor.ffmpeg.outputs.VideoOutput;
import org.sensorhub.impl.sensor.rtmpcam.config.ConnectionConfig;
import org.sensorhub.impl.sensor.rtmpcam.config.RtmpConfig;
import org.sensorhub.impl.sensor.rtmpcam.connection.RtmpListener;
import org.sensorhub.impl.sensor.rtmpcam.connection.RtmpListenerManager;
import org.sensorhub.impl.sensor.rtmpcam.event.RtmpConnectEvent;
import org.sensorhub.impl.sensor.rtmpcam.event.RtmpDisconnectEvent;
import org.sensorhub.impl.sensor.rtmpcam.event.RtmpReconnectEvent;
import org.sensorhub.impl.sensor.rtmpcam.event.RtmpStreamEvent;
import org.sensorhub.mpegts.MpegTsProcessor;

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
 * is tracked through a shared {@link RtmpListenerManager}.
 * </p>
 */
public class RtmpDriver extends AbstractSensorModule<RtmpConfig> implements RtmpListener {
    private static final String COMMAND_LINE_ARGS = "-timeout 0 -listen 1 -username test -password test";
    private static final int EXECUTOR_JOIN_TIMEOUT = 10;
    private static final TimeUnit EXECUTOR_JOIN_TIME_UNIT = TimeUnit.SECONDS;
    private static final int HEARTBEAT_INTERVAL = 5;
    private static final TimeUnit HEARTBEAT_TIME_UNIT = TimeUnit.SECONDS;
    private static final int MAX_STARTUP_WAIT_TIME_MS = 5000;

    private volatile boolean doStreamProcessing = false;
    private final AtomicReference<MpegTsProcessor> mpegTsProcessor = new AtomicReference<>();
    private final RtmpListenerManager rtmpListenerManager = RtmpListenerManager.getInstance();
    private ExecutorService executorService;
    private ExecutorService videoExecutorService = Executors.newSingleThreadExecutor();
    private ExecutorService audioExecutorService = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService heartbeatExecutorService;

    // TODO: Create a DataOutput
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
        if (config.connectionConfig.generateRandomKey) {
            config.connectionConfig.streamKey = generateStreamKey();
            config.connectionConfig.generateRandomKey = false;
        }

        rtmpListenerManager.removeListener(this);
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();
        rtmpListenerManager.addListener(this);
        doStreamProcessing = true;
    }

    private static String generateStreamKey() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public boolean doStreamProcessing() {
        return doStreamProcessing;
    }


    /**
     * Stops the driver and releases all RTMP stream resources.
     *
     * @throws SensorHubException if shutdown fails
     */
    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
        doStreamProcessing = false;
        rtmpListenerManager.removeListener(this);
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

    @Override
    public ConnectionConfig config() {
        return config.connectionConfig;
    }

    @Override
    public void onConnected(RtmpConnectEvent event) {
        reportStatus("Connected to: " + connectionUrl);
    }

    @Override
    public void onStreamConnected(RtmpStreamEvent event) {
        var streamInfo = event.getPayload();

        if (streamInfo == null) {
            logger.warn("StreamInfo is null");
            return;
        }
        if (streamInfo.videoCodec() != null) {
            videoOutput.set(new VideoOutput<>(this, streamInfo.videoDimensions(), streamInfo.videoCodec()));
            videoExecutorService = Executors.newSingleThreadExecutor();
            videoOutput.get().setExecutor(videoExecutorService);
            videoOutput.get().doInit();
            addOutput(videoOutput.get(), false);

        }
        if (streamInfo.audioCodec() != null) {
            audioOutput.set(new AudioOutput<>(this, streamInfo.audioSampleRate(), streamInfo.audioCodec()));
            audioExecutorService = Executors.newSingleThreadExecutor();
            audioOutput.get().setExecutor(audioExecutorService);
            audioOutput.get().doInit();
            addOutput(audioOutput.get(), false);
        }
    }

    @Override
    public void onDisconnected(RtmpDisconnectEvent event) {
        removeAllOutputs();
        reportStatus("Disconnected from: " + connectionUrl);
    }

    @Override
    public void onReconnected(RtmpReconnectEvent event) {

    }

    @Override
    public VideoOutput<?> getVideoOutput() {
        return this.videoOutput.get();
    }

    @Override
    public AudioOutput<?> getAudioOutput() {
        return this.audioOutput.get();
    }
}