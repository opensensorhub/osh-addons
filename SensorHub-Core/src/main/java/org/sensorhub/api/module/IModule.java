/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;

import org.sensorhub.api.common.SensorHubException;


/**
 * <p><b>Title:</b>
 * IModule
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Generic interface for all modules in the system.
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 12, 2010
 */
public interface IModule<ConfigType extends ModuleConfig>
{    
    /**
     * Initializes the module with the specified configuration
     * @param config
     */
    public void init(ConfigType config) throws SensorHubException;
    
    
    /**
     * Updates the module's configuration dynamically
     * The module must honor this new configuration unless an error is detected
     * @param config
     */
    public void updateConfig(ConfigType config) throws SensorHubException;
    
    
    /**
     * Retrieves a copy of the module configuration
     * (i.e. for reading only since changes won't have any effect until updateConfig is called)
     * @return a copy of the configuration object associated to this module
     */
    public ConfigType getConfiguration();
    
    
    /**
     * Helper method to get the module's name
     * @return
     */
    public String getName();
    
    
    /**
     * Helper method to get the module's local ID
     * @return
     */
    public String getLocalID();
    
    
    /**
     * Cleans up all ressources used by the module when deleted
     * @throws SensorHubException
     */
    public void cleanup() throws SensorHubException;
           
    
    /**
     * Saves the state of this module to the provided output stream
     * @param saver
     */
    public void saveState(IModuleStateSaver saver) throws SensorHubException;
    
    
    /**
     * Restores the state of this module from info provided by the input stream
     * @param loader
     */
    public void loadState(IModuleStateLoader loader) throws SensorHubException;
}
