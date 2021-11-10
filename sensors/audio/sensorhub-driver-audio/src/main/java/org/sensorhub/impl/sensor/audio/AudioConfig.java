package org.sensorhub.impl.sensor.audio;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;

public class AudioConfig extends SensorConfig
{
	@DisplayInfo(desc="Number of Audio Samples packed in the samples Array of each DataRecord")
//	Integer numSamplesPerArray = 100;
	Integer numSamplesPerArray = 256;
	
	@DisplayInfo(desc="The rate at which dataRecords are output in seconds")
	Double sampleOutputRate = 0.1;

	@DisplayInfo(desc="Wav file to process")
	String wavFile = "C:/Users/tcook/root/sensorHub/audio/wavFiles/GUI_Decode_2019-03-27 132356.wav";

	@DisplayInfo(desc="Directory containing multiple audio files to load")
	String wavDir = "C:/Users/tcook/root/sensorHub/socom/mastodon/data/20190327_SRSE_Vignette_Data/20190327_SRSE_Vignette_Data/Silent_Echo_Data/Audio_Decodes";
	
	//  TODO support pattern extraction of baseTime from a filename 
	@DisplayInfo(desc="baseTimePattern from timetagged filenames")
	String baseTimePattern = "yyyy-MM-dd HHmmss";
	
	//  TODO support this field
	@DisplayInfo(desc="override startTime")
	String startTimeOverride;
}

