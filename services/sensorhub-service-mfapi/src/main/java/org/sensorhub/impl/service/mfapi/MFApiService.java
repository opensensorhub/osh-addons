/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mfapi;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.impl.service.mfapi.home.MFCollectionsHandler;
import org.sensorhub.impl.service.mfapi.home.HomePageHandler;
import org.sensorhub.impl.service.mfapi.mf.MFHandler;
import org.sensorhub.impl.service.mfapi.mf.TemporalGeomHandler;
import org.sensorhub.impl.service.mfapi.mf.TemporalPropHandler;
import org.sensorhub.impl.service.sweapi.ObsSystemDbWrapper;
import org.sensorhub.impl.service.sweapi.home.ConformanceHandler;
import org.sensorhub.utils.NamedThreadFactory;
import com.google.common.collect.ImmutableSet;


/**
 * <p>
 * Implementation of SensorHub SWE API service.<br/>
 * The service can be configured to expose some or all of the systems and
 * observations available on the hub.
 * </p>
 *
 * @author Alex Robin
 * @since Oct 12, 2020
 */
public class MFApiService extends AbstractHttpServiceModule<MFApiServiceConfig> implements IServiceModule<MFApiServiceConfig>, IEventListener
{
    protected MFApiServlet servlet;
    ScheduledExecutorService threadPool;
    
    static final Set<String> CONF_CLASSES = ImmutableSet.of(
        "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/core",
        "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/html",
        "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/json",
        "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/oas30",
        "http://www.opengis.net/spec/ogcapi-common-2/0.0/conf/collections",
        "http://www.opengis.net/spec/ogcapi-common-2/0.0/conf/html",
        "http://www.opengis.net/spec/ogcapi-common-2/0.0/conf/json",
        
        "http://www.opengis.net/spec/ogcapi-movingfeatures-1/1.0/conf/common",
        "http://www.opengis.net/spec/ogcapi-movingfeatures-1/1.0/conf/mf-collection",
        "http://www.opengis.net/spec/ogcapi-movingfeatures-1/1.0/conf/movingfeatures"
    );


    @Override
    public void setConfiguration(MFApiServiceConfig config)
    {
        super.setConfiguration(config);
        this.securityHandler = new MFApiSecurity(this, config.security.enableAccessControl);
    }


    @Override
    protected void doStart() throws SensorHubException
    {
        // create filtered DB or expose entire federated DB
        IObsSystemDatabase readDb;
        if (config.exposedResources != null)
            readDb = config.exposedResources.getFilteredView(getParentHub());
        else
            readDb = getParentHub().getDatabaseRegistry().getFederatedDatabase();

        // init thread pool
        threadPool = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new NamedThreadFactory("SWAPool"));

        // init timeout monitor
        //timeOutMonitor = new TimeOutMonitor();
        
        var db = new ObsSystemDbWrapper(readDb, null, getParentHub().getIdEncoders());
        var eventBus = getParentHub().getEventBus();
        var security = (MFApiSecurity)this.securityHandler;
        
        // create resource handlers hierarchy
        var homePage = new HomePageHandler(config);
        var rootHandler = new RootHandler(homePage, true);
        rootHandler.addSubResource(new ConformanceHandler(CONF_CLASSES));
        
        var mfHandler = new MFHandler(eventBus, db, security.foi_permissions);
        rootHandler.addSubResource(mfHandler);
        
        var tgeomHandler = new TemporalGeomHandler(eventBus, db, security.foi_permissions);
        mfHandler.addSubResource(tgeomHandler);
        
        var tpropHandler = new TemporalPropHandler(eventBus, db, security.foi_permissions);
        mfHandler.addSubResource(tpropHandler);
        
        // collections
        var collectionHandler = new MFCollectionsHandler(eventBus, db, security.foi_permissions, config.collections);
        rootHandler.addSubResource(collectionHandler);
        
        // deploy servlet
        servlet = new MFApiServlet(this, (MFApiSecurity)securityHandler, rootHandler, getLogger());
        deploy();

        setState(ModuleState.STARTED);
    }


    protected void deploy() throws SensorHubException
    {
        var wildcardEndpoint = config.endPoint + "/*";
        
        // deploy ourself to HTTP server
        httpServer.deployServlet(servlet, wildcardEndpoint);
        httpServer.addServletSecurity(wildcardEndpoint, config.security.requireAuth);
    }


    @Override
    protected void doStop()
    {
        // undeploy servlet
        undeploy();
        
        // stop thread pool
        if (threadPool != null)
            threadPool.shutdown();

        setState(ModuleState.STOPPED);
    }


    protected void undeploy()
    {
        // return silently if HTTP server missing on stop
        if (httpServer == null || !httpServer.isStarted())
            return;

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


    public ScheduledExecutorService getThreadPool()
    {
        return threadPool;
    }
    
    
    public MFApiServlet getServlet()
    {
        return servlet;
    }


    /*public TimeOutMonitor getTimeOutMonitor()
    {
        return timeOutMonitor;
    }*/
}
