package com.georobotix.impl.service.mcp;

import com.georobotix.impl.service.mcp.resources.*;
import com.georobotix.impl.service.mcp.resources.*;
import com.georobotix.impl.service.mcp.tools.DatabaseTool;
import com.georobotix.impl.service.mcp.tools.ModuleTool;
import com.georobotix.impl.service.mcp.tools.ProcessTool;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IFederatedDatabase;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.utils.ModuleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * MCP (Model Context Protocol) Service for OpenSensorHub.
 *
 * Exposes OSH functionality through the MCP protocol, allowing AI assistants
 * and other MCP clients to interact with sensor systems, modules, and data.
 */
public class McpService extends AbstractHttpServiceModule<McpServiceConfig> {

    private static final Logger log = LoggerFactory.getLogger(McpService.class);

    private ScheduledExecutorService threadPool;
    private JavaxMcpStreamableTransport transportProvider;
    private McpAsyncServer mcpServer;

    // Resources
    private ModuleListResource moduleListResource;
    private ModuleDetailResource moduleDetailResource;
    private SystemListResource systemListResource;
    private DataStreamListResource dataStreamListResource;
    private ObservationResource observationResource;

    // Tools
    private ModuleTool moduleTool;
    private DatabaseTool databaseTool;
    private ProcessTool processTool;

    @Override
    public void setConfiguration(McpServiceConfig config) {
        super.setConfiguration(config);
        this.securityHandler = new McpSecurity(this, config.security.enableAccessControl);
    }

    @Override
    protected void doStart() throws SensorHubException {
        ModuleRegistry registry = getParentHub().getModuleRegistry();
        IFederatedDatabase fedDb = getParentHub().getDatabaseRegistry().getFederatedDatabase();

        // Initialize module resources
        moduleListResource = new ModuleListResource(registry);
        moduleDetailResource = new ModuleDetailResource(registry);

        // Initialize datastore resources
        systemListResource = new SystemListResource(fedDb);
        dataStreamListResource = new DataStreamListResource(fedDb);
        observationResource = new ObservationResource(fedDb);

        // Initialize tools
        moduleTool = new ModuleTool(registry);
        databaseTool = new DatabaseTool(
                getParentHub().getDatabaseRegistry(),
                config.writeDatabaseId
        );
        processTool = new ProcessTool(getParentHub());

        // Create transport provider
        transportProvider = new JavaxMcpStreamableTransport();

        // Collect all static resources
        List<McpServerFeatures.AsyncResourceSpecification> staticResources = new ArrayList<>();
        staticResources.add(new McpServerFeatures.AsyncResourceSpecification(
                moduleListResource.toResourceSchema(),
                (exchange, request) -> moduleListResource.read(exchange)
        ));
        staticResources.add(new McpServerFeatures.AsyncResourceSpecification(
                systemListResource.toResourceSchema(),
                (exchange, request) -> systemListResource.read(exchange)
        ));

        // Collect all resource templates
        List<McpServerFeatures.AsyncResourceTemplateSpecification> templateResources = new ArrayList<>();
        templateResources.add(new McpServerFeatures.AsyncResourceTemplateSpecification(
                moduleDetailResource.toResourceTemplateSchema(),
                (exchange, request) -> handleModuleDetailRead(request)
        ));
        templateResources.add(new McpServerFeatures.AsyncResourceTemplateSpecification(
                dataStreamListResource.toResourceTemplateSchema(),
                (exchange, request) -> handleDataStreamListRead(request)
        ));
        templateResources.add(new McpServerFeatures.AsyncResourceTemplateSpecification(
                observationResource.toResourceTemplateSchema(),
                (exchange, request) -> handleObservationRead(request)
        ));

        // Build the MCP server
        mcpServer = McpServer.async(transportProvider)
                .serverInfo("OpenSensorHub", getModuleVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .resources(true, true)  // subscribe=true, listChanged=true
                        .logging()
                        .build())
                // Register tools
                .toolCall(moduleTool.getTool(), moduleTool.getCallHandler())
                .toolCall(databaseTool.getTool(), databaseTool.getCallHandler())
                .toolCall(processTool.getTool(), processTool.getCallHandler())
                // Register static resources
                .resources(staticResources)
                // Register resource templates
                .resourceTemplates(templateResources)
                .build();

        log.info("MCP server initialized with {} tools and {} resources",
                3, staticResources.size() + templateResources.size());

        // Deploy the servlet
        deploy();
    }

    /**
     * Handle reads for the module detail resource template.
     */
    private reactor.core.publisher.Mono<McpSchema.ReadResourceResult> handleModuleDetailRead(
            McpSchema.ReadResourceRequest request) {
        String uri = request.uri();
        String prefix = "modules://detail/";
        if (!uri.startsWith(prefix)) {
            return reactor.core.publisher.Mono.error(
                    new IllegalArgumentException("Invalid URI format: " + uri));
        }
        String moduleId = uri.substring(prefix.length());
        if (moduleId.isEmpty()) {
            return reactor.core.publisher.Mono.error(
                    new IllegalArgumentException("Module ID is required"));
        }
        return moduleDetailResource.readWithId(moduleId);
    }

    /**
     * Handle reads for the datastream list resource template.
     */
    private reactor.core.publisher.Mono<McpSchema.ReadResourceResult> handleDataStreamListRead(
            McpSchema.ReadResourceRequest request) {
        String uri = request.uri();
        String prefix = "datastore://systems/";
        String suffix = "/datastreams";
        if (!uri.startsWith(prefix) || !uri.endsWith(suffix)) {
            return reactor.core.publisher.Mono.error(
                    new IllegalArgumentException("Invalid URI format: " + uri));
        }
        String systemUID = uri.substring(prefix.length(), uri.length() - suffix.length());
        if (systemUID.isEmpty()) {
            return reactor.core.publisher.Mono.error(
                    new IllegalArgumentException("System UID is required"));
        }
        return dataStreamListResource.readWithSystemUID(systemUID);
    }

    /**
     * Handle reads for the observation resource template.
     */
    private reactor.core.publisher.Mono<McpSchema.ReadResourceResult> handleObservationRead(
            McpSchema.ReadResourceRequest request) {
        String uri = request.uri();
        String prefix = "datastore://systems/";
        String suffix = "/observations";
        if (!uri.startsWith(prefix) || !uri.endsWith(suffix)) {
            return reactor.core.publisher.Mono.error(
                    new IllegalArgumentException("Invalid URI format: " + uri));
        }
        String systemUID = uri.substring(prefix.length(), uri.length() - suffix.length());
        if (systemUID.isEmpty()) {
            return reactor.core.publisher.Mono.error(
                    new IllegalArgumentException("System UID is required"));
        }
        return observationResource.readWithSystemUID(systemUID);
    }

    protected void deploy() throws SensorHubException {
        var wildcardEndpoint = config.endPoint + "/*";

        httpServer.deployServlet((HttpServlet) transportProvider, wildcardEndpoint);
        httpServer.addServletSecurity(wildcardEndpoint, config.security.requireAuth);

        log.info("MCP Streamable HTTP service deployed at {}", getPublicEndpointUrl());
    }

    @Override
    protected void doStop() {
        // Close MCP server gracefully
        if (transportProvider != null) {
            try {
                transportProvider.closeGracefully().block();
            } catch (Exception e) {
                log.warn("Error during MCP transport shutdown", e);
            }
        }

        // Undeploy servlet
        undeploy();

        // Stop thread pool
        if (threadPool != null) {
            threadPool.shutdown();
            threadPool = null;
        }

        // Clear references
        mcpServer = null;
        transportProvider = null;
        moduleListResource = null;
        moduleDetailResource = null;
        systemListResource = null;
        dataStreamListResource = null;
        observationResource = null;
        moduleTool = null;
        databaseTool = null;
        processTool = null;

        setState(ModuleState.STOPPED);
    }

    protected void undeploy() {
        if (httpServer == null || !httpServer.isStarted()) {
            return;
        }

        if (transportProvider != null) {
            try {
                httpServer.undeployServlet(transportProvider);
            } catch (Exception e) {
                log.warn("Error undeploying MCP servlet", e);
            }
        }
    }

    @Override
    public void cleanup() throws SensorHubException {
        if (securityHandler != null) {
            securityHandler.unregister();
        }
    }

    private String getModuleVersion() {
        try {
            var moduleInfo = ModuleUtils.getModuleInfo(McpService.class);
            return moduleInfo != null && moduleInfo.getModuleVersion() != null
                    ? moduleInfo.getModuleVersion()
                    : "1.0.0";
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    public ScheduledExecutorService getThreadPool() {
        return threadPool;
    }

    public String getPublicEndpointUrl() {
        return getHttpServer().getPublicEndpointUrl(config.endPoint);
    }

    public String getStreamableEndpointUrl() {
        return getPublicEndpointUrl();
    }
}
