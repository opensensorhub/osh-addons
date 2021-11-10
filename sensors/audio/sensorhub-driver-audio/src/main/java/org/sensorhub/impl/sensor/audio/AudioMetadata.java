package org.sensorhub.impl.sensor.audio;

/**
 * Simple container for a few metadata fields we need in order to decode with FFMpeg
 * 
 * @author tcook
 *
 */
public class AudioMetadata 
{
	String format;
	String codec;
	int sampleRate;
	int numChannels;
	String bitrate;
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("codec: " + codec + "\n");
		b.append("sampleRate: " + sampleRate + "\n");
		b.append("numChannels: " + numChannels + "\n");
		
		return b.toString();
	}
}
