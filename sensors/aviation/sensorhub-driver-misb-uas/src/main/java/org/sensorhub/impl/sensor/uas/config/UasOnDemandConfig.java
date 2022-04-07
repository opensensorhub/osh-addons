package org.sensorhub.impl.sensor.uas.config;

import org.sensorhub.impl.sensor.uas.UasOnDemandSensor;
import org.sensorhub.impl.sensor.uas.UasSensor;

/**
 * Configuration class for the {@link UasOnDemandSensor} class. This differs from the configuration of the
 * {@link UasSensor} class only that it adds information about the video frame size and codec.
 */
public class UasOnDemandConfig extends UasConfig{
	public VideoConfig video = new VideoConfig();
}
