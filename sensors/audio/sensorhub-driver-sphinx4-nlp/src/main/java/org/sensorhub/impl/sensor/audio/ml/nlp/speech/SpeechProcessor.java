/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2021 Nicolas Garay
 All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.audio.ml.nlp.speech;

import edu.cmu.sphinx.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Nick Garay
 * @since Mar. 12, 2021
 */
public class SpeechProcessor extends Thread {

    /**
     * Logging utility
     */
    private static final Logger logger = LoggerFactory.getLogger(SpeechProcessor.class);

    /**
     * Name of thread
     */
    private static final String WORKER_THREAD_NAME = "STREAM-PROCESSOR";

    private AbstractSpeechRecognizer recognizer;

    private final SpeechRecognizerType speechRecognizerType;

    private final InputStream inputStream;

    private final Configuration configuration;

    private final List<AudioTranscriptListener> listenerList = new ArrayList<>();

    private final AtomicBoolean processing = new AtomicBoolean(false);

    public SpeechProcessor(SpeechRecognizerType speechRecognizerType, LanguageModel languageModel,
                           InputStream inputStream) {

        super(WORKER_THREAD_NAME);

        this.speechRecognizerType = speechRecognizerType;
        this.inputStream = inputStream;

        configuration = new Configuration();
        configuration.setAcousticModelPath(languageModel.getAcousticModelPath());
        configuration.setDictionaryPath(languageModel.getDictionaryPath());
        configuration.setLanguageModelPath(languageModel.getLanguageModelPath());
    }

    public void processStream() throws IOException {

        if (speechRecognizerType == SpeechRecognizerType.STREAM) {

            recognizer = new StreamSpeechRecognizer(configuration);
            ((StreamSpeechRecognizer) recognizer).startRecognition(inputStream);

        } else {

            recognizer = new LiveSpeechRecognizer(configuration);
            ((LiveSpeechRecognizer) recognizer).startRecognition(true);
        }

        processing.set(true);

        start();
    }

    public void stopProcessingStream() {

        if (speechRecognizerType == SpeechRecognizerType.STREAM) {

            ((StreamSpeechRecognizer) recognizer).stopRecognition();

        } else {

            ((LiveSpeechRecognizer) recognizer).stopRecognition();
        }

        processing.set(false);
    }

    @Override
    public void run() {

        logger.info("Starting speech processor");

        SpeechResult result;

        while (processing.get() && ((result = recognizer.getResult()) != null)) {

            logger.info("Hypothesis: {}\n", result.getHypothesis());

            for(AudioTranscriptListener listener : listenerList) {

                listener.onTranscribedAudio(result);
            }
        }

        logger.info("Speech processor has terminated");
    }

    public void removeListener(AudioTranscriptListener listener) {

        listenerList.remove(listener);
    }

    public void addListener(AudioTranscriptListener listener) {

        if (!listenerList.contains(listener)) {

            listenerList.add(listener);
        }
    }
}
