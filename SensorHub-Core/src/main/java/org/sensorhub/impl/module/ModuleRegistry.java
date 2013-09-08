/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.IEventProducer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.module.IModuleManager;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.common.BasicEventHandler;



/**
 * <p><b>Title:</b>
 * ModuleRegistry
 * </p>
 *
 * <p><b>Description:</b><br/>
 * This class is in charge of loading all configured modules on startup
 * as well as dynamically loading/unloading modules on demand.
 * It also keeps lists of all loaded and available modules.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin
 * @date Sep 2, 2013
 */
public class ModuleRegistry implements IModuleManager<IModule<?>>, IEventProducer
{
    private static final Log log = LogFactory.getLog(ModuleRegistry.class);    
    private static ModuleRegistry instance;
    
    IModuleConfigRepository configRepos;
    List<String> availableModules;
    Map<String, IModule<?>> loadedModules;
    IEventHandler eventHandler;
    
    
    public static ModuleRegistry create(IModuleConfigRepository configRepos)
    {
        if (instance == null)
            instance = new ModuleRegistry(configRepos);
        
        return instance;
    }
    
    
    public static ModuleRegistry getInstance()
    {
        return instance;
    }
    
    
    /*
     * Singleton constructor
     */
    private ModuleRegistry(IModuleConfigRepository configRepos)
    {
        this.configRepos = configRepos;
        this.availableModules = new ArrayList<String>();
        this.loadedModules = new LinkedHashMap<String, IModule<?>>();
        
        this.eventHandler = new BasicEventHandler();
    }
    
    
    /**
     * Loads all enabled modules from configuration entries provided
     * by the specified IModuleConfigRepository
     */
    public synchronized void loadAllModules()
    {
        List<ModuleConfig> moduleConfs = configRepos.getAllModulesConfigurations();
        for (ModuleConfig config: moduleConfs)
        {
            try
            {
                availableModules.add(config.id);
                if (config.enabled)
                    loadModule(config);
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }
    }
    
    
    /**
     * Instantiates one module using the given configuration
     * @param config Configuration class to use to instantiate the module
     * @return
     */
    @SuppressWarnings("rawtypes")
    public synchronized IModule<?> loadModule(ModuleConfig config) throws SensorHubException
    {        
        if (config.id != null && loadedModules.containsKey(config.id))
            return loadedModules.get(config.id);
        
        try
        {
            // generate a new ID if non is provided
            if (config.id == null)
                config.id = UUID.randomUUID().toString();
            
            // instantiate and init module class
            Class<IModule> clazz = (Class<IModule>)Class.forName(config.moduleClass);
            IModule module = clazz.newInstance();
            module.init(config);
            
            // keep track of what modules are loaded
            loadedModules.put(config.id, module);
            
            // send event
            eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.LOADED));
            
            return module;
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error while instantiating module " + config.name, e);
        }
    }
    
    
    /**
     * Enables the module with the given id
     * @param moduleID Local ID of module to enable
     * @return
     */
    public synchronized IModule<?> enableModule(String moduleID) throws SensorHubException
    {
        checkID(moduleID);
        IModule<?> module;
        
        ModuleConfig config = configRepos.get(moduleID);
        if (!config.enabled)
        {
            config.enabled = true;
            module = loadModule(config);
        }
        else
        {
            module = loadedModules.get(moduleID);
        }
        
        eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.ENABLED));
        return module;
    }
    
    
    /**
     * Disables the module with the given id
     * @param moduleID Local ID of module to disable
     */
    public synchronized void disableModule(String moduleID) throws SensorHubException
    {
        checkID(moduleID);
        
        IModule<?> module = loadedModules.get(moduleID);
        module.getConfiguration().enabled = false;
        
        eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.DISABLED));
    }
    
    
    /**
     * Removes the module with the given id
     * @param moduleID Local ID of module to delete
     */
    public synchronized void destroyModule(String moduleID) throws SensorHubException
    {
        checkID(moduleID);
        
        IModule<?> module = loadedModules.remove(moduleID);
        module.cleanup();
        
        configRepos.remove(moduleID);
        
        eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.DELETED));
    }
    
    
    /**
     * Save all modules current configuration to the repository
     */
    public synchronized void saveModulesConfiguration()
    {
        for (IModule<?> module: loadedModules.values())
            configRepos.update(module.getConfiguration());
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.sensorhub.api.module.IModuleManager#getLoadedModules()
     */
    @Override
    public synchronized List<IModule<?>> getLoadedModules()
    {
        List<IModule<?>> moduleList = new ArrayList<IModule<?>>();
        moduleList.addAll(loadedModules.values());
        return moduleList;
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.sensorhub.api.module.IModuleManager#getModuleById(java.lang.String)
     */
    @Override
    public IModule<?> getModuleById(String moduleID)
    {
        checkID(moduleID);
        return loadedModules.get(moduleID);
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.sensorhub.api.module.IModuleManager#getAvailableModules()
     */
    @Override
    public synchronized List<ModuleConfig> getAvailableModules()
    {
        return configRepos.getAllModulesConfigurations();
    }
    
    
    /**
     * Retrieves list of all installed module types
     * @return
     */
    public List<IModuleProvider> getInstalledModuleTypes()
    {
        List<IModuleProvider> installedModules = new ArrayList<IModuleProvider>();
        
        ServiceLoader<IModuleProvider> sl = ServiceLoader.load(IModuleProvider.class);
        for (IModuleProvider provider: sl)
            installedModules.add(provider);
        
        return installedModules;
    }
    
    
    /*
     * Checks if module id exists in registry
     */
    private void checkID(String moduleID)
    {
        if (!availableModules.contains(moduleID))
            throw new RuntimeException("Module with ID " + moduleID + " is not available");
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);        
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);        
    }

}
