/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.flir.dh390;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.ffmpeg.outputs.AudioOutput;
import org.sensorhub.impl.sensor.ffmpeg.outputs.VideoOutput;
import org.sensorhub.mpegts.MpegTsProcessor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Sensor driver for the FLIR DH-390 camera.
 */
public class DH390Driver extends AbstractSensorModule<DH390Config> {
    protected VideoOutput<DH390Driver> visualVideoOutput;
    protected VideoOutput<DH390Driver> thermalVideoOutput;
    protected AudioOutput<DH390Driver> audioOutput;
    protected DH390OrientationOutput orientationOutput;
    protected MpegTsProcessor visualMpegTsProcessor;
    protected MpegTsProcessor thermalMpegTsProcessor;
    protected ScheduledExecutorService executor;
    String visualConnectionString;
    String thermalConnectionString;

    /**
     * Initialize the sensor driver and its outputs.
     *
     * @throws SensorHubException If there is an error initializing the sensor driver.
     */
    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        generateUniqueID("urn:osh:sensor:flir:dh-390:", config.serialNumber);
        generateXmlID("FLIR_DH-390_", config.serialNumber);

        if (visualMpegTsProcessor != null)
            visualMpegTsProcessor.closeStream();
        if (thermalMpegTsProcessor != null)
            thermalMpegTsProcessor.closeStream();

        visualMpegTsProcessor = null;
        thermalMpegTsProcessor = null;
        visualVideoOutput = null;
        thermalVideoOutput = null;

        // Create connection strings for the visual and thermal cameras
        String connectionString = "rtsp://" + config.connection.userName + ":" + config.connection.password + "@" + config.connection.ipAddress;
        visualConnectionString = connectionString + "/ch1";
        thermalConnectionString = connectionString + "/ch2";

        setupStreams();

        // Initialize the orientation output
        if (config.positionConfig.orientation != null) {
            orientationOutput = new DH390OrientationOutput(this);
            orientationOutput.doInit();
            addOutput(orientationOutput, true);
        }
    }

    /**
     * Start the sensor driver and its outputs.
     *
     * @throws SensorHubException If there is an error starting the sensor driver.
     */
    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        // Set the sensor orientation
        if (orientationOutput != null && config.positionConfig.orientation != null) {
            orientationOutput.setOrientation(config.positionConfig.orientation);
        }

        setupStreams();
        startStreams();
    }

    /**
     * Stop the sensor driver and its outputs.
     *
     * @throws SensorHubException If there is an error stopping the sensor driver.
     */
    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();

        stopStreams();
        shutdownExecutor();
    }

    /**
     * Check if the sensor driver is connected to the camera.
     *
     * @return True if the sensor driver is connected to the camera, false otherwise.
     */
    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("FLIR DH-390 Camera");
        }
    }

    protected void setupStreams() throws SensorHubException {
        openStreamThermal();
        openStreamVisual();
        setupExecutor();
    }

    /**
     * Creates the background thread that will handle video decoding if it hasn't already been done.
     * Also tells the setDecoder and videoOutput about the executor.
     */
    protected void setupExecutor() {
        if (executor == null)
            executor = Executors.newSingleThreadScheduledExecutor();

        if (visualVideoOutput != null)
            visualVideoOutput.setExecutor(executor);
        if (thermalVideoOutput != null)
            thermalVideoOutput.setExecutor(executor);
        if (audioOutput != null)
            audioOutput.setExecutor(executor);
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
     * Opens the connection to the upstream source but does not yet start reading any more than the initial
     * metadata necessary to get the video frame size.
     * This can be called multiple times, but will only have any effect the first time it's called after doInit()
     * since it checks for a null mpegTsProcessor.
     * This also has the side effect of creating and adding the Video output if it hasn't already happened earlier.
     *
     * @throws SensorHubException If there is a problem opening the stream.
     */
    protected void openStreamVisual() throws SensorHubException {
        if (visualMpegTsProcessor == null) {
            visualMpegTsProcessor = new MpegTsProcessor(visualConnectionString);

            // Initialize the MPEG transport stream processor from the source named in the configuration.
            if (visualMpegTsProcessor.openStream()) {
                // If there is a video content in the stream
                if (visualMpegTsProcessor.hasVideoStream()) {
                    // In case we were waiting until we got video data to make the video frame output,
                    // we go ahead and do that now.
                    if (visualVideoOutput == null) {
                        visualVideoOutput = new VideoOutput<>(this, visualMpegTsProcessor.getVideoStreamFrameDimensions(), visualMpegTsProcessor.getVideoCodecName(), "visual", "Visual Camera", "Visual stream using ffmpeg library");
                        if (executor != null) {
                            visualVideoOutput.setExecutor(executor);
                        }
                        addOutput(visualVideoOutput, false);
                        visualVideoOutput.doInit();
                    }
                    // Set video stream packet listener to video output
                    visualMpegTsProcessor.setVideoDataBufferListener(visualVideoOutput);
                }

                // If there is an audio content in the stream
                if (visualMpegTsProcessor.hasAudioStream()) {
                    // In case we were waiting until we got audio data to make the audio output,
                    // we go ahead and do that now.
                    if (audioOutput == null) {
                        audioOutput = new AudioOutput<>(this, visualMpegTsProcessor.getAudioSampleRate(), visualMpegTsProcessor.getAudioCodecName());
                        if (executor != null) {
                            audioOutput.setExecutor(executor);
                        }
                        addOutput(audioOutput, false);
                        audioOutput.doInit();
                    }
                    // Set audio stream packet listener to audio output
                    visualMpegTsProcessor.setAudioDataBufferListener(audioOutput);
                }
            } else {
                throw new SensorHubException("Unable to open stream from data source");
            }
        }
    }

    /**
     * Opens the connection to the upstream source but does not yet start reading any more than the initial
     * metadata necessary to get the video frame size.
     * This can be called multiple times, but will only have any effect the first time it's called after doInit()
     * since it checks for a null mpegTsProcessor.
     * This also has the side effect of creating and adding the Video output if it hasn't already happened earlier.
     *
     * @throws SensorHubException If there is a problem opening the stream.
     */
    protected void openStreamThermal() throws SensorHubException {
        if (thermalMpegTsProcessor == null) {
            thermalMpegTsProcessor = new MpegTsProcessor(thermalConnectionString);

            // Initialize the MPEG transport stream processor from the source named in the configuration.
            if (thermalMpegTsProcessor.openStream()) {
                // If there is a video content in the stream
                if (thermalMpegTsProcessor.hasVideoStream()) {
                    // In case we were waiting until we got video data to make the video frame output,
                    // we go ahead and do that now.
                    if (thermalVideoOutput == null) {
                        thermalVideoOutput = new VideoOutput<>(this, thermalMpegTsProcessor.getVideoStreamFrameDimensions(), thermalMpegTsProcessor.getVideoCodecName(), "thermal", "Thermal Camera", "Thermal stream using ffmpeg library");
                        if (executor != null) {
                            thermalVideoOutput.setExecutor(executor);
                        }
                        addOutput(thermalVideoOutput, false);
                        thermalVideoOutput.doInit();
                    }
                    // Set video stream packet listener to video output
                    thermalMpegTsProcessor.setVideoDataBufferListener(thermalVideoOutput);
                }
            } else {
                throw new SensorHubException("Unable to open stream from data source");
            }
        }
    }

    /**
     * This causes the frames of the video to start being processed.
     *
     * @throws SensorHubException If there is a problem starting the stream processor.
     */
    protected void startStreams() throws SensorHubException {
        try {
            if (visualMpegTsProcessor != null) {
                visualMpegTsProcessor.processStream();
                visualMpegTsProcessor.setReconnect(true);
            }
            if (thermalMpegTsProcessor != null) {
                thermalMpegTsProcessor.processStream();
                thermalMpegTsProcessor.setReconnect(true);
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
    protected void stopStreams() throws SensorHubException {
        if (visualMpegTsProcessor != null) {
            visualMpegTsProcessor.stopProcessingStream();

            try {
                // Wait for thread to finish
                visualMpegTsProcessor.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SensorHubException("Interrupted waiting for stream processor to stop", e);
            } finally {
                // Close stream and cleanup resources
                visualMpegTsProcessor.closeStream();
                visualMpegTsProcessor = null;
            }
        }

        if (thermalMpegTsProcessor != null) {
            thermalMpegTsProcessor.stopProcessingStream();

            try {
                // Wait for thread to finish
                thermalMpegTsProcessor.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SensorHubException("Interrupted waiting for stream processor to stop", e);
            } finally {
                // Close stream and cleanup resources
                thermalMpegTsProcessor.closeStream();
                thermalMpegTsProcessor = null;
            }
        }
    }
}
