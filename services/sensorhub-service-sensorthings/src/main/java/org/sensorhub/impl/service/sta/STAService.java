/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.IHistoricalObsDatabase;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.service.HttpServer;
import com.google.common.collect.Sets;
import de.fraunhofer.iosb.ilt.frostserver.http.common.ServletV1P0;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import static de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings.*;
import static de.fraunhofer.iosb.ilt.frostserver.settings.PersistenceSettings.*;


/**
 * <p>
 * Implementation of SensorThings API service.
 * This service is automatically configured from information obtained
 * from the selected data sources (sensors, storages, processes, etc).
 * </p>
 *
 * @author Alex Robin
 * @since Sep 6, 2019
 */
public class STAService extends AbstractModule<STAServiceConfig> implements IServiceModule<STAServiceConfig>, IEventListener
{
    static final String SERVICE_INSTANCE_ID = "oshServiceId";
    static final HashMap<Integer, STAService> serviceInstances = new HashMap<>(); // static map needed to get access to service from persistence manager
    static final String DEFAULT_GROUP_UID = "urn:osh:sta";
    
    ServletV1P0 servlet;
    IHistoricalObsDatabase obsDatabase;
    
    
    @Override
    public void requestStart() throws SensorHubException
    {
        if (canStart())
        {
            HttpServer httpServer = HttpServer.getInstance();
            if (httpServer == null)
                throw new SensorHubException("HTTP server module is not loaded");
            
            // subscribe to server lifecycle events
            httpServer.registerListener(this);
            
            // we actually start in the handleEvent() method when
            // a STARTED event is received from HTTP server
        }
    }
    
    
    @Override
    public void setConfiguration(STAServiceConfig config)
    {
        super.setConfiguration(config);
        this.securityHandler = new STASecurity(this, config.security.enableAccessControl);
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        serviceInstances.put(System.identityHashCode(this), this);
        
        /*// register group
        if (getProcedureGroupUID() != null)
        {
            AbstractProcess procGroup = SMLHelper.createSimpleProcess(getProcedureGroupUID()).getDescription();
            procGroup.setName(config.virtualSensorGroup.name);
            procGroup.setDescription(config.virtualSensorGroup.description);
            getParentHub().getProcedureRegistry().register(new VirtualSensorProxy(procGroup));
        }*/
        
        if (config.databaseID != null)
            initDatabase();
        
        // deploy servlet
        servlet = new ServletV1P0();
        deploy();
        
        setState(ModuleState.STARTED);
    }
    
    
    protected String getProcedureGroupUID()
    {
        if (config.virtualSensorGroup != null)
            return config.virtualSensorGroup.uid;
        else
            return DEFAULT_GROUP_UID;        
    }
    
    
    protected IHistoricalObsDatabase getDatabase()
    {
        return obsDatabase;
    }
    
    
    protected void initDatabase()
    {
        // retrieve handle of database used for writing
        try
        {
            obsDatabase = (IHistoricalObsDatabase)getParentHub().getModuleRegistry().getModuleById(config.databaseID);
            Set<String> wildcardUID = Sets.newHashSet(getProcedureGroupUID()+"*");
            getParentHub().getDatabaseRegistry().register(wildcardUID, obsDatabase);
        }
        catch (SensorHubException e)
        {
            throw new IllegalArgumentException("Cannot find STA database", e);
        }
    }
    
    
    @Override
    public void stop()
    {
        // undeploy servlet
        undeploy();
        servlet = null;
        
        serviceInstances.remove(System.identityHashCode(this));
        
        setState(ModuleState.STOPPED);
    }
   
    
    protected void deploy() throws SensorHubException
    {
        HttpServer httpServer = HttpServer.getInstance();
        if (httpServer == null || !httpServer.isStarted())
            throw new SensorHubException("An HTTP server instance must be started");
        
        Properties staSettings = new Properties();
        staSettings.setProperty(TAG_SERVICE_ROOT_URL, "http://localhost:8181/sensorhub/sta");
        staSettings.setProperty(TAG_TEMP_PATH, "/tmp");
        staSettings.setProperty(PREFIX_PERSISTENCE+TAG_IMPLEMENTATION_CLASS, OSHPersistenceManager.class.getCanonicalName());
        staSettings.setProperty(PREFIX_PERSISTENCE+SERVICE_INSTANCE_ID, Integer.toString(System.identityHashCode(this)));
        //staSettings.setProperty(TAG_USE_ABSOLUTE_NAVIGATION_LINKS, "false");
            
        // deploy ourself to HTTP server
        httpServer.deployServlet(servlet, config.endPoint + "/v1.0/*");
        servlet.getServletContext().setAttribute(TAG_CORE_SETTINGS, new CoreSettings(staSettings));
        httpServer.addServletSecurity(config.endPoint, config.security.requireAuth);
    }
    
    
    protected void undeploy()
    {
        HttpServer httpServer = HttpServer.getInstance();
        
        // return silently if HTTP server missing on stop
        if (httpServer == null || !httpServer.isStarted())
            return;
        
        httpServer.undeployServlet(servlet);
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
        // stop listening to http server events
        HttpServer httpServer = HttpServer.getInstance();
        if (httpServer != null)
            httpServer.unregisterListener(this);
        
        // unregister security handler
        if (securityHandler != null)
            securityHandler.unregister();
    }
    
    
    @Override
    public void handleEvent(Event e)
    {
        // catch HTTP server lifecycle events
        if (e instanceof ModuleEvent && e.getSource() == HttpServer.getInstance())
        {
            ModuleState newState = ((ModuleEvent) e).getNewState();
            
            // start when HTTP server is enabled
            if (newState == ModuleState.STARTED)
            {
                try
                {
                    start();
                }
                catch (Exception ex)
                {
                    reportError("SensorThings API Service could not start", ex);
                }
            }
            
            // stop when HTTP server is disabled
            else if (newState == ModuleState.STOPPED)
                stop();
        }
    }
    
    
    public STASecurity getSecurityHandler()
    {
        return (STASecurity)securityHandler;
    }
}
