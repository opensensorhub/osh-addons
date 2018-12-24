package org.sensorhub.impl.sensor.kinect;

import org.openkinect.freenect.DepthFormat;
import org.openkinect.freenect.LedStatus;
import org.openkinect.freenect.Resolution;
import org.openkinect.freenect.VideoFormat;

class KinectDeviceParams {
	
	private VideoFormat videoFormat = VideoFormat.IR_8BIT;
	private Resolution videoResolution = Resolution.LOW;
	
	private DepthFormat depthFormat = DepthFormat.D11BIT;
	private Resolution depthResolution = Resolution.LOW;
	
	private LedStatus ledStatus = LedStatus.GREEN;
    
	private double tiltAngle = 0.0;
	
	private int frameWidth = 640;
	
	private int frameheight = 480;
	
	enum VideoMode {
		CAMERA(0),
		IR(1);

		private int value;
		
		private VideoMode(int value) {
			
			this.value = value;
		}
		
		public int valueOf() {
			
			return value;
		}
	}
	
	private VideoMode videoMode = VideoMode.IR;
		
	public KinectDeviceParams() {
		
	}
	
	public VideoMode getVideoMode() {
		
		return videoMode;
	}
	
	public void setVideoMode(int value) {
		
		videoMode = VideoMode.CAMERA;
		videoFormat = VideoFormat.RGB;
		
		if (value == VideoMode.IR.valueOf()) {
			
			videoMode = VideoMode.IR;
			videoFormat = VideoFormat.IR_10BIT;
		}
	}
	
	public VideoFormat getVideoFormat() {
		
		return videoFormat;
	}
	
	public Resolution getVideoResolution() {
		
		return videoResolution;
	}
	
	public DepthFormat getDepthFormat() {
		
		return depthFormat;
	}

	public Resolution getDepthSensorResolution() {
		
		return depthResolution;
	}

	public void setTiltParams(double angle) {
		
		tiltAngle = angle;
	}
	
	public double getTiltAngle() {
		
		return tiltAngle;
	}
	
	public void setLedParams(int statusValue) {
		
		LedStatus status = LedStatus.fromInt(statusValue);
		
		if (null != status) {
		
			ledStatus = status;
		}
	}
	
	public LedStatus getLedStatus() {
		
		return ledStatus;
	}
	
	public int getFrameWidth() {
		
		return frameWidth;
	}
	
	public void setFrameWidth(int width) {
		
		frameWidth = width;
	}
	
	public int getFrameHeight() {
		
		return frameheight;
	}
	
	public void setFrameHeight(int height) {
		
		frameheight = height;
	}
}
