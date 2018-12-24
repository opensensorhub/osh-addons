package org.sensorhub.impl.sensor.kinect;

import org.openkinect.freenect.DepthFormat;
import org.openkinect.freenect.LedStatus;
import org.openkinect.freenect.VideoFormat;

class KinectDeviceParams {
	
	private boolean depthEnabled = true;
	
	private boolean irEnabled = true;

	private VideoFormat videoFormat = VideoFormat.IR_8BIT;
	
	private DepthFormat depthFormat = DepthFormat.D11BIT;
	
	private LedStatus ledStatus = LedStatus.GREEN;
    
	private double tiltAngle = 0.0;
	
	private int frameWidth = 640;
	
	private int frameheight = 480;
		
	public KinectDeviceParams() {
		
	}
	
	public boolean getIrEnabled() {
		
		return irEnabled;
	}
	
	public void toggleIrEnabled() {
		
		irEnabled = !irEnabled;
	}
	
	public boolean getDepthEnabled() {
		
		return depthEnabled;
	}
	
	public void toggleDepthEnabled() {
		
		depthEnabled = !depthEnabled;
	}
	
	public VideoFormat getVideoFormat() {
		
		return videoFormat;
	}
	
	public DepthFormat getDepthFormat() {
		
		return depthFormat;
	}

	public void setTiltAngle(double angle) {
		
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
	
	public int getFrameHeight() {
		
		return frameheight;
	}
}
