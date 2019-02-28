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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.openkinect.freenect.Device;
import org.openkinect.freenect.FrameMode;
import org.openkinect.freenect.VideoHandler;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.vast.data.DataBlockMixed;

import net.opengis.swe.v20.DataBlock;

class KinectInfraredOutputMJPEG extends KinectInfraredOutput {

	private static final String STR_NAME = new String("Kinect IR Camera (MJPEG)");

	private static final String STR_JPG_FORMAT_SPECIFIER = new String("jpg");

	private static final String ERR_STR = new String("Error while initializing IR output");

	public KinectInfraredOutputMJPEG(KinectSensor parentSensor, Device kinectDevice) {

		super(parentSensor, kinectDevice);

		name = STR_NAME;
	}

	@Override
	public void init() throws SensorException {

		device.setVideoFormat(getParentModule().getConfiguration().irFormat);

		try {

			VideoCamHelper irHelper = new VideoCamHelper();
			
            irStream = irHelper.newVideoOutputMJPEG(STR_NAME,
            		getParentModule().getConfiguration().frameWidth, 
            		getParentModule().getConfiguration().frameHeight);

		} catch (Exception e) {

			throw new SensorException(ERR_STR, e);
		}
	}

	@Override
	public void start() {

		device.startVideo(new VideoHandler() {

			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {

				DataBlock dataBlock = irStream.getElementType().createDataBlock();

				BufferedImage bufferedImage = new BufferedImage(getParentModule().getConfiguration().frameWidth,
						getParentModule().getConfiguration().frameHeight, BufferedImage.TYPE_BYTE_GRAY);

				byte[] channelData = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
				
				getChannelData(frame, channelData);

				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

				BufferedOutputStream bos = new BufferedOutputStream(byteStream);

				try {
					
					ImageIO.write(bufferedImage, STR_JPG_FORMAT_SPECIFIER, bos);

					byteStream.flush();

					byte[] newImage = byteStream.toByteArray();

					double samplingTime = System.currentTimeMillis() / MS_PER_S;
					
					dataBlock.setDoubleValue(samplingTime);

					((DataBlockMixed) dataBlock).getUnderlyingObject()[IDX_PAYLOAD_DATA_COMPONENT].setUnderlyingObject(newImage);

					latestRecord = dataBlock;

					latestRecordTime = System.currentTimeMillis();

					eventHandler.publishEvent(
							new SensorDataEvent(latestRecordTime, KinectInfraredOutputMJPEG.this, dataBlock));

				} catch (IOException e) {

					e.printStackTrace();
					
				} finally {

					frame.position(0);
				}
			}
		});
	}
}
