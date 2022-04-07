package org.sensorhub.impl.sensor.uas.config;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.sensor.uas.UasOnDemandSensor;

/**
 * Configuration sub-object for the {@link UasOnDemandSensor} that provides the video frame size and codec.
 */
public class VideoConfig {
	@DisplayInfo(label = "Video Width", desc = "Width of the video frames, in pixels.")
	public int videoFrameWidth = 1920;
	
	@DisplayInfo(label = "Video Height", desc = "Height of the video frames, in pixels.")
	public int videoFrameHeight = 1080;
}
