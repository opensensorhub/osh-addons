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
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.vast.data.DataBlockMixed;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;

class KinectVideoOutput extends KinectOutputInterface {
	
	private static final String ERR_STR = new String("Error while initializing RGB video output");
	
	private static final String STR_NAME = new String("Kinect Camera (RGB)");
			
	protected static final int BYTES_PER_PIXEL = 3;

	protected DataStream videoStream;

	public KinectVideoOutput(KinectSensor parentSensor, Device kinectDevice) {
		
		super(parentSensor, kinectDevice);
		
		name = STR_NAME;
	}

	@Override
	public DataComponent getRecordDescription() {
		
		return videoStream.getElementType();
	}

	@Override
	public DataEncoding getRecommendedEncoding() {

		return videoStream.getEncoding();
	}
	
	@Override
	public void init() throws SensorException {

		device.setVideoFormat(getParentModule().getConfiguration().rgbFormat);

        try {

            VideoCamHelper videoCamHelper = new VideoCamHelper();

            videoStream = videoCamHelper.newVideoOutputRGB(getName(),
            		getParentModule().getConfiguration().frameWidth, getParentModule().getConfiguration().frameHeight);
                                    
        } catch (Exception e) {
        	
            throw new SensorException(ERR_STR, e);
        }
	}

	@Override
	public void start() {
		
		device.startVideo(new VideoHandler() {
			
			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
				
				DataBlock dataBlock = videoStream.getElementType().createDataBlock();
				
				byte[] channelData = new byte[BYTES_PER_PIXEL * getParentModule().getConfiguration().frameWidth * getParentModule().getConfiguration().frameHeight];

				getChannelData(frame, channelData);

				double samplingTime = System.currentTimeMillis() / MS_PER_S;
				
				dataBlock.setDoubleValue(IDX_TIME_DATA_COMPONENT, samplingTime);
						
				((DataBlockMixed) dataBlock).getUnderlyingObject()[IDX_PAYLOAD_DATA_COMPONENT].setUnderlyingObject(channelData);

				latestRecord = dataBlock;
		        
		        latestRecordTime = System.currentTimeMillis();
		        
		        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, KinectVideoOutput.this, dataBlock));      
		        
				frame.position(0);
			}
		});		
	}
	
	protected void getChannelData(ByteBuffer frame, byte[] channelData) {
		
		for (short y = 0; y < getParentModule().getConfiguration().frameHeight; ++y) {

			for (short x = 0; x < getParentModule().getConfiguration().frameWidth; ++x) {
			
				int offset = BYTES_PER_PIXEL * (x + y * getParentModule().getConfiguration().frameWidth);

				// Kinect reports in BGRA
				byte r = frame.get(offset + 2);
				byte g = frame.get(offset + 1);
				byte b = frame.get(offset + 0);
				
				// Transpose as RGB
				channelData[offset + 0] = r;
				channelData[offset + 1] = g;
				channelData[offset + 2] = b;
			}
		}
	}
}
