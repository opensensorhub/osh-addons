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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;


public class AudioDriver extends AbstractSensorModule<AudioConfig> 
{
	AudioOutput output;
	Thread fileReader;
	
	String [] supportedFormats = { "wav" };
	Logger logger;
	Path filePath;  // TODO support reading all files in dir
	boolean isDir = false;
	
	
	public AudioDriver() {
	}

	@Override
	public void init() throws SensorHubException
	{
		super.init();
		logger = getLogger();
		this.output = new AudioOutput(this);
		addOutput(output, false);
	}

	@Override
	public void start() throws SensorHubException {
		//  Start 
	
	}
	
	public void startSingleFile() throws SensorHubException
	{
		Path p = Paths.get(config.wavFile);
		double baseTime = filenameTimeParser(p.getFileName().toString());
		output.baseTime = baseTime;
//		output.baseTime = System.currentTimeMillis() / 1000;;
		fileReader = new FfmpegReader(config, output);
		fileReader.start();
	}
	
	//  Extract baseTime from filename 
	// GUI_Decode_2019-03-27 132356.wav
	public double filenameTimeParser(String fname) {
//		String fname = "GUI_Decode_2019-03-27 132356.wav";
		String dateStr = fname.substring(11, fname.length() - 4);
		System.err.println(dateStr);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss").withZone(ZoneId.of("GMT"));
		ZonedDateTime zdt = ZonedDateTime.parse(dateStr, formatter);
		long sec = zdt.toInstant().getEpochSecond();
		long nano = zdt.toInstant().getNano();
		double time = sec + nano;
//		System.err.println(time);
//		Instant inst = Instant.ofEpochSecond(sec, nano);
//		System.err.println(inst);
		
		return time;
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

