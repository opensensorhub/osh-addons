
package org.sensorhub.impl.sensor.ahrs;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


public class AHRSModuleDescriptor extends JarModuleProvider implements IModuleProvider 
{
	@Override
	public Class<? extends IModule<?>> getModuleClass()
	{
		return AHRSSensor.class;
	}
	

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
		return AHRSConfig.class;
	}
}
