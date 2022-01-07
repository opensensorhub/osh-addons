package org.sensorhub.impl.sensor.audio;

public class AudioRecord 
{
	AudioMetadata metadata = new AudioMetadata();
	
	double [] sampleData;  // normalized array of samples
	int sampleIndex; // index of first sample in array 
	
	public byte [] getByteData() {
		byte[] bdata = new byte[sampleData.length];
		int min = 1000, max = -1000;
		for(int i=0; i<bdata.length; i++ ) {
			// map (-1,1) to (-127,128)
			bdata[i] = (byte)(sampleData[i] * 255.0);
			if(bdata[i] < min)  min = bdata[i];
			if(bdata[i] > max)  max = bdata[i];
		}
//		System.err.println("mm: " + min + "," + max);
		return bdata;
	}

	public float [] getFloatData() {
		float[] fdata = new float[sampleData.length];
		float min = 1000000.f, max = -1000000.f;

		for(int i=0; i<fdata.length; i++ ) {
			fdata[i] = (float)(sampleData[i]);
			if(fdata[i] < min)  min = fdata[i];
			if(fdata[i] > max)  max = fdata[i];
		}
//		System.err.println("mm: " + min + "," + max);
		return fdata;
	}
}
