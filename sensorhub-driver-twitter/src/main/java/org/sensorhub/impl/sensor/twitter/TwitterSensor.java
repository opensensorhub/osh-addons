package org.sensorhub.impl.sensor.twitter;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

/**
 * <p>
 * Implementation of Twitter streaming API. This particular class stores 
 * configuration parameters.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since March 24, 2017
 */
public class TwitterSensor extends AbstractSensorModule<TwitterConfig> 
{
	public final String urn = "urn:osh:sensor:twitter";
	public final String xmlID = "TWITTER_STREAM_";
	
	TwitterOutput dataInterface;
	
	public TwitterSensor() {}
	
	@Override
	public void setConfiguration(final TwitterConfig config)
	{
		super.setConfiguration(config);
	}

	@Override
	public void init() throws SensorHubException
	{
		super.init();
		
		// generate identifiers
		generateUniqueID(urn, config.streamID);
		generateXmlID(xmlID, config.streamID);
		
		// init main data interface
		dataInterface = new TwitterOutput(this);
		addOutput(dataInterface, false);
		dataInterface.init();
	}
	
	@Override
	public String getName() {
		return "Twitter_Sensor";
	}

	@Override
	public boolean isConnected() 
	{
		return dataInterface.isConnected();
	}
	
	@Override
	public void start() throws SensorHubException
	{
		try {
			dataInterface.start(config);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void stop()
	{
		dataInterface.stop();
	}
}
