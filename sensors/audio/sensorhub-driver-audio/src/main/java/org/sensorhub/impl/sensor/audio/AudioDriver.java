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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;


public class AudioDriver extends AbstractSensorModule<AudioConfig> 
{
	AudioOutput output;
	Thread fileReader;
	
	String [] supportedFormats = { "wav" };
	Logger logger;
	
	@Override
	public void init() throws SensorHubException
	{
		super.init();
		logger = getLogger();
		this.output = new AudioOutput(this);
		addOutput(output, false);
		
		if(config.startTimeOverride != null) {
			//  TODO
			// verifyValidTime(startTimeOverride
		}
	}

	@Override
	public void start() throws SensorHubException {
		if(config.wavDir != null) {
			if(config.startTimeOverride != null) {
				// TODO
			}
			startDir();
		} else if(config.wavFile != null) {
			startSingleFile();
		} else {
			//  TODO: add support for capture from Android/other audio sources
		}
	}
	
	public void startDir() {
//		Path p = Paths.get(config.wavDir);
//		assert Files.exists(p);
//		Iterator<File> it = WavDirectoryIterator.getFileIterator(p, "wav");
//		while(it.hasNext()) {
		//  TODO modify reader for one file at a time
		fileReader = new FfmpegReader(config, output);
		fileReader.start();
	}
	
	//  TODO - I broke this when supporting dir of files- fix it
	public void startSingleFile() throws SensorHubException
	{
		try {
			Path p = Paths.get(config.wavFile);
			double baseTime = WavDirectoryIterator.filenameTimeParser(p.getFileName().toString(), "'");
			output.baseTime = baseTime;
//		output.baseTime = System.currentTimeMillis() / 1000;;
			fileReader = new FfmpegReader(config, output);
			fileReader.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

