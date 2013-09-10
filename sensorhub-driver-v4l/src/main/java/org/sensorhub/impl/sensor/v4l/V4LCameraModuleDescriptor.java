/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.v4l;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;


/**
 * <p><b>Title:</b>
 * V4LCameraModuleDescriptor
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Descriptor of V4L driver module for automatic discovery
 * by the ModuleRegistry
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 7, 2013
 */
public class V4LCameraModuleDescriptor implements IModuleProvider
{

    @Override
    public String getModuleTypeName()
    {
        return "Video4Linux Camera Driver";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return V4LCameraDriver.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return V4LCameraConfig.class;
    }

}
