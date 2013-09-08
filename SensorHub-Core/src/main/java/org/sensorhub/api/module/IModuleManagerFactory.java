/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;

import java.util.Map;


/**
 * <p><b>Title:</b>
 * IModuleManagerFactory
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Interface for the overall module managers factory. This is a top level structure that has
 * to be called to get instances of module managers.
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 15, 2010
 */
public interface IModuleManagerFactory
{
    /**
     * Gets a reference to the manager instance handling the specified type of module.
     * The default manager is created if non have been started explicitely.
     * @param moduleType
     * @return The concrete manager instance
     */
    public <ModuleType extends IModule<?>> IModuleManager<ModuleType> getManager(Class<ModuleType> moduleType);
    
    
    /**
     * Starts a manager handling the specified type of module and that provides as much functionality
     * specified by the hints table as possible.
     * @param moduleType
     * @param hints
     * @return
     */
    public <ModuleType extends IModule<?>> IModuleManager<ModuleType> startManager(Class<ModuleType> moduleType, Map<String, Object> hints);
}
