package org.sensorhub.impl.sensor.kinect;

import java.nio.ByteBuffer;

import org.openkinect.freenect.Device;
import org.openkinect.freenect.FrameMode;
import org.openkinect.freenect.VideoHandler;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;

class KinectCameraOutput extends AbstractSensorOutput<KinectSensor>{

	private Device device = null;

	private DataEncoding encoding;

	private DataComponent data;

	public KinectCameraOutput(KinectSensor parentSensor, Device kinectDevice) {
		
		super(parentSensor);
		
		device = kinectDevice;
	}

	@Override
	public DataComponent getRecordDescription() {
		
		return data;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {

		return encoding;
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

		device.setVideoFormat(parentSensor.getDeviceParams().getCameraVideoFormat(), 
				parentSensor.getDeviceParams().getCameraVideoResolution());

        try {

            VideoCamHelper videoCamHelper = new VideoCamHelper();

            // build output structure
            DataStream videoStream = videoCamHelper.newVideoOutputRGB(getName(), device.getVideoMode().width, device.getVideoMode().height);
            
            data = videoStream.getElementType();
            
            encoding = videoStream.getEncoding();
            
        } catch (Exception e) {
        	
            throw new SensorException("Error while initializing video output", e);
        }
	}

	public void start() {
		
		device.startVideo(new VideoHandler() {
			
			@Override
			public void onFrameReceived(FrameMode mode, ByteBuffer frame, int timestamp) {
				
				DataBlock dataBlock = data.createDataBlock();
				
				dataBlock.setUnderlyingObject(frame.array());
				
		        latestRecord = dataBlock;
		        
		        latestRecordTime = System.currentTimeMillis();
		        
		        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, KinectCameraOutput.this, dataBlock));        
			}
		});		
	}
}
