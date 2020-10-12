package org.sensorhub.impl.sensor.audio;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

public class AudioConfig extends SensorConfig
{
	@DisplayInfo(desc="Number of Audio Samples packed in the samples Array of each DataRecord")
	Integer numSamplesPerArray = 100;
	
	@DisplayInfo(desc="The rate at which dataRecords are output")
	Double sampleOutputRate = 0.1;

	@DisplayInfo(desc="Wav file to process")
	String wavFile = "C:/Users/tcook/root/sensorHub/audio/wavFiles/GUI_Decode_2019-03-27 132356.wav";
}
