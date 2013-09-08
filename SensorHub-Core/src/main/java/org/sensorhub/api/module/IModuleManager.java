/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;

import java.util.List;


/**
 * <p><b>Title:</b>
 * IModuleManager
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Base interface for all module managers.
 * Module managers are used to manage all modules of the same type
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 12, 2010
 */
public interface IModuleManager<ModuleType extends IModule<?>>
{
    /**
     * Gets the list of all modules handled by this manager
     * that are already loaded (i.e. enabled)
     * @return
     */
    public List<ModuleType> getLoadedModules();
    
    
    /**
     * Gets the list of all modules handled by this manager
     * that are configured but not yet loaded (i.e. disabled)
     * @return
     */
    public List<ModuleConfig> getAvailableModules();
    
    
    /**
     * Retrieves a module instance by its local ID
     * @param moduleID Id of module to retrieve
     * @return direct reference to the module instance
     */
    public ModuleType getModuleById(String moduleID);

}
