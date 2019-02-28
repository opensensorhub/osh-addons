package org.sensorhub.impl.sensor.domoticz;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;


public class DomoticzModuleDescriptor extends JarModuleProvider implements IModuleProvider
{
    
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
