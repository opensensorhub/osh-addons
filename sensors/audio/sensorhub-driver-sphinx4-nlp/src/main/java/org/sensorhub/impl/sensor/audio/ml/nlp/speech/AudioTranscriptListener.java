/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2021 Nicolas Garay
 All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.audio.ml.nlp.speech;

import edu.cmu.sphinx.api.SpeechResult;

/**
 *
 * @author Nick Garay
 * @since Mar. 12, 2021
 */
public interface AudioTranscriptListener {

    void onTranscribedAudio(SpeechResult speechResult);
}
