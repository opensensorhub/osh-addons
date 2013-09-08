/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.StorageConfig;


public class PerstStorageProvider implements IModuleProvider
{

    @Override
    public String getModuleTypeName()
    {
        return "PERST Generic Storage";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return BasicStorageImpl.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return StorageConfig.class;
    }

}
