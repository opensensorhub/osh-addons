/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.axis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.ffmpeg.outputs.VideoOutput;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Per-stream adapter that wires an {@link MpegTsProcessor} to a
 * {@link VideoOutput} for a single Axis video URL (MJPEG over HTTP,
 * or H.264 / H.265 over RTSP).
 * </p>
 *
 * <p>
 * Each instance owns its own single-threaded decode executor and its own
 * {@link MpegTsProcessor}. Multiple instances can coexist in the same
 * Axis driver (e.g. one MJPEG + one H.264 output).
 * </p>
 */
class AxisVideoStream {
    private static final Logger logger = LoggerFactory.getLogger(AxisVideoStream.class);

    private final AxisCameraDriver parent;
    private final String outputName;
    private final String sourceUrl;
    private final String commandLineArgs;

    private MpegTsProcessor mpegTsProcessor;
    private VideoOutput<AxisCameraDriver> videoOutput;
    private ExecutorService executor;

    /**
     * @param parent            Axis driver that owns this stream.
     * @param outputName        Name under which the resulting video output will be registered.
     * @param sourceUrl         FFmpeg-compatible source URL (http://..., rtsp://...).
     * @param commandLineArgs   Optional FFmpeg command-line-style options (e.g. "-timeout 3000000",
     *                          or "-rtsp_transport tcp -stimeout 3000000"). May be {@code null}.
     */
    AxisVideoStream(AxisCameraDriver parent, String outputName, String sourceUrl, String commandLineArgs) {
        this.parent = parent;
        this.outputName = outputName;
        this.sourceUrl = sourceUrl;
        this.commandLineArgs = commandLineArgs == null ? "" : commandLineArgs;
    }

    /**
     * Opens the remote stream, discovers its frame dimensions and codec, and
     * creates + initializes the matching {@link VideoOutput}. The caller is
     * responsible for calling {@code addOutput(getOutput(), false)} on the
     * parent driver after this returns.
     */
    void init() throws SensorException {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "axis-ffmpeg-" + outputName);
            t.setDaemon(true);
            return t;
        });

        mpegTsProcessor = new MpegTsProcessor(sourceUrl, commandLineArgs);
        // Live sources need extradata re-injection for late-join decoders
        // (browser / CDS clients connecting after the first keyframe).
        mpegTsProcessor.setInjectVideoExtradata(true);

        if (!mpegTsProcessor.openStream()) {
            shutdownExecutorQuietly();
            throw new SensorException("Could not open FFmpeg stream: " + sourceUrl);
        }

        if (!mpegTsProcessor.hasVideoStream()) {
            closeStreamQuietly();
            shutdownExecutorQuietly();
            throw new SensorException("No video track found in stream: " + sourceUrl);
        }

        int[] dims = mpegTsProcessor.getVideoStreamFrameDimensions();
        String codecName = mpegTsProcessor.getVideoCodecName();
        videoOutput = new VideoOutput<>(parent, dims, codecName, outputName, "Video", "Video stream via FFmpeg (" + codecName + ")");
        videoOutput.setExecutor(executor);
        videoOutput.doInit();

        mpegTsProcessor.setVideoDataBufferListener(videoOutput);
    }

    /**
     * The video output created by {@link #init()}. Returns {@code null} if
     * {@code init()} was not called (or threw).
     */
    VideoOutput<AxisCameraDriver> getOutput() {
        return videoOutput;
    }

    /**
     * Starts pulling frames from the remote stream into the video output.
     * Safe to call only after {@link #init()} has succeeded.
     */
    void start() throws SensorException {
        if (mpegTsProcessor == null)
            throw new SensorException("Stream has not been initialized");

        try {
            mpegTsProcessor.processStream();
            mpegTsProcessor.setReconnect(true);
        } catch (IllegalStateException e) {
            throw new SensorException("Failed to start FFmpeg stream: " + sourceUrl, e);
        }
    }

    /**
     * Stops frame delivery and releases native resources. Safe to call multiple
     * times and safe to call before {@link #init()}.
     */
    void stop() {
        if (mpegTsProcessor != null) {
            try {
                mpegTsProcessor.stopProcessingStream();
                mpegTsProcessor.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while stopping FFmpeg stream {}", outputName, e);
            } finally {
                closeStreamQuietly();
            }
        }
        shutdownExecutorQuietly();
    }

    private void closeStreamQuietly() {
        try {
            if (mpegTsProcessor != null)
                mpegTsProcessor.closeStream();
        } catch (Exception e) {
            logger.warn("Error closing FFmpeg stream {}", outputName, e);
        } finally {
            mpegTsProcessor = null;
        }
    }

    private void shutdownExecutorQuietly() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
