/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.audio;

import java.io.File;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


public class AudioDriver extends AbstractSensorModule<AudioConfig> 
{
	AudioOutput output;
	Thread fileReaderThread;
	FileTester fileReader;

	public AudioDriver() {
	}

	@Override
	public void init() throws SensorHubException
	{
		super.init();
		this.output = new AudioOutput(this);
		addOutput(output, false);
	}

	@Override
	public void start() throws SensorHubException
	{
		fileReader = new FileTester(config.wavFile);
		fileReaderThread = new Thread(fileReader);
		fileReaderThread.start();
	}

	class FileTester implements Runnable {
		String testPath;
		//		private volatile boolean running = false; 

		public FileTester(String testPath) {
			this.testPath = testPath;
		}

		@Override
		public void run() {
			// Open the wav file specified as the first argument
			//			running = true;
			int frameCnt = 0;
			try(WavFile wavFile = WavFile.openWavFile(new File(config.wavFile))) {
				// Display information about the wav file
				wavFile.display();
				// Get the number of audio channels in the wav file
				int numChannels = wavFile.getNumChannels();
				// Create a buffer of 100 frames
				double[] buffer = new double[config.numSamplesPerArray * numChannels];

				int framesRead;
				double min = Double.MAX_VALUE;
				double max = Double.MIN_VALUE;
				do {
					// Read frames into buffer
					framesRead = wavFile.readFrames(buffer, config.numSamplesPerArray);
//					long offsetTime
					output.publishChunk(buffer, frameCnt, wavFile.getSampleRate());

					// Loop through frames and look for minimum and maximum value
					for (int s=0 ; s<framesRead * numChannels ; s++) {
						if (buffer[s] > max) max = buffer[s];
						if (buffer[s] < min) min = buffer[s];
					}
					Thread.sleep(5000L);
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

		public void stop() {
			//running = false;
		}
	}

	@Override
	public void stop() throws SensorHubException
	{
//		if(fileReader != null)
//			fileReader.stop();
	}

	@Override
	public boolean isConnected() {
		return false;
	}

}

