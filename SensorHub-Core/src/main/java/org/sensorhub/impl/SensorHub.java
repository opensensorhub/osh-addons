/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl;

import org.sensorhub.api.config.IGlobalConfig;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.impl.module.ModuleConfigDatabaseJson;
import org.sensorhub.impl.module.ModuleRegistry;


/**
 * <p><b>Title:</b>
 * SensorHub
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Main class reponsible for starting/stopping all modules
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 4, 2013
 */
public class SensorHub
{
    private static SensorHub instance;
    
    private IGlobalConfig config;
    
    
    public static SensorHub getInstance(IGlobalConfig config)
    {
        if (instance == null)
            instance = new SensorHub(config);
        
        return instance;
    }
    
    
    private SensorHub(IGlobalConfig config)
    {
        this.config = config;
    }
    
    
    public void start()
    {
        IModuleConfigRepository configDB = new ModuleConfigDatabaseJson(config.getModuleConfigPath());
        ModuleRegistry.create(configDB);
        
        // load all modules in the order implied by dependency constraints
        
    }
    
    
    public void stop()
    {
        
    }

}
