package org.sensorhub.impl.sensor.domoticz;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;

public class DomoticzModuleDescriptor implements IModuleProvider
{
    @Override
    public String getModuleName()
    {
        return "Z-Wave Home Network";
    }

    @Override
    public String getModuleDescription()
    {
        return "Driver for home network of Z-Wave sensors";
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
        return DomoticzDriver.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return DomoticzConfig.class;
    }
}
