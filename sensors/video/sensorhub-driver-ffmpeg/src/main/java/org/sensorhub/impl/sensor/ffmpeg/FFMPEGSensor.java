/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.ffmpeg.common.SyncTime;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.ffmpeg.outputs.Video;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEConstants;
import org.vast.util.Asserts;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Sensor driver that can read video data that is compatible with FFMPEG.
 *
 * @author Drew Botts
 * @since Feb. 2023
 */
public class FFMPEGSensor extends AbstractSensorModule<FFMPEGConfig> {
    /**
     * Debug logger
     */
    private static final Logger logger = LoggerFactory.getLogger(FFMPEGSensor.class.getSimpleName());

    /**
     * Thing that knows how to parse the data out of the bytes from the video stream.
     */
    protected MpegTsProcessor mpegTsProcessor;

    /**
     * Background thread manager. Used for image decoding. At the moment, this is a single thread.
     */
    protected ScheduledExecutorService executor;

    /**
     * Sensor output for the video frames.
     */
    protected Video videoOutput;

    /**
     * Keeps track of the times in the data stream so that we can put an accurate phenomenon time in the data blocks.
     */
    protected SyncTime syncTime;

    /**
     * Lock to prevent simultaneous access to syncTime.
     */
    protected final Object syncTimeLock = new Object();

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        generateUniqueID("urn:osh:sensor:ffmpeg:", config.serialNumber);
        generateXmlID("FFMPEG_", config.serialNumber);

        // Every time we do init we have to tear down the mpegTsProcessor,
        // just in case they changed some setting that might cause the video output to be different.
        if (mpegTsProcessor != null) {
            try {
                mpegTsProcessor.closeStream();
            } catch (Exception e) {
                logger.warn("Could not close MPEG TS processor", e);
            } finally {
                // Regardless of exceptions, go ahead and set it to null.
                // If there were severe problems, we can hope that garbage collection will take care of it eventually.
                mpegTsProcessor = null;
            }
        }
        // We also have to clear out the video output since its settings may have changed
        // (based on having a new input video, for example).
        videoOutput = null;

        // We need the background thread here since we start reading the video data immediately to determine the video size.
        setupExecutor();

        // Open up the stream so that we can get the video output.
        openStream();
    }

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        // Start up the background thread if it's not already going.
        // Normally doInit() will have just been called, so this is redundant (but harmless).
        // But if the user has stopped the sensor and re-started it, then this call is necessary.
        setupExecutor();

        // Make sure the stream is already open.
        // If the sensor has been previously started, then stopped, the stream won't be open.
        openStream();

        // Some preliminary data was read from the stream in doInit(),
        // but this call makes it start processing all the frames.
        startStream();
    }

    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();

        stopStream();
        shutdownExecutor();
    }

    /**
     * Overridden to set the definition of the sensor to <a href="http://www.w3.org/ns/ssn/System">http://www.w3.org/ns/ssn/System</a>
     */
    @Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();
            sensorDescription.setDefinition(SWEConstants.DEF_SYSTEM);
        }
    }

    @Override
    public boolean isConnected() {
        return (mpegTsProcessor != null) && isStarted();
    }

    /**
     * Creates the background thread that'll handle video decoding if it hasn't already been done.
     * Also tells the setDecoder and videoOutput about the executor.
     * This can be called multiple times without causing problems, and that's done on purpose
     * so that the two subclasses could potentially call it at different times in their life cycle.
     */
    protected void setupExecutor() {
        if (executor == null) {
            logger.debug("Executor was null, so creating a new one");
            executor = Executors.newSingleThreadScheduledExecutor();
        } else {
            logger.debug("Already had an executor.");
        }
        if (videoOutput != null) {
            videoOutput.setExecutor(executor);
        }
    }

    /**
     * Cleanly shuts down the background thread and sets it to null.
     * If it's already null, doesn't do anything.
     * This is called when the sensor is stopped to clean up the background thread (hopefully).
     */
    protected void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /**
     * Create and initialize the video output.
     * The caller must be careful not to call this if the video output has already been created and added to the sensor.
     */
    protected void createVideoOutput(int[] videoDims, String codecFormat) {
        videoOutput = new Video(this, videoDims, codecFormat);
        if (executor != null) {
            videoOutput.setExecutor(executor);
        }
        addOutput(videoOutput, false);
        videoOutput.init();
    }

    /**
     * Opens the connection to the upstream source but does not yet start reading any more than the initial
     * metadata necessary to get the video frame size.
     * This can be called multiple times, but will only have any effect the first time it's called after doInit()
     * since it checks for a null mpegTsProcessor.
     * This also has the side effect of creating and adding the Video output if it hasn't already happened earlier.
     */
    protected void openStream() throws SensorHubException {
        if (mpegTsProcessor == null) {
            logger.info("Opening MPEG TS connection for {} ...", getUniqueIdentifier());
            // Initialize the MPEG transport stream processor from the source named in the configuration.
            // If neither the file source nor a connection string is specified,
            // throw an exception so the user knows that they have to provide at least one of them.
            if ((null != config.connection.transportStreamPath) && (!config.connection.transportStreamPath.isBlank())) {
                Asserts.checkArgument(config.connection.fps >= 0, "FPS must be >= 0");
                mpegTsProcessor = new MpegTsProcessor(config.connection.transportStreamPath, config.connection.fps, config.connection.loop);
            } else if ((null != config.connection.connectionString) && (!config.connection.connectionString.isBlank())) {
                mpegTsProcessor = new MpegTsProcessor(config.connection.connectionString);
            } else {
                throw new SensorHubException("Either the input file path or the connection string must be set");
            }

            if (mpegTsProcessor.openStream()) {
                logger.info("Stream opened for {}", getUniqueIdentifier());
                mpegTsProcessor.queryEmbeddedStreams();
                // If there is a video content in the stream
                if (mpegTsProcessor.hasVideoStream()) {
                    // In case we were waiting until we got video data to make the video frame output,
                    // we go ahead and do that now.
                    if (videoOutput == null) {
                        int[] videoDimensions = mpegTsProcessor.getVideoStreamFrameDimensions();
                        String codecFormat = "string";
                        createVideoOutput(videoDimensions, codecFormat);
                    }
                    // Set video stream packet listener to video output
                    mpegTsProcessor.setVideoDataBufferListener(videoOutput);
                }
            } else {
                throw new SensorHubException("Unable to open stream from data source");
            }

            logger.info("MPEG TS stream for {} opened.", getUniqueIdentifier());
        }
    }

    /**
     * This causes the frames of the video to start being processed.
     * If it's from a network stream, that means that data will start flowing across the wire.
     * If it's from a file stream, the frames are read from disk.
     */
    protected void startStream() throws SensorHubException {
        try {
            if (mpegTsProcessor != null) {
                mpegTsProcessor.processStream();
            }
        } catch (IllegalStateException e) {
            String message = "Failed to start stream processor";
            logger.error(message);
            throw new SensorHubException(message, e);
        }
    }

    protected void stopStream() throws SensorHubException {
        logger.info("Stopping MPEG TS processor for {}", getUniqueIdentifier());

        if (null != mpegTsProcessor) {
            mpegTsProcessor.stopProcessingStream();

            try {
                // Wait for thread to finish
                mpegTsProcessor.join();
            } catch (InterruptedException e) {
                logger.error("Interrupted waiting for stream processor to stop", e);
                Thread.currentThread().interrupt();
                throw new SensorHubException("Interrupted waiting for stream processor to stop", e);
            } finally {
                // Close stream and cleanup resources
                mpegTsProcessor.closeStream();
                mpegTsProcessor = null;
            }
        }
    }

    /**
     * Sets a synchronization time element used to synchronize telemetry with video stream data
     *
     * @param syncTime The most recent synchronization data from telemetry stream
     */
    public void setStreamSyncTime(SyncTime syncTime) {
        synchronized (syncTimeLock) {
            this.syncTime = syncTime;
        }
    }

    /**
     * Returns the latest {@link SyncTime} record or null if not yet received
     *
     * @return The latest sync time or null
     */
    public SyncTime getSyncTime() {
        SyncTime currentSyncTime;

        synchronized (syncTimeLock) {
            currentSyncTime = syncTime;
        }

        return currentSyncTime;
    }

    /**
     * Returns the ignoreDataTimestamp flag
     *
     * @return ignoreDataTimestamp
     */
    public Boolean getIgnoreDataTimestamp() {
        return config.connection.ignoreDataTimestamps;
    }

    /**
     * Returns the isMJPEG flag
     *
     * @return isMJPEG
     */
    public Boolean getIsMJPEG() {
        return config.connection.isMJPEG;
    }
}
