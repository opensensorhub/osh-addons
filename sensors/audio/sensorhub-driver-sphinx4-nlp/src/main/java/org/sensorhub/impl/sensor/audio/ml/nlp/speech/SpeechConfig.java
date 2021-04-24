/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2021 Nicolas Garay
 All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.audio.ml.nlp.speech;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;

/**
 *
 * @author Nick Garay
 * @since Mar. 12, 2021
 */
public class SpeechConfig {

    @DisplayInfo(label="Source", desc="Source type of audio information")
    public SpeechRecognizerType speechRecognizerType = SpeechRecognizerType.STREAM;

    @DisplayInfo(label="Language", desc="The language model to use in speech recognition")
    public LanguageModel languageModel = LanguageModel.ENGLISH;

    @DisplayInfo(label="WAV File", desc="Optional WAV File to Process for Testing")
    public String wavFile = null;

    @DisplayInfo(desc="Communication settings to connect to data stream")
    public CommProviderConfig<?> commSettings;
}
