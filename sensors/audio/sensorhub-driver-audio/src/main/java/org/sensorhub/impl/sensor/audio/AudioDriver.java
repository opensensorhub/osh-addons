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
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;


public class AudioDriver extends AbstractSensorModule<AudioConfig> 
{
	AudioOutput output;
//	Thread fileReaderThread;
	Thread fileReader;
	String [] supportedFormats = { "wav" };
	Logger logger;
	
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
	public void start() throws SensorHubException
	{
//		fileReader = new JavaReader(config, output);
		fileReader = new FfmpegReader(config, output);
		fileReader.start();
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

