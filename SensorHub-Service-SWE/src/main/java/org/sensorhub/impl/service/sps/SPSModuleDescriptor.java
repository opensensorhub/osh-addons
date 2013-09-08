/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sps;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;


/**
 * <p><b>Title:</b>
 * SPSModuleDescriptor
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Descriptor of SPS service module, needed for automatic discovery
 * by the ModuleRegistry
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 7, 2013
 */
public class SPSModuleDescriptor implements IModuleProvider
{

    @Override
    public String getModuleTypeName()
    {
        return "SPS Service";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return SPSService.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return SPSServiceConfig.class;
    }

}
