package org.sensorhub.impl.sensor.kinect;

import java.nio.ByteBuffer;

import org.openkinect.freenect.Device;
import org.openkinect.freenect.FrameMode;
import org.openkinect.freenect.VideoHandler;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.vast.data.DataBlockByte;
import org.vast.data.DataBlockList;

import net.opengis.swe.v20.DataBlock;

class KinectInfraredOutput extends KinectVideoOutput {

	public KinectInfraredOutput(KinectSensor parentSensor, Device kinectDevice) {

		super(parentSensor, kinectDevice);
	}

	@Override
	public void start() {
		
		device.startVideo(new VideoHandler() {
			
			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
				
				DataBlock dataBlock = videoStream.createDataBlock();
				
				byte[] pixels = new byte[getParentModule().getConfiguration().frameWidth * getParentModule().getConfiguration().frameHeight];
				
				for (short height = 0; height < getParentModule().getConfiguration().frameHeight; ++height) {
					
					for (short width = 0; width < getParentModule().getConfiguration().frameWidth; ++width) {
					
						int offset = (width + height * getParentModule().getConfiguration().frameWidth);
						
						pixels[offset] = frame.get(offset);
					}
				}
				
				DataBlockByte blockByte = new DataBlockByte();
				blockByte.setUnderlyingObject(pixels);
	            ((DataBlockList)dataBlock).getUnderlyingObject().add(blockByte);
				
		        latestRecord = dataBlock;
		        
		        latestRecordTime = System.currentTimeMillis();
		        
		        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, KinectInfraredOutput.this, dataBlock));      
		        
				frame.position(0);
			}
		});		
	}
}
