/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.
Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2019 Botts Innovative Research, Inc. All Rights Reserved. 

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kinect;

import java.nio.ByteBuffer;

import org.openkinect.freenect.Device;
import org.openkinect.freenect.FrameMode;
import org.openkinect.freenect.VideoHandler;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.vast.data.DataBlockByte;
import org.vast.data.DataBlockList;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;

class KinectInfraredOutput extends AbstractSensorOutput<KinectSensor> {

	protected Device device = null;

	protected DataStream irStream;

	public KinectInfraredOutput(KinectSensor parentSensor, Device kinectDevice) {

		super(parentSensor);

		device = kinectDevice;

		name = "Kinect IR Camera";
	}

	@Override
	public DataComponent getRecordDescription() {

		return irStream.getElementType();
	}

	@Override
	public DataEncoding getRecommendedEncoding() {

		return irStream.getEncoding();
	}

	@Override
	public double getAverageSamplingPeriod() {

		return device.getVideoMode().framerate;
	}

	@Override
	protected void stop() {

		device.stopVideo();
	}

	public void init() throws SensorException {

		device.setVideoFormat(getParentModule().getConfiguration().irFormat);

		try {

			VideoCamHelper videoCamHelper = new VideoCamHelper();

            irStream = videoCamHelper.newVideoOutputRGB(getName(),
            		getParentModule().getConfiguration().frameWidth, getParentModule().getConfiguration().frameHeight);

		} catch (Exception e) {

			throw new SensorException("Error while initializing ir output", e);
		}
	}

	public void start() {

		device.startVideo(new VideoHandler() {

			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {

				DataBlock dataBlock = irStream.createDataBlock();

				byte[] channelData = new byte[getParentModule().getConfiguration().frameWidth
						* getParentModule().getConfiguration().frameHeight];

				for (short y = 0; y < getParentModule().getConfiguration().frameHeight; ++y) {

					for (short x = 0; x < getParentModule().getConfiguration().frameWidth; ++x) {

						int offset = (x + y * getParentModule().getConfiguration().frameWidth);

						channelData[offset] = frame.get(offset);
					}
				}

				DataBlockByte blockByte = new DataBlockByte();
				blockByte.setUnderlyingObject(channelData);
				((DataBlockList)dataBlock).getUnderlyingObject().add(blockByte);

				latestRecord = dataBlock;

				latestRecordTime = System.currentTimeMillis();

				eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, KinectInfraredOutput.this, dataBlock));

				frame.position(0);
			}
		});
	}
}
