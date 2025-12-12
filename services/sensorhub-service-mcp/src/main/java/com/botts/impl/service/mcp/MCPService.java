package com.botts.impl.service.mcp;

import com.botts.impl.service.mcp.resources.ModuleDetailResource;
import com.botts.impl.service.mcp.resources.ModuleListResource;
import com.botts.impl.service.mcp.tools.ModuleTool;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.utils.ModuleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * MCP (Model Context Protocol) Service for OpenSensorHub.
 *
 * Exposes OSH functionality through the MCP protocol, allowing AI assistants
 * and other MCP clients to interact with sensor systems, modules, and data.
 */
public class MCPService extends AbstractHttpServiceModule<MCPServiceConfig> {

    private static final Logger log = LoggerFactory.getLogger(MCPService.class);

    private ScheduledExecutorService threadPool;
    private JavaxMcpSseTransport transportProvider;
    private McpAsyncServer mcpServer;

    // Resources
    private ModuleListResource moduleListResource;
    private ModuleDetailResource moduleDetailResource;

    // Tools
    private ModuleTool moduleTool;

    @Override
    public void setConfiguration(MCPServiceConfig config) {
        super.setConfiguration(config);
        this.securityHandler = new MCPSecurity(this, config.security.enableAccessControl);
    }

    @Override
    protected void doStart() throws SensorHubException {
        ModuleRegistry registry = getParentHub().getModuleRegistry();

        // Initialize resources
        moduleListResource = new ModuleListResource(registry);
        moduleDetailResource = new ModuleDetailResource(registry);

        // Initialize tools
        moduleTool = new ModuleTool(registry);

        // Create transport provider
        transportProvider = new JavaxMcpSseTransport();

        // Build the MCP server
        mcpServer = McpServer.async(transportProvider)
                .serverInfo("OpenSensorHub", getModuleVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .resources(true, true)  // subscribe=true, listChanged=true
                        .logging()
                        .build())
                // Register tools
                .tool(
                        moduleTool.getTool(),
                        moduleTool.getCallHandler()
                )
                // Register static resources
                .resources(List.of(
                        new McpServerFeatures.AsyncResourceSpecification(
                                moduleListResource.toResourceSchema(),
                                (exchange, request) -> moduleListResource.read(exchange)
                        ))
                )
                // Register resource templates
                .resourceTemplates(List.of(
                        new McpServerFeatures.AsyncResourceTemplateSpecification(
                                moduleDetailResource.toResourceTemplateSchema(),
                                (exchange, request) -> handleModuleDetailRead(request)
                        ))
                )
                .build();

        log.info("MCP server initialized with {} tools and {} resources",
                1, 2);

        // Deploy the servlet
        deploy();
    }

    /**
     * Handle reads for the module detail resource template.
     * Extracts the moduleId from the request URI and delegates to the resource.
     */
    private reactor.core.publisher.Mono<McpSchema.ReadResourceResult> handleModuleDetailRead(
            McpSchema.ReadResourceRequest request) {

        String uri = request.uri();

        // Extract moduleId from URI: modules://detail/{moduleId}
        String prefix = "modules://detail/";
        if (!uri.startsWith(prefix)) {
            return reactor.core.publisher.Mono.error(
                    new IllegalArgumentException("Invalid URI format: " + uri)
            );
        }

        String moduleId = uri.substring(prefix.length());
        if (moduleId.isEmpty()) {
            return reactor.core.publisher.Mono.error(
                    new IllegalArgumentException("Module ID is required")
            );
        }

        return moduleDetailResource.readWithId(moduleId);
    }

    protected void deploy() throws SensorHubException {
        var wildcardEndpoint = config.endPoint + "/*";

        httpServer.deployServlet((HttpServlet) transportProvider, wildcardEndpoint);
        httpServer.addServletSecurity(wildcardEndpoint, config.security.requireAuth);

        log.info("MCP service deployed at {}", getPublicEndpointUrl());
        log.info("  SSE endpoint: {}/sse", getPublicEndpointUrl());
        log.info("  Message endpoint: {}/message", getPublicEndpointUrl());
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
        moduleTool = null;

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
            var moduleInfo = ModuleUtils.getModuleInfo(MCPService.class);
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

    public String getSseEndpointUrl() {
        return getPublicEndpointUrl() + "/sse";
    }
}