package org.sensorhub.impl.sensor.avl;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


public class AVLModuleDescriptor extends JarModuleProvider implements IModuleProvider
{
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
