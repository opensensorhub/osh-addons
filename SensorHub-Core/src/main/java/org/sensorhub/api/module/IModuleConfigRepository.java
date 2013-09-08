/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;

import java.util.List;


/**
 * <p><b>Title:</b>
 * IModuleConfigDatabase
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Interface for the module configuration repository.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 3, 2013
 */
public interface IModuleConfigRepository
{
    
    /**
     * Retrieves the list of all modules configuration
     * @return
     */
    public List<ModuleConfig> getAllModulesConfigurations();
    
    
    /**
     * Retrieves the configuration of the module with the given id
     * @param moduleID
     * @return
     */
    public ModuleConfig get(String moduleID);
    
    
    /**
     * Adds a module configuration to the repository
     * An exception will be thrown if a module with the same id already exist
     * @param config
     */
    public void add(ModuleConfig config);
    
    
    /**
     * Updates the module configuration with the given id in the repository
     * If the id does not exist, the configuration entry is added with the add method
     * @param config
     */
    public void update(ModuleConfig newConfig);
    
    
    /**
     * Removes the module configuration with the given id from the repository
     * @param moduleID
     */
    public void remove(String moduleID);
    
}
