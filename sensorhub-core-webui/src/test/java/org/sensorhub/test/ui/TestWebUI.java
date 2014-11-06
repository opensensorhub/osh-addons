/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.service.ServiceConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.DummyModule;
import org.sensorhub.impl.module.InMemoryConfigDb;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.ui.AdminUI;
import com.vaadin.server.VaadinServlet;


public class TestWebUI
{
        
    static public void setup() throws Exception
    {
        // instantiate module registry
        ModuleRegistry registry = new ModuleRegistry(setupConfig());
        SensorHub.createInstance(null, registry);
        //registry.loadAllModules();
        
        // start HTTP server
        HttpServer server = HttpServer.getInstance();
        HttpServerConfig config = new HttpServerConfig();
        server.init(config);
        server.start();
        
        // connect to servlet and check response
        URL url = new URL("http://localhost:" + config.httpPort + config.rootURL + "/test");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String resp = reader.readLine();
        System.out.println(resp);
        reader.close();
        
        // deploy Vaadin servlet
        VaadinServlet vaadin = new VaadinServlet();
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("UI", AdminUI.class.getCanonicalName());
        initParams.put("productionMode", "true");
        server.deployServlet("/*", vaadin, initParams);
    }
    
    
    static protected InMemoryConfigDb setupConfig()
    {
        InMemoryConfigDb configDB = new InMemoryConfigDb();
        
        String[] moduleNames = new String[] {"SOS Service", "SPS Service", "Storage1", "Storage2", "Sensor1", "Sensor2"};
        
        for (String name: moduleNames)
        {
            ModuleConfig config;
            
            if (name.contains("Service"))
                config = new ServiceConfig();
            else if (name.contains("Storage"))
                config = new StorageConfig();
            else if (name.contains("Process"))
                config = new ProcessConfig();
            else
                config = new SensorConfig();
                
            config.id = UUID.randomUUID().toString();
            config.name = name;
            config.enabled = true;
            config.moduleClass = DummyModule.class.getCanonicalName();
            configDB.add(config);   
        }
        
        return configDB;
    }
    
    
    public static void main(String[] args) throws Exception
    {
        setup();
        Thread.sleep(1000*3600);
    }
}
