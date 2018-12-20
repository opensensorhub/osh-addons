package org.sensorhub.impl.sensor.kinect;

import org.openkinect.freenect.DepthFormat;
import org.openkinect.freenect.LedStatus;
import org.openkinect.freenect.Resolution;
import org.openkinect.freenect.VideoFormat;

class KinectDeviceParams {
	
	private VideoFormat cameraVideoFormat = VideoFormat.BAYER;
	private Resolution cameraResolution = Resolution.LOW;
	
	private VideoFormat infraredVideoFormat = VideoFormat.IR_10BIT;
	private Resolution infraredResolution = Resolution.LOW;
	
	private DepthFormat depthFormat = DepthFormat.MM;
	private Resolution depthResolution = Resolution.LOW;
	
	private LedStatus ledStatus = LedStatus.GREEN;
    
	private double tiltAngle = 0.0;
		
	public KinectDeviceParams() {
		
	}
	
	public void setCameraOutputParams(int formatValue, int resValue) {
		
		VideoFormat format = VideoFormat.fromInt(formatValue);
		
		if (null != format) {
		
			cameraVideoFormat = format;
		}
		
		Resolution resolution = Resolution.fromInt(resValue);
		
		if (null != resolution) {
			
			cameraResolution = resolution;
		}
	}
	
	public VideoFormat getCameraVideoFormat() {
		
		return cameraVideoFormat;
	}
	
	public Resolution getCameraVideoResolution() {
		
		return cameraResolution;
	}
	
	public void setInfraredOuptutParams(int formatValue, int resValue) {
		
		VideoFormat format = VideoFormat.fromInt(formatValue);
		
		if (null != format) {
		
			infraredVideoFormat = format;
		}

		Resolution resolution = Resolution.fromInt(resValue);
		
		if (null != resolution) {
			
			infraredResolution = resolution;
		}
	}
	
	public VideoFormat getInfraredVideoFormat() {
		
		return infraredVideoFormat;
	}
	
	public Resolution getInfraredVideoResolution() {
		
		return infraredResolution;
	}

	public void setDepthOutputParams(int formatValue, int resValue) {
		
		DepthFormat format = DepthFormat.fromInt(formatValue);
		
		if (null != format) {
		
			depthFormat = format;
		}
		
		Resolution resolution = Resolution.fromInt(resValue);
		
		if (null != resolution) {
			
			depthResolution = resolution;
		}
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
}
