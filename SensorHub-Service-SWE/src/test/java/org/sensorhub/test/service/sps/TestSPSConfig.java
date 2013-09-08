/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.UUID;
import org.junit.Test;
import org.sensorhub.impl.module.ModuleConfigDatabaseJson;
import org.sensorhub.impl.service.sps.SPSServiceConfig;
import org.sensorhub.impl.service.sps.SPSService;


public class TestSPSConfig
{
    
    @Test
    public void testAddToJsonDatabase() throws Exception
    {
        File configFolder = new File("junittest/");
        configFolder.mkdirs();        
        ModuleConfigDatabaseJson db = new ModuleConfigDatabaseJson(configFolder.getAbsolutePath());
        
        SPSServiceConfig config = new SPSServiceConfig();
        config.id = UUID.randomUUID().toString();
        config.name = "SPS Service #1";
        config.moduleClass = SPSService.class.getCanonicalName();
        config.enabled = true;
        config.enableSOAP = false;
        config.ogcCapabilitiesInfo.title = "SensorHub SPS Service";
        config.ogcCapabilitiesInfo.description = "An SPS service deployed on SensorHub that allows to control sensors";
        config.ogcCapabilitiesInfo.keywords = new String[] {"OGC", "sensors", "control"};
        config.ogcCapabilitiesInfo.contact.setCity("Toulouse");
        db.add(config);
        
        // display stored file
        for (File f: configFolder.listFiles())
        {
            // print out file
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            while ((line = reader.readLine()) != null)
                System.out.println(line);
                        
            // delete file
            f.delete();
        }
        
        configFolder.delete();
    }
}
