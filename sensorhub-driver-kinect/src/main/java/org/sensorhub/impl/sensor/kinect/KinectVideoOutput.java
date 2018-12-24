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

class KinectVideoOutput extends AbstractSensorOutput<KinectSensor>{

	protected Device device = null;

	protected DataStream videoStream;
	
	protected static final int BYTES_PER_PIXEL = 3;

	public KinectVideoOutput(KinectSensor parentSensor, Device kinectDevice) {
		
		super(parentSensor);
		
		device = kinectDevice;
		
		name = "Kinect Camera";
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
	public double getAverageSamplingPeriod() {

		return device.getVideoMode().framerate;
	}

	@Override
	protected void stop() {

		device.stopVideo();
	}
	
	public void init() throws SensorException {

		device.setVideoFormat(parentSensor.getDeviceParams().getVideoFormat());

        try {

            VideoCamHelper videoCamHelper = new VideoCamHelper();

            videoStream = videoCamHelper.newVideoOutputRGB(getName(),
            		parentSensor.getDeviceParams().getFrameWidth(), parentSensor.getDeviceParams().getFrameHeight());
                                    
        } catch (Exception e) {
        	
            throw new SensorException("Error while initializing video output", e);
        }
	}

	public void start() {
		
		device.startVideo(new VideoHandler() {
			
			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
				
				DataBlock dataBlock = videoStream.createDataBlock();
				
				byte[] pixels = new byte[BYTES_PER_PIXEL *parentSensor.getDeviceParams().getFrameWidth() * parentSensor.getDeviceParams().getFrameHeight()];
				
				for (short height = 0; height < parentSensor.getDeviceParams().getFrameHeight(); ++height) {
					
					for (short width = 0; width < parentSensor.getDeviceParams().getFrameWidth(); ++width) {
					
						int offset = BYTES_PER_PIXEL * (width + height * parentSensor.getDeviceParams().getFrameWidth());

						// Kinect reports in BGRA
						byte r = frame.get(offset + 2);
						byte g = frame.get(offset + 1);
						byte b = frame.get(offset + 0);
						
						// Transpose as RGB
						pixels[offset + 0] = r;
						pixels[offset + 1] = g;
						pixels[offset + 2] = b;
					}
				}
				
				DataBlockByte blockByte = new DataBlockByte();
				blockByte.setUnderlyingObject(pixels);
	            ((DataBlockList)dataBlock).getUnderlyingObject().add(blockByte);
				
		        latestRecord = dataBlock;
		        
		        latestRecordTime = System.currentTimeMillis();
		        
		        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, KinectVideoOutput.this, dataBlock));      
		        
				frame.position(0);
			}
		});		
	}
}
