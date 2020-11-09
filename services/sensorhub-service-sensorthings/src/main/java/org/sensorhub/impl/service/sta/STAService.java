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
import javax.xml.namespace.QName;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IProcedureObsDatabase;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.procedure.ProcedureId;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.sensor.VirtualProcedureGroupConfig;
import org.sensorhub.impl.service.HttpServer;
import org.vast.ogc.gml.GenericFeature;
import org.vast.ogc.gml.GenericFeatureImpl;
import org.vast.sensorML.SMLHelper;
import com.google.common.base.Strings;
import de.fraunhofer.iosb.ilt.frostserver.http.common.ServletV1P0;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import net.opengis.sensorml.v20.AbstractProcess;
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
    static final String DEFAULT_GROUP_UID = "urn:osh:sta:group";
    static final long HUB_THING_ID = 1;

    IProcedureObsDatabase readDatabase;
    ISTADatabase writeDatabase;
    GenericFeature hubThing;
    ServletV1P0 servlet;
    ProcedureId virtualGroupId;


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
        
        // TODO check config
        if (config.virtualSensorGroup == null)
        {
            config.virtualSensorGroup = new VirtualProcedureGroupConfig();
            config.virtualSensorGroup.uid = DEFAULT_GROUP_UID;
            config.virtualSensorGroup.name = "SensorThings Sensor Group";
            config.virtualSensorGroup.description = "Sensors registered via SensorThings API";
        }
        
        if (Strings.isNullOrEmpty(config.virtualSensorGroup.uid))
            throw new IllegalArgumentException("Virtual Sensor Group UID cannot be null");
        
        this.securityHandler = new STASecurity(this, config.security.enableAccessControl);
    }


    @Override
    public void start() throws SensorHubException
    {
        serviceInstances.put(System.identityHashCode(this), this);
        
        if (config.dbConfig != null)
        {
            // init database
            // TODO load database implementation class dynamically
            writeDatabase = new STADatabase(this, config.dbConfig);
        }
        
        // get existing or create new FilteredView from config
        if (config.exposedResources != null)
            readDatabase = config.exposedResources.getFilteredView(getParentHub());
        else
            readDatabase = getParentHub().getDatabaseRegistry().getFederatedObsDatabase();
        
        // create or retrieve virtual sensor group
        String virtualGroupUID = config.virtualSensorGroup.uid;
        FeatureKey fk;
        if (!writeDatabase.getProcedureStore().contains(virtualGroupUID))
        {
            // register optional group
            AbstractProcess procGroup = new SMLHelper().createPhysicalSystem()
                .uniqueID(virtualGroupUID)
                .name(config.virtualSensorGroup.name)
                .description(config.virtualSensorGroup.description)
                .build();
            
            fk = writeDatabase.getProcedureStore().add(procGroup);
            virtualGroupId = new ProcedureId(fk.getInternalID(), procGroup.getUniqueIdentifier());
        }
        else
            fk = writeDatabase.getProcedureStore().getCurrentVersionKey(virtualGroupUID);
        virtualGroupId = new ProcedureId(fk.getInternalID(), virtualGroupUID);
        
        // create default hub thing
        String uid = getProcedureGroupID().getUniqueID() + ":thing:hub";
        hubThing = new GenericFeatureImpl(new QName("Thing"));
        hubThing.setUniqueIdentifier(uid);
        hubThing.setName(config.hubThing.name);
        hubThing.setDescription(config.hubThing.description);
        writeDatabase.getThingStore().put(new FeatureKey(1, FeatureKey.TIMELESS), hubThing);

        // deploy servlet
        servlet = new STAServlet((STASecurity)securityHandler);
        deploy();

        setState(ModuleState.STARTED);
    }


    protected ProcedureId getProcedureGroupID()
    {
        return virtualGroupId;
    }


    @Override
    public void stop()
    {
        // undeploy servlet
        if (servlet != null)
        {
            undeploy();
            servlet = null;
        }

        // close database
        if (writeDatabase != null)
        {
            writeDatabase.close();
            writeDatabase = null;
        }

        serviceInstances.remove(System.identityHashCode(this));
        setState(ModuleState.STOPPED);
    }


    protected void deploy() throws SensorHubException
    {
        HttpServer httpServer = HttpServer.getInstance();
        if (httpServer == null || !httpServer.isStarted())
            throw new SensorHubException("An HTTP server instance must be started");

        Properties staSettings = new Properties();
        staSettings.setProperty(TAG_SERVICE_ROOT_URL, config.getPublicEndpoint());
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
