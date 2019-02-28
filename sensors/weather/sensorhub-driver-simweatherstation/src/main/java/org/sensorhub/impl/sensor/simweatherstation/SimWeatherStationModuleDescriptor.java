package org.sensorhub.impl.sensor.simweatherstation;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class SimWeatherStationModuleDescriptor implements IModuleProvider
{
	@Override
	public String getModuleName()
	{
		return "Simulate dWeather Station";
	}
	
	@Override
	public String getModuleDescription()
	{
		return "Simulated Arduino Sensor Weather Station";
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
		return SimWeatherStationSensor.class;
	}
	
	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
		return SimWeatherStationConfig.class;
	}
}