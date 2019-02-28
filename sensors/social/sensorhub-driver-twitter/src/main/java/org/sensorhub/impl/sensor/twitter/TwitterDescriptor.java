package org.sensorhub.impl.sensor.twitter;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

/**
 * <p>
 * Implementation of Twitter streaming API. This particular class stores 
 * descriptor attributes.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since March 24, 2017
 */
public class TwitterDescriptor extends JarModuleProvider implements IModuleProvider 
{
	@Override
	public String getModuleName() 
	{
		return "Twitter Sensor";
	}

	@Override
	public String getModuleDescription() 
	{
		return "Twitter Sensor via Twitter's API";
	}

	@Override
	public String getModuleVersion() 
	{
		return "0.0.1";
	}

	@Override
	public String getProviderName() 
	{
		return "Twitter";
	}

	@Override
	public Class<? extends IModule<?>> getModuleClass() 
	{
		return TwitterSensor.class;
	}

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass() 
	{
		return TwitterConfig.class;
	}

}
