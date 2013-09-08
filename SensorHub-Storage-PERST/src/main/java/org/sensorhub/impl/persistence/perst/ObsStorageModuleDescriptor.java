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
 * ObsStorageModuleDescriptor
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Descriptor of PERST observation storage module.
 * This is needed for automatic discovery by the ModuleRegistry
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 7, 2013
 */
public class ObsStorageModuleDescriptor implements IModuleProvider
{

    @Override
    public String getModuleTypeName()
    {
        return "PERST Observation Storage";
    }


    @Override
    public Class<? extends IModule<?>> getModuleClass()
    {
        return ObservationStorageImpl.class;
    }


    @Override
    public Class<? extends ModuleConfig> getModuleConfigClass()
    {
        return ObsStorageConfig.class;
    }

}
