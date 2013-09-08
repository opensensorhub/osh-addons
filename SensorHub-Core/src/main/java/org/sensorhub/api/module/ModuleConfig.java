/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;

import java.io.Serializable;
import com.esotericsoftware.kryo.Kryo;


/**
 * <p><b>Title:</b>
 * ModuleConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Base class to hold modules' configuration options
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 16, 2010
 */
public class ModuleConfig implements Serializable, Cloneable
{
    private static final long serialVersionUID = 2267529983474592096L;
    
    
    /**
     * Name of module that this configuration is for
     */
    public String name;
    
    
    /**
     * Unique ID of the module. It must be unique within the SensorHub instance
     * and remain the same during the whole life-time of the module
     */
    public String id;
    
    
    /**
     * Class implementing the module (to be instantiated)
     */
    public String moduleClass;
    
    
    /**
     * Used to enable/disable the module
     */
    public boolean enabled = false;
    
    
    @Override
    public ModuleConfig clone()
    {
        Kryo kryo = new Kryo();
        return kryo.copy(this);
    }
}
