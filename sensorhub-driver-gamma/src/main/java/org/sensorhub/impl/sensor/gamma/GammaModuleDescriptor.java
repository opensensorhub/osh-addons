package org.sensorhub.impl.sensor.gamma;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class GammaModuleDescriptor implements IModuleProvider
{
	@Override
	public String getModuleName()
	{
		return "Model 2070 Gamma Detector";
	}
	
	@Override
	public String getModuleDescription()
	{
		return "Model 2070 Gamma Detector by Health Physics Instruments - reports radiation dose";
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
		return GammaSensor.class;
	}
	
	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
		return GammaConfig.class;
	}
}