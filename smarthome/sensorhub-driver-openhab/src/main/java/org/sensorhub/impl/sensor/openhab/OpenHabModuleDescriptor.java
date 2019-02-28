package org.sensorhub.impl.sensor.openhab;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.sensor.openhab.OpenHabDriver;

public class OpenHabModuleDescriptor implements IModuleProvider
{
    @Override
    public String getModuleName()
    {
        return "OpenHAB Network";
    }

    @Override
    public String getModuleDescription()
    {
        return "Driver for home network of OpenHAB Z-Wave sensors";
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
        return OpenHabDriver.class;
    }

    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return OpenHabConfig.class;
    }
}
