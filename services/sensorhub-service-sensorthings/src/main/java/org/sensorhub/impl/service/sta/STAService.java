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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow.Subscription;
import javax.xml.namespace.QName;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.FeatureKey;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.procedure.IProcedureRegistry;
import org.sensorhub.api.procedure.ProcedureAddedEvent;
import org.sensorhub.api.procedure.ProcedureEnabledEvent;
import org.sensorhub.api.procedure.ProcedureEvent;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.service.HttpServer;
import org.vast.ogc.gml.GenericFeature;
import org.vast.ogc.gml.GenericFeatureImpl;
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
    static final long HUB_THING_ID = 1;

    Subscription procRegistrySub;
    ISTADatabase database;
    GenericFeature hubThing;
    ServletV1P0 servlet;
    Set<Long> exposedProcedureIDs = ConcurrentHashMap.newKeySet();


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

        // fetch internalIDs of exposed procedures
        // and ensure we get notified if procedures are added later
        exposedProcedureIDs.clear();
        subscribeToProcedureRegistryEvents()
            .thenAccept(s -> {
                // keep handle to subscription so we can cancel it later
                procRegistrySub = s;

                // expose all procedures that are already available
                // others will be handled later when added/enabled
                for (String procUID: config.exposedProcedures)
                    addProcedure(procUID);
            });

        /*// register group
        if (getProcedureGroupUID() != null)
        {
            AbstractProcess procGroup = SMLHelper.createSimpleProcess(getProcedureGroupUID()).getDescription();
            procGroup.setName(config.virtualSensorGroup.name);
            procGroup.setDescription(config.virtualSensorGroup.description);
            getParentHub().getProcedureRegistry().register(new VirtualSensorProxy(procGroup));
        }*/

        // init database
        // TODO load database implementation class dynamically
        database = new STADatabase(this, config.dbConfig);

        // create default hub thing
        String uid = getProcedureGroupUID() + ":thing:hub";
        hubThing = new GenericFeatureImpl(new QName("Thing"));
        hubThing.setUniqueIdentifier(uid);
        hubThing.setName(config.hubThing.name);
        hubThing.setDescription(config.hubThing.description);
        database.getThingStore().put(new FeatureKey(1, FeatureKey.TIMELESS), hubThing);

        // deploy servlet
        servlet = new STAServlet((STASecurity)securityHandler);
        deploy();

        setState(ModuleState.STARTED);
    }


    /*
     * Expose new procedure if listed in configuration
     */
    protected void addProcedure(String procUID)
    {
        // get procedure internal ID to speed-up lookups later on
        var federatedProcStore = getParentHub().getDatabaseRegistry().getFederatedObsDatabase().getProcedureStore();
        var key = federatedProcStore.getLatestVersionKey(procUID);
        if (key != null)
            exposedProcedureIDs.add(key.getInternalID());
    }


    /*
     * Check if UID or parent UID was configured to be exposed by service
     */
    protected boolean isProcedureExposed(long publicID)
    {
        // TODO handle wildcard and group member cases

        return exposedProcedureIDs.contains(publicID);// || config.exposedProcedures.contains(parentUid)
    }


    protected boolean isProcedureExposed(String uid)
    {
        // TODO handle wildcard and group member cases

        return config.exposedProcedures.contains(uid);
    }


    protected CompletableFuture<Subscription> subscribeToProcedureRegistryEvents()
    {
        return getParentHub().getEventBus().newSubscription(ProcedureEvent.class)
            .withSourceID(IProcedureRegistry.EVENT_SOURCE_ID)
            .withEventType(ProcedureAddedEvent.class)
            .withEventType(ProcedureEnabledEvent.class)
            .consume(e -> {
                var uid = e.getProcedureID().getUniqueID();
                if (isProcedureExposed(uid))
                    addProcedure(uid);
            });
    }


    protected String getProcedureGroupUID()
    {
        if (config.virtualSensorGroup != null)
            return config.virtualSensorGroup.uid;
        else
            return DEFAULT_GROUP_UID;
    }


    @Override
    public void stop()
    {
        // unsubscribe from procedure registry
        if (procRegistrySub != null)
        {
            procRegistrySub.cancel();
            procRegistrySub = null;
        }

        // undeploy servlet
        if (servlet != null)
        {
            undeploy();
            servlet = null;
        }

        // close database
        if (database != null)
        {
            database.close();
            database = null;
        }

        serviceInstances.remove(System.identityHashCode(this));
        exposedProcedureIDs.clear();

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
