package org.sensorhub.impl.sensor.isa;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


public class ISADriverDescriptor extends JarModuleProvider implements IModuleProvider
{
	@Override
	public Class<? extends IModule<?>> getModuleClass()
	{
		return ISADriver.class;
	}
	
	
	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
		return ISAConfig.class;
	}
}