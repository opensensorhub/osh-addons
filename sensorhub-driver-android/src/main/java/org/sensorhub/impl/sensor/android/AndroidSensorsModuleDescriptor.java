/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.android;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;


/**
 * <p><b>Title:</b>
 * AndroidSensorsModuleDescriptor
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Descriptor of Android sensors driver module for automatic discovery
 * by the ModuleRegistry
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 7, 2013
 */
public class AndroidSensorsModuleDescriptor implements IModuleProvider
{

    @Override
    public String getModuleTypeName()
    {
        return "Android Sensors Driver";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return AndroidSensorsDriver.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return AndroidSensorsConfig.class;
    }

}
