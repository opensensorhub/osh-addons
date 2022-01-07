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
	int sampleRate; // Hz
	int numChannels;
	String bitrate;
	double duration;  // seconds
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("codec: " + codec + "\n");
		b.append("sampleRate: " + sampleRate + "\n");
		b.append("numChannels: " + numChannels + "\n");
		b.append("duration: " + duration + "\n");
		b.append("bitrate: " + bitrate + "\n");
		return b.toString();
	}
}
