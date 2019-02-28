package org.sensorhub.impl.sensor.vaisala;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class VaisalaWeatherModuleDescriptor implements IModuleProvider
{
	@Override
	public String getModuleName()
	{
		return "Vaisala Weather";
	}
	
	@Override
	public String getModuleDescription()
	{
		return "Vaisala WXT520 Weather Transmitter measuring temperature, wind speed, wind direction, relative humidity, rain amount";
	}
	
	@Override
	public String getModuleVersion()
	{
		return "0.1";
	}
	
	@Override
	public String getProviderName()
	{
		return "Botts Innovative Research, Inc.";
	}
	
	@Override
	public Class<? extends IModule<?>> getModuleClass()
	{
		return VaisalaWeatherSensor.class;
	}
	
	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
		return VaisalaWeatherConfig.class;
	}
}