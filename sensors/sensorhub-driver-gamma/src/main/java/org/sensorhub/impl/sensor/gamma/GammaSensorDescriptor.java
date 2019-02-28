package org.sensorhub.impl.sensor.gamma;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


public class GammaSensorDescriptor extends JarModuleProvider implements IModuleProvider
{
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