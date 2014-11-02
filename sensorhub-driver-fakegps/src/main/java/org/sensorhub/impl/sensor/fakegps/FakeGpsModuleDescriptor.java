/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakegps;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;


public class FakeGpsModuleDescriptor implements IModuleProvider
{

    @Override
    public String getModuleName()
    {
        return "Fake GPS Sensor";
    }


    @Override
    public String getModuleDescription()
    {
        return "Sensor outputing fake GPS sensor data generated from Google Directions results";
    }


    @Override
    public String getModuleVersion()
    {
        return "0.1";
    }


    @Override
    public String getProviderName()
    {
        return "Sensia Software LLC";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return FakeGpsSensor.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return FakeGpsConfig.class;
    }

}
