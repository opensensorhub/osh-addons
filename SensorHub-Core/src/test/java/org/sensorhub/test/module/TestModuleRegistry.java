/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.module;

import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.impl.module.ModuleConfigDatabaseJson;
import org.sensorhub.impl.module.ModuleRegistry;


public class TestModuleRegistry
{
    File configFolder;
    ModuleConfigDatabaseJson configDb;
    ModuleRegistry registry;
    
    
    @Before
    public void setup()
    {
        configFolder = new File("test/");
        configFolder.mkdirs();
        configDb = new ModuleConfigDatabaseJson(configFolder.getAbsolutePath());
        
        registry = ModuleRegistry.create(configDb);
    }
    
    
    @Test
    public void testGetInstalledModuleTypes()
    {
        for (IModuleProvider moduleType: registry.getInstalledModuleTypes())
        {
            System.out.println(moduleType.getModuleTypeName() + ": " +
                               moduleType.getModuleClass().getCanonicalName());
        }        
    }
    
    

    @Test
    public void testLoadModule()
    {
        
    }
    
    
    @After
    public void cleanup()
    {
        for (File f: configFolder.listFiles())
            f.delete();
        configFolder.delete();
    }
}
