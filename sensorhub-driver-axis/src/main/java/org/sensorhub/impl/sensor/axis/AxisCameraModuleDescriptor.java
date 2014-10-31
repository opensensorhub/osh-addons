package org.sensorhub.impl.sensor.axis;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class AxisCameraModuleDescriptor implements IModuleProvider
{

	@Override
	public String getModuleName()
	{
		return "Axis IP Video Camera";
	}

	@Override
	public String getModuleDescription()
	{
		return "Supports access to video and tasking of Pan-Tilt-Zoom gimbal for any Axis video camera using IP protocol for commands and data";
	}

	@Override
	public String getModuleVersion()
	{
		return "0.1";
	}

	@Override
	public String getProviderName()
	{
		return "Botts Innovative Research Inc.";
	}

	@Override
	public Class<? extends IModule<?>> getModuleClass()
	{
		return AxisCameraDriver.class;	
	}

	@Override
	public Class<? extends ModuleConfig> getModuleConfigClass()
	{
	       return AxisCameraConfig.class;
	}

}
