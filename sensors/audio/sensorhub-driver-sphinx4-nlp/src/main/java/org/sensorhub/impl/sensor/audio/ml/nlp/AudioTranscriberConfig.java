/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2021 Nicolas Garay
 All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.audio.ml.nlp;

import org.sensorhub.impl.sensor.audio.ml.nlp.speech.SpeechConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Configuration settings for the Audio Transcriber Sensor driver exposed via the OpenSensorHub Admin panel.
 *
 * Configuration settings take the form of
 * <code>
 *     DisplayInfo(desc="Description of configuration field to show in UI")
 *     public Type configOption;
 * </code>
 *
 * Containing an annotation describing the setting and if applicable its range of values
 * as well as a public access variable of the given Type
 *
 * @author Nick Garay
 * @since Mar. 5, 2021
 */
public class AudioTranscriberConfig extends SensorConfig {

    /**
     * The unique identifier for the configured sensor.
     */
    @DisplayInfo.Required
    @DisplayInfo(desc="Serial number or unique identifier for sensor")
    public String serialNumber = "sphinx4-001";

    @DisplayInfo.Required
    @DisplayInfo(label="Speech Recognition Settings", desc="Configuration settings for speech recognition")
    public SpeechConfig speechConfig = new SpeechConfig();
}
