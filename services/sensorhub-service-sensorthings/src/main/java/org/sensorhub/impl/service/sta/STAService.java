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
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.api.system.SystemId;
import org.sensorhub.impl.database.registry.FilteredFederatedObsDatabase;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.vast.ogc.gml.GenericFeatureImpl;
import org.vast.sensorML.SMLHelper;
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
public class STAService extends AbstractHttpServiceModule<STAServiceConfig> implements IServiceModule<STAServiceConfig>, IEventListener
{
    static final String SERVICE_INSTANCE_ID = "oshServiceId";
    static final HashMap<Integer, STAService> serviceInstances = new HashMap<>(); // static map needed to get access to service from persistence manager
    static final String DEFAULT_GROUP_UID = "urn:osh:sta:group";
    static final long HUB_THING_ID = 1;

    IObsSystemDatabase readDatabase;
    ISTADatabase writeDatabase;
    GenericFeatureImpl hubThing;
    ServletV1P0 servlet;
    SystemId virtualGroupId;
    STAMqttConnector mqttConnector;


    @Override
    public void setConfiguration(STAServiceConfig config)
    {
        super.setConfiguration(config);
        this.securityHandler = new STASecurity(this, config.security.enableAccessControl);
    }


    @Override
    protected void doStart() throws SensorHubException
    {
        serviceInstances.put(System.identityHashCode(this), this);
        
        if (config.dbConfig != null)
        {
            // init database
            writeDatabase = new STADatabase(this, config.dbConfig);
        }
        
        // get filter for FilteredView from config
        var filter = config.exposedResources.getObsFilter();
        if (writeDatabase != null)
        {
            // if a writable database was provided, make sure we always expose
            // its content via this service by flagging it as unfiltered
            readDatabase = new FilteredFederatedObsDatabase(
                getParentHub().getDatabaseRegistry(),
                (ObsFilter)filter, writeDatabase.getDatabaseNum());
        }
        else
            readDatabase = config.exposedResources.getFilteredView(getParentHub());
        
        // create or retrieve virtual sensor group
        if (config.virtualSensorGroup != null)
        {
            String virtualGroupUID = config.virtualSensorGroup.uid;
            FeatureKey fk;
            if (!writeDatabase.getSystemDescStore().contains(virtualGroupUID))
            {
                // register optional group
                AbstractProcess procGroup = new SMLHelper().createPhysicalSystem()
                    .uniqueID(virtualGroupUID)
                    .name(config.virtualSensorGroup.name)
                    .description(config.virtualSensorGroup.description)
                    .build();
                
                fk = writeDatabase.getSystemDescStore().add(new SystemWrapper(procGroup));
                virtualGroupId = new SystemId(fk.getInternalID(), procGroup.getUniqueIdentifier());
            }
            else
                fk = writeDatabase.getSystemDescStore().getCurrentVersionKey(virtualGroupUID);
            virtualGroupId = new SystemId(fk.getInternalID(), virtualGroupUID);
        }
        else
            virtualGroupId = new SystemId(1, "urn:osh:sta");
        
        // create default hub thing
        String uid = getSystemGroupID().getUniqueID() + ":thing:hub";
        hubThing = new GenericFeatureImpl(new QName("Thing"));
        hubThing.setUniqueIdentifier(uid);
        hubThing.setName(config.hubThing.name);
        hubThing.setDescription(config.hubThing.description);
        writeDatabase.getThingStore().put(new FeatureKey(1, FeatureKey.TIMELESS), hubThing);

        // deploy servlet
        servlet = new STAServlet(this);
        deploy();

        setState(ModuleState.STARTED);
    }


    protected void deploy() throws SensorHubException
    {
        Properties staSettings = new Properties();
        staSettings.setProperty(TAG_SERVICE_ROOT_URL, httpServer.getPublicEndpointUrl(config.endPoint));
        staSettings.setProperty(TAG_TEMP_PATH, "/tmp");
        staSettings.setProperty(PREFIX_PERSISTENCE+TAG_IMPLEMENTATION_CLASS, OSHPersistenceManager.class.getCanonicalName());
        staSettings.setProperty(PREFIX_PERSISTENCE+SERVICE_INSTANCE_ID, Integer.toString(System.identityHashCode(this)));
        //staSettings.setProperty(TAG_USE_ABSOLUTE_NAVIGATION_LINKS, "false");

        // deploy ourself to HTTP server
        var endpoint = getEndPoint();
        var wildcardEndpoint = endpoint + "*";
        httpServer.deployServlet(servlet, wildcardEndpoint);
        httpServer.addServletSecurity(wildcardEndpoint, config.security.requireAuth);
        var coreSettings = new CoreSettings(staSettings);
        servlet.getServletContext().setAttribute(TAG_CORE_SETTINGS, coreSettings);
        
        // also enable MQTT extension if an MQTT server is available
        if (config.enableMqtt)
        {
            getParentHub().getModuleRegistry().waitForModuleType(IMqttServer.class, ModuleState.STARTED)
                .thenAccept(mqtt -> {
                    if (mqtt != null)
                    {
                        mqttConnector = new STAMqttConnector(this, endpoint, coreSettings);
                        mqtt.registerHandler(endpoint, mqttConnector);
                        getLogger().info("SensorThings MQTT handler registered");
                    }
                });
        }
    }


    @Override
    protected void doStop()
    {
        // undeploy servlet
        undeploy();

        // close database
        if (writeDatabase != null)
        {
            getParentHub().getDatabaseRegistry().unregister(writeDatabase);
            writeDatabase.close();
            writeDatabase = null;
        }

        serviceInstances.remove(System.identityHashCode(this));
        setState(ModuleState.STOPPED);
    }


    protected void undeploy()
    {
        // return silently if HTTP server missing on stop
        if (httpServer == null || !httpServer.isStarted())
            return;
        
        // also stop MQTT extension if it was enabled
        if (mqttConnector != null)
        {
            getParentHub().getModuleRegistry().waitForModuleType(IMqttServer.class, ModuleState.STARTED)
                .thenAccept(mqtt -> {
                    if (mqtt != null)
                    {
                        mqtt.unregisterHandler(getEndPoint(), mqttConnector);
                        getLogger().info("SensorThings MQTT handler unregistered");
                        mqttConnector.stop();
                        mqttConnector = null;
                    }
                });
        }

        if (servlet != null)
        {
            httpServer.undeployServlet(servlet);
            servlet.destroy();
            servlet = null;
        }
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        // unregister security handler
        if (securityHandler != null)
            securityHandler.unregister();
    }
    
    
    protected String getEndPoint()
    {
        return config.endPoint + "/v1.0/";
    }


    protected SystemId getSystemGroupID()
    {
        return virtualGroupId;
    }


    public STASecurity getSecurityHandler()
    {
        return (STASecurity)securityHandler;
    }
}
