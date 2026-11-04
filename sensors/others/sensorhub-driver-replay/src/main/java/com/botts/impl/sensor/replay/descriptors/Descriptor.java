/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.sensor.replay.descriptors;

import com.botts.impl.sensor.replay.ReplaySensor;
import com.botts.impl.sensor.replay.config.ReplayConfig;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.JarModuleProvider;

/**
 * Module descriptor, allows OpenSensorHub to identify and load the module.
 *
 * @author Nicolas Garay
 * @since 3/19/2025
 */
public class Descriptor extends JarModuleProvider implements IModuleProvider
{
    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return ReplaySensor.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return ReplayConfig.class;
    }

}
