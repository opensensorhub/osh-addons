/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;

import org.sensorhub.api.common.Event;


/**
 * <p><b>Title:</b>
 * ModuleEvent
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Event type generated at various times during a module's lifecycle
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 5, 2013
 */
public class ModuleEvent extends Event
{
    private static final long serialVersionUID = -6428469756344649559L;

    
    public enum Type 
    {
        LOADED,
        DELETED,
        DISABLED,
        ENABLED,
        CONFIG_CHANGE
    }
    
    
    public Type type;
    public IModule<?> moduleInstance;
    public ModuleConfig newConfig;
    
    
    public ModuleEvent(IModule<?> moduleInstance, Type type)
    {
        this.moduleInstance = moduleInstance;
        this.type = type;
    }
    
    
    public ModuleEvent(IModule<?> moduleInstance, ModuleConfig newConfig)
    {
        this.moduleInstance = moduleInstance;
        this.type = Type.CONFIG_CHANGE;
        this.newConfig = newConfig;
    } 
}
