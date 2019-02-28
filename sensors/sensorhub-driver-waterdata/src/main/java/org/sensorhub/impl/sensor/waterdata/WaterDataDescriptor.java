package org.sensorhub.impl.sensor.waterdata;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;
import org.sensorhub.impl.sensor.waterdata.WaterDataConfig;
import org.sensorhub.impl.sensor.waterdata.WaterDataDriver;

public class WaterDataDescriptor extends JarModuleProvider implements IModuleProvider
{

	@Override
	public Class<? extends IModule<?>> getModuleClass()
	{
		return WaterDataDriver.class;	
	}

	
	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
		return WaterDataConfig.class;
	}

}
