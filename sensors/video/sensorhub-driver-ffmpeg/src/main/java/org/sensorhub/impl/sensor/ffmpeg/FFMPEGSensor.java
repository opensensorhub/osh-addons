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
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.ffmpeg.config.streamfilter.MaxCountFilter;
import org.sensorhub.impl.sensor.ffmpeg.outputs.AudioOutput;
import org.sensorhub.impl.sensor.ffmpeg.outputs.DataOutput;
import org.sensorhub.impl.sensor.ffmpeg.outputs.VideoOutput;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.sensorhub.mpegts.StreamContext;
import org.vast.swe.SWEConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Sensor driver that can read video data that is compatible with FFmpeg.
 */
public class FFMPEGSensor extends AbstractSensorModule<FFMPEGConfig> {
    /**
     * Thing that knows how to parse the data out of the bytes from the video stream.
     */
    protected MpegTsProcessor mpegTsProcessor;

    /**
     * Background thread manager. Used for image decoding. At the moment, this is a single thread.
     */
    protected ScheduledExecutorService executor;

    /**
     * Sensor output for the video frames of the first video stream of the source.
     * The outputs for any further video streams are in {@link FFMPEGSensor#videoOutputs}.
     */
    protected VideoOutput<FFMPEGSensor> videoOutput;

    /**
     * Sensor output for the audio data of the first audio stream of the source.
     * The outputs for any further audio streams are in {@link FFMPEGSensor#audioOutputs}.
     */
    protected AudioOutput<FFMPEGSensor> audioOutput;

    /**
     * Sensor output for the raw bytes of the first binary data stream of the source.
     * The outputs for any further data streams are in {@link FFMPEGSensor#dataOutputs}.
     */
    protected DataOutput<FFMPEGSensor> dataOutput;

    /**
     * All video outputs, ordered by stream ID. The first entry is also held in {@link FFMPEGSensor#videoOutput}.
     */
    protected final List<VideoOutput<FFMPEGSensor>> videoOutputs = new ArrayList<>();

    /**
     * All audio outputs, ordered by stream ID. The first entry is also held in {@link FFMPEGSensor#audioOutput}.
     */
    protected final List<AudioOutput<FFMPEGSensor>> audioOutputs = new ArrayList<>();

    /**
     * All binary data outputs, ordered by stream ID. The first entry is also held in {@link FFMPEGSensor#dataOutput}.
     */
    protected final List<DataOutput<FFMPEGSensor>> dataOutputs = new ArrayList<>();

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();
        clearStatus();
        logger.info("Initializing FFMPEG sensor for {}", getUniqueIdentifier());

        generateUniqueID("urn:osh:sensor:ffmpeg:", config.serialNumber);
        generateXmlID("FFMPEG_", config.serialNumber);

        if (config.connection.fps < 0)
            throw new SensorHubException("FPS must be a positive value");

        if (config.connection.streamFilter == null) {
            logger.warn("No stream filter specified, using default");
            config.connection.streamFilter = new MaxCountFilter();
        }

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

        // We also have to clear out the outputs since their settings may have changed
        // (based on having a new input video, for example).
        videoOutput = null;
        audioOutput = null;
        dataOutput = null;
        videoOutputs.clear();
        audioOutputs.clear();
        dataOutputs.clear();

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

        videoOutputs.forEach(output -> output.setExecutor(executor));
        audioOutputs.forEach(output -> output.setExecutor(executor));
        dataOutputs.forEach(output -> output.setExecutor(executor));
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
    protected void createVideoOutput(int[] videoDims, String codecName) {
        int index = videoOutputs.size();
        String name = outputName("video", index);

        var output = new VideoOutput<FFMPEGSensor>(this, videoDims, codecName, name,
                outputLabel("Video", index), "Video stream using ffmpeg library");

        if (executor != null) {
            output.setExecutor(executor);
        }
        addOutput(output, false);
        output.doInit();
        videoOutputs.add(output);

        // Keep the field pointing at the primary video output
        if (videoOutput == null)
            videoOutput = output;
    }

    /**
     * Create and initialize the audio output.
     * The caller must be careful not to call this if the audio output has already been created and added to the sensor.
     */
    protected void createAudioOutput(int sampleRate, String codecName) {
        int index = audioOutputs.size();
        String name = outputName("audio", index);

        var output = new AudioOutput<FFMPEGSensor>(this, sampleRate, codecName, name,
                outputLabel("Audio", index), "Audio stream using ffmpeg library");

        if (executor != null) {
            output.setExecutor(executor);
        }
        addOutput(output, false);
        output.doInit();
        audioOutputs.add(output);

        // Keep the field pointing at the primary audio output
        if (audioOutput == null)
            audioOutput = output;
    }

    /**
     * Create and initialize the binary data output.
     * The caller must be careful not to call this if the data output has already been created and added to the sensor.
     */
    protected void createDataOutput() {
        int index = dataOutputs.size();
        String name = outputName("data", index);

        var output = new DataOutput<FFMPEGSensor>(this, name,
                outputLabel("Data", index), "Raw data stream using ffmpeg library");

        if (executor != null) {
            output.setExecutor(executor);
        }
        addOutput(output, false);
        output.doInit();
        dataOutputs.add(output);

        // Keep the field pointing at the primary data output
        if (dataOutput == null)
            dataOutput = output;
    }

    /**
     * Opens the connection to the upstream source but does not yet start reading any more than the initial
     * metadata necessary to get the video frame size.
     * This can be called multiple times, but will only have any effect the first time it's called after doInit()
     * since it checks for a null mpegTsProcessor.
     * This also has the side effect of creating and adding the Video output if it hasn't already happened earlier.
     *
     * @return {@code true} if the stream was opened or already open, {@code false} otherwise.
     */
    protected boolean openStream() {
        if (mpegTsProcessor == null) {
            logger.info("Opening MPEG TS connection.");

            // Regex to determine if the connection string is a file path.
            String fileRegex = "^(?:[a-zA-Z]:)?[\\\\/].*";

            // For files, the FPS and loop settings are used to control playback.
            if (config.connection.connectionString != null && config.connection.connectionString.matches(fileRegex)) {
                logger.info("Opening file stream with FPS: {} and loop: {}", config.connection.fps, config.connection.loop);
                mpegTsProcessor = new MpegTsProcessor(config.connection.connectionString, config.connection.commandLineArgs, config.connection.fps, config.connection.loop);
            } else {
                logger.info("Opening network stream");
                mpegTsProcessor = new MpegTsProcessor(config.connection.connectionString, config.connection.commandLineArgs);
            }
            mpegTsProcessor.setInjectVideoExtradata(config.connection.injectExtradata);
            mpegTsProcessor.registerDevices(config.connection.registerDevices);
        }

        if (mpegTsProcessor.isStreamOpened()) {
            logger.info("Stream already opened.");
            return true;
        }

        // Initialize the MPEG transport stream processor from the source named in the configuration.
        if (mpegTsProcessor.openStream()) {
            initializeOutputs();
            logger.info("MPEG TS stream for {} opened.", getUniqueIdentifier());
            return true;
        }

        return false;
    }

    /**
     * Called after the stream is opened.
     * Initialize outputs based on the stream contents.
     * <p>
     * An output is created for every video, audio, and binary data stream the source carries,
     * filtered by {@link org.sensorhub.impl.sensor.ffmpeg.config.Connection#streamFilter}, and each output is
     * registered as the listener for its own stream.
     * <p>
     * Outputs are reused across stop/start cycles: on restart the stream contexts are rebuilt by the stream
     * processor, so the listeners have to be re-registered, but the outputs themselves stay valid.
     * <br>Override this method for custom output handling. (Custom data stream output, etc.)
     */
    protected void initializeOutputs() {
        int videoCount = 0;
        int audioCount = 0;
        int dataCount = 0;
        var streamFilter = config.connection.streamFilter;

        for (StreamContext streamContext : streamFilter.getFilteredStreams(mpegTsProcessor.getStreamCollection())) {
            switch (streamContext.getStreamType()) {
                case VIDEO -> {
                    attachVideoOutput(streamContext, videoCount++);
                }
                case AUDIO -> {
                    attachAudioOutput(streamContext, audioCount++);
                }
                case DATA -> {
                    attachDataOutput(streamContext, dataCount++);
                }
                default -> logger.debug("Ignoring stream {} of type {}",
                        streamContext.getStreamId(), streamContext.getStreamType());
            }
        }

        logger.info("Initialized {} video, {} audio, and {} binary data output(s) for {}",
                videoCount, audioCount, dataCount, getUniqueIdentifier());

        if (videoCount == 0 && audioCount == 0 && dataCount == 0)
            reportStatus("No video, audio, or binary data streams published from " + config.connection.connectionString);
    }

    /**
     * @return All video outputs of this driver, ordered by stream ID.
     */
    public List<VideoOutput<FFMPEGSensor>> getVideoOutputs() {
        return Collections.unmodifiableList(videoOutputs);
    }

    /**
     * @return All audio outputs of this driver, ordered by stream ID.
     */
    public List<AudioOutput<FFMPEGSensor>> getAudioOutputs() {
        return Collections.unmodifiableList(audioOutputs);
    }

    /**
     * @return All binary data outputs of this driver, ordered by stream ID.
     */
    public List<DataOutput<FFMPEGSensor>> getDataOutputs() {
        return Collections.unmodifiableList(dataOutputs);
    }

    /**
     * Creates the video output for the given stream if it does not exist yet, then registers it as the
     * listener for that stream.
     *
     * @param streamContext The video stream to publish.
     * @param index         Zero-based ordinal of this stream among the video streams of the source.
     */
    protected void attachVideoOutput(StreamContext streamContext, int index) {
        if (index >= videoOutputs.size()) {
            createVideoOutput(streamContext.getFrameDimensions(), streamContext.getCodecName());

            logger.info("Created video output '{}' for stream {} ({}, {}x{})", videoOutputs.get(index).getName(),
                    streamContext.getStreamId(), streamContext.getCodecName(),
                    streamContext.getFrameWidth(), streamContext.getFrameHeight());
        }

        streamContext.setDataBufferListener(videoOutputs.get(index));
    }

    /**
     * Creates the audio output for the given stream if it does not exist yet, then registers it as the
     * listener for that stream.
     *
     * @param streamContext The audio stream to publish.
     * @param index         Zero-based ordinal of this stream among the audio streams of the source.
     */
    protected void attachAudioOutput(StreamContext streamContext, int index) {
        if (index >= audioOutputs.size()) {
            createAudioOutput(streamContext.getSampleRate(), streamContext.getCodecName());

            logger.info("Created audio output '{}' for stream {} ({}, {} Hz)", audioOutputs.get(index).getName(),
                    streamContext.getStreamId(), streamContext.getCodecName(), streamContext.getSampleRate());
        }

        streamContext.setDataBufferListener(audioOutputs.get(index));
    }

    /**
     * Creates the binary data output for the given stream if it does not exist yet, then registers it as the
     * listener for that stream.
     *
     * @param streamContext The data stream to publish.
     * @param index         Zero-based ordinal of this stream among the data streams of the source.
     */
    protected void attachDataOutput(StreamContext streamContext, int index) {
        if (index >= dataOutputs.size()) {
            createDataOutput();

            logger.info("Created binary data output '{}' for stream {} (codec {}, tag '{}', handler '{}')",
                    dataOutputs.get(index).getName(), streamContext.getStreamId(), streamContext.getCodecName(),
                    streamContext.getCodecTagString(), streamContext.getHandlerName());
        }

        streamContext.setDataBufferListener(dataOutputs.get(index));
    }

    /**
     * Builds the output name for a stream, leaving the first stream of each kind unsuffixed so that it
     * matches the name a single-stream source would have used.
     */
    protected static String outputName(String baseName, int index) {
        return index == 0 ? baseName : baseName + (index + 1);
    }

    /**
     * Builds the output label for a stream, leaving the first stream of each kind unsuffixed.
     */
    protected static String outputLabel(String baseLabel, int index) {
        return index == 0 ? baseLabel : baseLabel + " " + (index + 1);
    }

    /**
     * This causes the frames of the video to start being processed.
     * If it's from a network stream, that means that data will start flowing across the wire.
     * If it's from a file stream, the frames are read from disk.
     *
     * @throws SensorHubException If there is a problem starting the stream processor.
     */
    protected void startStream() throws SensorHubException {
        try {
            if (mpegTsProcessor != null) {
                mpegTsProcessor.processStream();
                mpegTsProcessor.setReconnect(true);
            }
        } catch (IllegalStateException e) {
            String message = "Failed to start stream processor";
            logger.error(message);
            throw new SensorHubException(message, e);
        }
    }

    /**
     * Stops the stream processor and cleans up resources.
     *
     * @throws SensorHubException If there is a problem stopping the stream processor.
     */
    protected void stopStream() throws SensorHubException {
        logger.info("Stopping MPEG TS processor for {}", getUniqueIdentifier());

        if (mpegTsProcessor != null) {
            mpegTsProcessor.stopProcessingStream();

            try {
                // Wait for thread to finish
                logger.info("Waiting for stream processor to stop");
                mpegTsProcessor.join(1000);
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
}
