
package org.sensorhub.impl.sensor.ahrs;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class AHRSModuleDescriptor implements IModuleProvider 
{

	@Override
	public String getModuleName() 
	{
//		System.out.println("In AHRSModuleDescriptor ...");

		return "Microstrain Attitude & Heading Reference System - AHRS";
	}

	@Override
	public String getModuleDescription() {
		// TODO Auto-generated method stub
		return "AHRS measures attitude";
	}

	@Override
	public String getModuleVersion() {
		// TODO Auto-generated method stub
		return "0.1";
	}

	@Override
	public String getProviderName() {
		// TODO Auto-generated method stub
		return "Botts Innovative Research";
	}

	@Override
	public Class<? extends IModule<?>> getModuleClass() {
		// TODO Auto-generated method stub
		return AHRSSensor.class;
	}

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass() {
		// TODO Auto-generated method stub
		return AHRSConfig.class;
	}

}
