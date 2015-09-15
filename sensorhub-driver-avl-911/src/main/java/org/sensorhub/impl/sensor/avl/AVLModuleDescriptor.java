package org.sensorhub.impl.sensor.avl;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class AVLModuleDescriptor implements IModuleProvider
{

	@Override
	public String getModuleName()
	{
		return "AVL-911";
	}

	@Override
	public String getModuleDescription()
	{
		return "Automatic Vehicle Location providing location and status based on Intergraph's 911 System";
	}

	@Override
	public String getModuleVersion()
	{
		return "0.1";
	}

	@Override
	public String getProviderName()
	{
		return "Botts Innovative Research Inc";
	}

	@Override
	public Class<? extends IModule<?>> getModuleClass()
	{
		return AVLDriver.class;
	}

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
		return AVLConfig.class;
	}

}
