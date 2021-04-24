/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2021 Nicolas Garay
 All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.audio.ml.nlp;

import org.sensorhub.impl.sensor.audio.ml.nlp.speech.SpeechProcessor;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Sensor driver for the audio transcription providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Mar. 5, 2021
 */
public class AudioTranscriberSensor extends AbstractSensorModule<AudioTranscriberConfig> {

    private static final Logger logger = LoggerFactory.getLogger(AudioTranscriberSensor.class);

    private AudioTranscriberOutput output;

    private ICommProvider<?> commProvider = null;

    private SpeechProcessor speechProcessor;

    @Override
    public void init() throws SensorHubException {

        super.init();

        // Generate identifiers
        generateUniqueID("urn:sensor:audio:transcriber:", config.serialNumber);
        generateXmlID("AUDIO_TRANSCRIBER_", config.serialNumber);

        // Create and initialize output
        output = new AudioTranscriberOutput(this);

        output.init();

        addOutput(output, false);
    }

    @Override
    public void start() throws SensorHubException {

        if (null != output) {

            // Allocate necessary resources and start outputs
            output.start();
        }

        try {

            InputStream inputStream = null;

            if (config.speechConfig.commSettings != null) {

                if (commProvider == null) {

                    commProvider = config.speechConfig.commSettings.getProvider();
                    commProvider.init();
                    commProvider.start();

                    inputStream = commProvider.getInputStream();
                }

            } else {

                inputStream = new FileInputStream(config.speechConfig.wavFile);
            }

            speechProcessor = new SpeechProcessor(
                    config.speechConfig.speechRecognizerType,
                    config.speechConfig.languageModel,
                    inputStream);
            speechProcessor.addListener(output);
            speechProcessor.processStream();

        } catch (FileNotFoundException e) {

            logger.error("Failed to get input stream from file due to exception {}", e.getMessage());

            throw new SensorHubException("Failed to start driver due to exception:", e);

        } catch (IOException e) {

            logger.error("Failed to get input stream from commProvider due to exception {}", e.getMessage());

            throw new SensorHubException("Failed to start driver due to exception:", e);
        }
    }

    @Override
    public void stop() throws SensorHubException {

        if (null != speechProcessor) {

            speechProcessor.removeListener(output);
            speechProcessor.stopProcessingStream();
            speechProcessor = null;
        }

        try {

            if (commProvider != null && commProvider.isStarted()) {

                commProvider.stop();
                commProvider.cleanup();
                commProvider = null;
            }

        } finally {

            if (null != output) {

                output.stop();
                output = null;
            }
        }
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }
}
