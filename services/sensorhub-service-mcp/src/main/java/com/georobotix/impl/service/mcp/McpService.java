/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.georobotix.impl.service.mcp;

import com.georobotix.impl.service.mcp.oauth.*;
import com.georobotix.impl.service.mcp.resources.*;
import com.georobotix.impl.service.mcp.tools.DatabaseTool;
import com.georobotix.impl.service.mcp.tools.ModuleTool;
import com.georobotix.impl.service.mcp.tools.ProcessTool;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IFederatedDatabase;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.AbstractHttpServiceModule;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.utils.ModuleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import java.util.ArrayList;
import java.util.Arrays;
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
    private McpOAuthMetadataServlet oauthMetadataServlet;
    private McpOAuthProtectedResourceMetadataServlet oauthProtectedResourceMetadataServlet;
    private Handler oauthRootHandler;
    private Handler oauthProtectedResourceRootHandler;
    private Handler oauthChallengeHandler;
    private HandlerCollection serverHandlers;
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
        // Config updates and HTTP server lifecycle events can both re-enter doStart().
        // Ensure the previous servlet/handlers are gone before replacing the transport.
        closeTransportProvider();
        undeploy();

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
                getParentHub().getEventBus(),
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

    private String getServletPath() {
        return ((HttpServerConfig) httpServer.getConfiguration()).servletsRootUrl;
    }

    protected void deploy() throws SensorHubException {
        var wildcardEndpoint = config.endPoint + "/*";

        removeStaleMcpTransportMappings(wildcardEndpoint);
        httpServer.deployServlet((HttpServlet) transportProvider, wildcardEndpoint);
        // TODO Use OAuth from detected OAuth security module
        var oauthEnabled = config.oauth != null && config.oauth.enabled;
        if (!oauthEnabled) {
            httpServer.addServletSecurity(wildcardEndpoint, config.security.requireAuth);
        }

        if (oauthEnabled) {
            validateOAuthConfig(config.oauth);

            var resourceMetadataUrl = getRootPublicUrl(McpOAuthHelper.getProtectedResourcePath(getServletPath(), config.endPoint));
            transportProvider.configureOAuth(
                    resourceMetadataUrl,
                    config.oauth.requiredScopes,
                    config.oauth.allowedOrigins,
                    new McpBearerTokenValidator(config.oauth, getPublicEndpointUrl())
            );

            oauthMetadataServlet = new McpOAuthMetadataServlet(config.oauth);

            var wellKnownPath = McpOAuthHelper.getAuthorizationServerPath(getServletPath(), config.endPoint);
            HttpServer server = (HttpServer) httpServer;
            serverHandlers = (HandlerCollection) server.getJettyServer().getHandler();

            // Create a ServletContextHandler at the well-known path
            ServletContextHandler oauthContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            oauthContext.setContextPath(wellKnownPath);
            oauthContext.setAllowNullPathInfo(true);
            oauthContext.addServlet(new ServletHolder(oauthMetadataServlet), "/*");

            oauthRootHandler = oauthContext;
            oauthRootHandler.setServer(server.getJettyServer());

            try {
                oauthRootHandler.start();
            } catch (Exception e) {
                throw new SensorHubException("Error starting OAuth metadata handler", e);
            }

            serverHandlers.addHandler(oauthRootHandler);
            // Do NOT add security - this endpoint must be publicly accessible
            log.info("OAuth metadata endpoint deployed at {}", wellKnownPath);

            var protectedResourcePath = McpOAuthHelper.getProtectedResourcePath(getServletPath(), config.endPoint);
            oauthProtectedResourceMetadataServlet = new McpOAuthProtectedResourceMetadataServlet(
                    config.oauth,
                    getPublicEndpointUrl()
            );

            ServletContextHandler protectedResourceContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            protectedResourceContext.setContextPath(protectedResourcePath);
            protectedResourceContext.setAllowNullPathInfo(true);
            protectedResourceContext.addServlet(new ServletHolder(oauthProtectedResourceMetadataServlet), "/*");

            oauthProtectedResourceRootHandler = protectedResourceContext;
            oauthProtectedResourceRootHandler.setServer(server.getJettyServer());

            oauthChallengeHandler = new McpOAuthChallengeHandler(
                    getContextEndpointPath(),
                    resourceMetadataUrl,
                    config.oauth.requiredScopes
            );
            oauthChallengeHandler.setServer(server.getJettyServer());

            try {
                oauthProtectedResourceRootHandler.start();
                oauthChallengeHandler.start();
            } catch (Exception e) {
                throw new SensorHubException("Error starting OAuth protected resource metadata handler", e);
            }

            serverHandlers.addHandler(oauthProtectedResourceRootHandler);
            serverHandlers.addHandler(oauthChallengeHandler);
            log.info("OAuth protected resource metadata endpoint deployed at {}", protectedResourcePath);
        } else {
            transportProvider.configureOAuth(null, null, null, null);
        }

        log.info("MCP Streamable HTTP service deployed at {}", getPublicEndpointUrl());
    }

    @Override
    protected void doStop() {
        // Close MCP server gracefully
        closeTransportProvider();

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

        if (serverHandlers != null && oauthRootHandler != null) {
            try {
                serverHandlers.removeHandler(oauthRootHandler);
                oauthRootHandler.stop();
            } catch (Exception e) {
                log.warn("Error undeploying OAuth metadata handler", e);
            }
            oauthRootHandler = null;
        }
        if (serverHandlers != null && oauthProtectedResourceRootHandler != null) {
            try {
                serverHandlers.removeHandler(oauthProtectedResourceRootHandler);
                oauthProtectedResourceRootHandler.stop();
            } catch (Exception e) {
                log.warn("Error undeploying OAuth protected resource metadata handler", e);
            }
            oauthProtectedResourceRootHandler = null;
        }
        if (serverHandlers != null && oauthChallengeHandler != null) {
            try {
                serverHandlers.removeHandler(oauthChallengeHandler);
                oauthChallengeHandler.stop();
            } catch (Exception e) {
                log.warn("Error undeploying OAuth challenge handler", e);
            }
            oauthChallengeHandler = null;
        }
        serverHandlers = null;
        oauthMetadataServlet = null;
        oauthProtectedResourceMetadataServlet = null;
    }

    private void closeTransportProvider() {
        if (transportProvider != null) {
            try {
                transportProvider.closeGracefully().block();
            } catch (Exception e) {
                log.warn("Error during MCP transport shutdown", e);
            }
        }
    }

    private void removeStaleMcpTransportMappings(String pathSpec) {
        if (!(httpServer instanceof HttpServer server) || server.getServletHandler() == null)
            return;

        ServletHandler handler = server.getServletHandler().getServletHandler();
        var mappingsToKeep = new ArrayList<ServletMapping>();
        var transportServletNames = new ArrayList<String>();

        for (ServletMapping mapping: handler.getServletMappings()) {
            if (Arrays.asList(mapping.getPathSpecs()).contains(pathSpec)) {
                transportServletNames.add(mapping.getServletName());
            } else {
                mappingsToKeep.add(mapping);
            }
        }

        if (transportServletNames.isEmpty())
            return;

        var holdersToKeep = new ArrayList<ServletHolder>();
        for (ServletHolder holder: handler.getServlets()) {
            if (!transportServletNames.contains(holder.getName()))
                holdersToKeep.add(holder);
        }

        handler.setServletMappings(mappingsToKeep.toArray(new ServletMapping[0]));
        handler.setServlets(holdersToKeep.toArray(new ServletHolder[0]));
        log.info("Removed stale MCP servlet mapping at {}", pathSpec);
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

    private String getRootPublicUrl(String path) {
        HttpServer server = (HttpServer) getHttpServer();
        var baseUrl = server.getServerBaseUrl();
        if (baseUrl.endsWith("/"))
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private String getContextEndpointPath() {
        HttpServer server = (HttpServer) getHttpServer();
        var root = server.getConfiguration().servletsRootUrl;
        if (root == null || root.isBlank() || "/".equals(root))
            return config.endPoint;
        return (root.endsWith("/") ? root.substring(0, root.length() - 1) : root) + config.endPoint;
    }

    private void validateOAuthConfig(McpOAuthConfig oauth) throws SensorHubException {
        requireOAuthField(oauth.issuer, "issuer");
        requireOAuthField(oauth.authorizationEndpoint, "authorizationEndpoint");
        requireOAuthField(oauth.tokenEndpoint, "tokenEndpoint");
        requireOAuthField(oauth.jwksUri, "jwksUri");
    }

    private void requireOAuthField(String value, String fieldName) throws SensorHubException {
        if (value == null || value.isBlank())
            throw new SensorHubException("OAuth is enabled but " + fieldName + " is not configured");
    }
}
