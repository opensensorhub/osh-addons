package org.sensorhub.impl.sensor.audio;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaReader extends Thread
{
	AudioConfig config;
	AudioOutput output;
	Path wavPath = Paths.get("");
	boolean isDir = false;
	//		private volatile boolean running = false; 
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public JavaReader(AudioConfig config, AudioOutput output) {
		this.config = config;
		this.output = output;
		this.wavPath = Paths.get(config.wavFile);
//		isDir = wavPath.toFile().isDirectory();
	}

	public JavaReader(String wavFile) {
		this.config = config;
		this.output = output;
		this.wavPath = Paths.get(wavFile);
//		isDir = wavFile.toFile().isDirectory();
	}

	@Override
	public void run() {
		// Open the wav file(s) specified as the first argument
		//			running = true;
		int frameCnt = 0;
		if(isDir) {
			
		}
		
		try(WavFile wavFile = WavFile.openWavFile(new File(wavPath.toString()))) {
			// Display information about the wav file
			wavFile.display();
			// Get the number of audio channels in the wav file
			int numChannels = wavFile.getNumChannels();
			// Create a buffer of numSamples frames
			double[] buffer = new double[config.numSamplesPerArray * numChannels];

			int framesRead;
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			do {
				// Read frames into buffer
				framesRead = wavFile.readFrames(buffer, config.numSamplesPerArray);
//				long offsetTime
				AudioRecord rec = new AudioRecord();
				rec.sampleData = buffer;
				rec.sampleIndex = frameCnt;
				rec.samplingRate = wavFile.getSampleRate();
				if(output != null)
					output.publishRecord(rec);

				// Loop through frames and look for minimum and maximum value
				for (int s=0 ; s<framesRead * numChannels ; s++) {
					if (buffer[s] > max) max = buffer[s];
					if (buffer[s] < min) min = buffer[s];
				}
				Thread.sleep(500L);
				frameCnt += framesRead;
				if(frameCnt % 1000 == 0)
					System.err.println(frameCnt + " frames read...");
			} while (framesRead != 0);

			// Close the wavFile
			wavFile.close();
			System.err.printf("Min: %f, Max: %f\n", min, max);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			//running = false;
		}
	}
	
	public static void main(String[] args) {
		AudioConfig config =  new AudioConfig();
//		String wavFile = "C:/Users/tcook/root/sensorHub/audio/wavFiles/GUI_Decode_2019-03-27 132356.wav";
		Thread readerThread =  new JavaReader(config, null);
		readerThread.start();
	}
}
