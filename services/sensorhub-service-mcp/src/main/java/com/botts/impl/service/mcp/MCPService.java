package com.botts.impl.service.mcp;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.botts.impl.service.mcp.tools.ModuleTool;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.impl.service.mfapi.home.MFCollectionsHandler;
import org.sensorhub.impl.service.mfapi.home.HomePageHandler;
import org.sensorhub.impl.service.mfapi.home.MFCollectionItemsHandler;
import org.sensorhub.impl.service.mfapi.mf.MFHandler;
import org.sensorhub.impl.service.mfapi.mf.TemporalGeomHandler;
import org.sensorhub.impl.service.mfapi.mf.TemporalPropHandler;
import org.sensorhub.impl.service.consys.ObsSystemDbWrapper;
import org.sensorhub.impl.service.consys.RestApiService;
import org.sensorhub.impl.service.consys.home.ConformanceHandler;
import org.sensorhub.utils.ModuleUtils;
import org.sensorhub.utils.NamedThreadFactory;
import com.google.common.collect.ImmutableSet;

import javax.servlet.http.HttpServlet;


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
public class MCPService extends AbstractHttpServiceModule<MCPServiceConfig>
{
    ScheduledExecutorService threadPool;
    HttpServletStreamableServerTransportProvider transportProvider;

    @Override
    public void setConfiguration(MCPServiceConfig config)
    {
        super.setConfiguration(config);
        this.securityHandler = new MCPSecurity(this, config.security.enableAccessControl);
    }

    private HttpServletStreamableServerTransportProvider createTransportProvider() {
        return HttpServletStreamableServerTransportProvider.builder()
//                .jsonMapper()
//                .contextExtractor()
//                .mcpEndpoint()
                .keepAliveInterval(Duration.ofSeconds(1))
                .build();
    }

    @Override
    protected void doStart() throws SensorHubException
    {
        transportProvider = createTransportProvider();

        McpAsyncServer asyncServer = McpServer.async(transportProvider)
                .serverInfo("OpenSensorHub", ModuleUtils.getModuleInfo(MCPService.class).getModuleVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(false, true)
                        .tools(true)
                        .prompts(true)
                        .logging()
                        .completions()
                        .build())
                .build();

        asyncServer.addTool(new ModuleTool().getSpecification());

//        asyncServer.addTool();
    }


    protected void deploy() throws SensorHubException
    {
        var wildcardEndpoint = config.endPoint + "/*";

        httpServer.deployServlet((HttpServlet) transportProvider, wildcardEndpoint);
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
    

    public String getPublicEndpointUrl()
    {
        return getHttpServer().getPublicEndpointUrl(config.endPoint);
    }


    /*public TimeOutMonitor getTimeOutMonitor()
    {
        return timeOutMonitor;
    }*/
}
