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


/**
 * <p><b>Title:</b>
 * BasicStorageModuleDescriptor
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Descriptor of PERST based basic storage module.
 * This is needed for automatic discovery by the ModuleRegistry.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 7, 2013
 */
public class BasicStorageModuleDescriptor implements IModuleProvider
{

    @Override
    public String getModuleTypeName()
    {
        return "PERST Basic Storage";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return BasicStorageImpl.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return BasicStorageConfig.class;
    }

}
