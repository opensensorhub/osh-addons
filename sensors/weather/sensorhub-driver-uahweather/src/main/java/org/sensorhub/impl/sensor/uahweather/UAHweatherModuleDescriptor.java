package org.sensorhub.impl.sensor.uahweather;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class UAHweatherModuleDescriptor implements IModuleProvider
{
	@Override
	public String getModuleName()
	{
		return "UAH Home Weather Station";
	}
	
	@Override
	public String getModuleDescription()
	{
		return "UAH Home Weather Station - Arduino Sensor Collection";
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
		return UAHweatherSensor.class;
	}
	
	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
		return UAHweatherConfig.class;
	}
}