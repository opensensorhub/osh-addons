package org.sensorhub.impl.sensor.usgswater;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

public class USGSWaterDescriptor extends JarModuleProvider implements IModuleProvider
{

	@Override
	public Class<? extends IModule<?>> getModuleClass()
	{
		return USGSWaterDriver.class;	
	}

	
	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
		return USGSWaterConfig.class;
	}

}
