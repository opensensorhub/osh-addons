/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.IDataStorage;
import org.sensorhub.api.persistence.IPersistenceManager;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.api.sensor.ISensorInterface;
import org.sensorhub.impl.module.ModuleRegistry;


/**
 * <p><b>Title:</b>
 * PersistenceManagerImpl
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Default implementation of the persistence manager. 
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 15, 2010
 */
public class PersistenceManagerImpl implements IPersistenceManager
{
    
    
    public PersistenceManagerImpl()
    {

    }
    
    
    @Override
    public List<IDataStorage<?,?,?>> getLoadedModules()
    {
        List<IDataStorage<?,?,?>> enabledStorages = new ArrayList<IDataStorage<?,?,?>>();
        
        // retrieve all modules implementing ISensorInterface
        for (IModule<?> module: ModuleRegistry.getInstance().getLoadedModules())
        {
            if (module instanceof ISensorInterface)
                enabledStorages.add((IDataStorage<?,?,?>)module);
        }
        
        return enabledStorages;
    }


    @Override
    public List<ModuleConfig> getAvailableModules()
    {
        List<ModuleConfig> configuredSensors = new ArrayList<ModuleConfig>();
        
        // retrieve all modules implementing ISensorInterface
        for (ModuleConfig config: ModuleRegistry.getInstance().getAvailableModules())
        {
            try
            {
                if (Class.forName(config.moduleClass).isInstance(ISensorInterface.class))
                    configuredSensors.add(config);
            }
            catch (Exception e)
            {
            }
        }
        
        return configuredSensors;
    }


    @Override
    public IDataStorage<?,?,?> getModuleById(String moduleID)
    {
        IModule<?> module = ModuleRegistry.getInstance().getModuleById(moduleID);
        
        if (module instanceof IDataStorage<?,?,?>)
            return (IDataStorage<?,?,?>)module;
        else
            return null;
    }


    @Override
    public void destroyStorage(String storageId, boolean deleteAllData) throws StorageException
    {
        IDataStorage<?,?,?> storage = (IDataStorage<?,?,?>)ModuleRegistry.getInstance().getModuleById(storageId);
        storage.close();
        if (deleteAllData)
            storage.cleanup();       
    }  
}
