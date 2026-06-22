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

import com.georobotix.impl.service.mcp.oauth.McpBearerTokenValidator;
import com.georobotix.impl.service.mcp.oauth.McpOAuthHelper;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Javax-compatible Streamable HTTP transport provider for MCP.
 *
 * Implements the MCP Streamable HTTP protocol using javax.servlet, since
 * OSH's embedded Jetty server uses javax.servlet (not jakarta.servlet).
 */
public class JavaxMcpStreamableTransport extends HttpServlet implements McpStreamableServerTransportProvider {

    private static final Logger log = LoggerFactory.getLogger(JavaxMcpStreamableTransport.class);

    private static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ORIGIN_HEADER = "Origin";
    private static final String MCP_PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";
    private static final String AUTHENTICATED_USER_ATTRIBUTE = JavaxMcpStreamableTransport.class.getName() + ".authenticatedUser";
    private static final String TEXT_EVENT_STREAM = "text/event-stream";
    private static final String APPLICATION_JSON = "application/json";
    private static final String MESSAGE_EVENT_TYPE = "message";
    private static final Set<String> SUPPORTED_PROTOCOL_VERSIONS = Set.of(
            "2025-03-26",
            "2025-06-18",
            "2025-11-25"
    );

    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    private final McpJsonMapper jsonMapper;
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();
    private McpStreamableServerSession.Factory sessionFactory;
    private volatile boolean isClosing = false;
    private volatile boolean oauthEnabled = false;
    private volatile String resourceMetadataUrl;
    private volatile String requiredScopes;
    private volatile Set<String> allowedOrigins = Set.of();
    private volatile boolean allowAnyOrigin = false;
    private volatile McpBearerTokenValidator bearerTokenValidator;

    /**
     * Returns the authenticated user for the current HTTP request, or "anonymous" if not authenticated.
     */
    public static String getCurrentUser() {
        var user = currentUser.get();
        return user != null ? user : "anonymous";
    }

    /**
     * Configures MCP OAuth discovery and browser-origin validation for this transport.
     */
    public void configureOAuth(String resourceMetadataUrl, String requiredScopes, String allowedOrigins,
                               McpBearerTokenValidator bearerTokenValidator) {
        this.oauthEnabled = resourceMetadataUrl != null && !resourceMetadataUrl.isBlank();
        this.resourceMetadataUrl = resourceMetadataUrl;
        this.requiredScopes = requiredScopes;
        this.bearerTokenValidator = bearerTokenValidator;

        var origins = new HashSet<String>();
        this.allowAnyOrigin = false;
        if (allowedOrigins != null) {
            for (var origin: allowedOrigins.split(",")) {
                var trimmed = origin.trim();
                if ("*".equals(trimmed)) {
                    this.allowAnyOrigin = true;
                } else if (!trimmed.isEmpty()) {
                    origins.add(trimmed);
                }
            }
        }
        this.allowedOrigins = Set.copyOf(origins);
    }

    public JavaxMcpStreamableTransport() {
        this.jsonMapper = McpJsonDefaults.getMapper();
    }

    public JavaxMcpStreamableTransport(McpJsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (sessions.isEmpty()) {
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> {
            for (McpStreamableServerSession session : sessions.values()) {
                session.sendNotification(method, params)
                        .doOnError(e -> log.warn("Failed to send notification to session {}", session.getId(), e))
                        .subscribe();
            }
        });
    }

    @Override
    public Mono<Void> closeGracefully() {
        isClosing = true;
        return Mono.fromRunnable(() -> {
            sessions.values().forEach(session -> {
                try {
                    session.closeGracefully().block();
                } catch (Exception e) {
                    log.warn("Error closing session {}", session.getId(), e);
                }
            });
            sessions.clear();
        });
    }

    // ==================== Servlet Implementation ====================

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (isClosing) {
            sendPlainError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is shutting down");
            return;
        }

        if (sessionFactory == null) {
            sendPlainError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "MCP server not initialized");
            return;
        }

        if (!validateOrigin(req, resp) ||
                !validateAuthorization(req, resp) ||
                !validatePostAccept(req, resp)) {
            return;
        }

        // Store authenticated user for the duration of this request
        var principal = req.getUserPrincipal();
        var authenticatedUser = req.getAttribute(AUTHENTICATED_USER_ATTRIBUTE);
        currentUser.set(authenticatedUser instanceof String user && !user.isBlank()
                ? user
                : principal != null ? principal.getName() : req.getRemoteUser());
        try {
            doPostInternal(req, resp);
        } finally {
            currentUser.remove();
        }
    }

    private void doPostInternal(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Read the request body
        String body = readRequestBody(req);
        McpSchema.JSONRPCMessage message;
        try {
            message = parseJsonRpcMessage(body);
        } catch (Exception e) {
            sendJsonRpcError(resp, HttpServletResponse.SC_BAD_REQUEST, -32700, "Parse error: " + e.getMessage());
            return;
        }

        // Route based on message type
        if (message instanceof McpSchema.JSONRPCRequest request) {
            if ("initialize".equals(request.method())) {
                handleInitialize(req, resp, request);
            } else {
                handleRequest(req, resp, request);
            }
        } else if (message instanceof McpSchema.JSONRPCNotification notification) {
            handleNotification(req, resp, notification);
        } else if (message instanceof McpSchema.JSONRPCResponse response) {
            handleResponse(req, resp, response);
        } else {
            sendJsonRpcError(resp, HttpServletResponse.SC_BAD_REQUEST, -32600, "Invalid message type");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (isClosing) {
            sendPlainError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is shutting down");
            return;
        }

        if (!validateOrigin(req, resp) ||
                !validateAuthorization(req, resp) ||
                !validateProtocolVersion(req, resp)) {
            return;
        }

        // Check Accept header
        String accept = req.getHeader(ACCEPT_HEADER);
        if (accept == null || !accept.contains(TEXT_EVENT_STREAM)) {
            sendPlainError(resp, HttpServletResponse.SC_NOT_ACCEPTABLE, "Accept header must include text/event-stream");
            return;
        }

        // Require session ID
        String sessionId = req.getHeader(MCP_SESSION_ID_HEADER);
        if (sessionId == null || sessionId.isBlank()) {
            sendPlainError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing Mcp-Session-Id header");
            return;
        }

        McpStreamableServerSession session = sessions.get(sessionId);
        if (session == null) {
            sendPlainError(resp, HttpServletResponse.SC_NOT_FOUND, "Session not found: " + sessionId);
            return;
        }

        // Set up SSE stream
        resp.setContentType(TEXT_EVENT_STREAM);
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");
        resp.setHeader("X-Accel-Buffering", "no");

        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(0);

        PrintWriter writer = resp.getWriter();
        StreamableSessionTransport transport = new StreamableSessionTransport(sessionId, asyncContext, writer, jsonMapper);

        // Open a listening stream for server-initiated messages
        McpStreamableServerSession.McpStreamableServerSessionStream stream = session.listeningStream(transport);

        asyncContext.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                stream.close();
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                stream.close();
                try { asyncContext.complete(); } catch (Exception ignored) {}
            }

            @Override
            public void onError(AsyncEvent event) {
                stream.close();
                try { asyncContext.complete(); } catch (Exception ignored) {}
            }

            @Override
            public void onStartAsync(AsyncEvent event) {}
        });

        log.debug("SSE listening stream opened for session {}", sessionId);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!validateOrigin(req, resp) ||
                !validateAuthorization(req, resp) ||
                !validateProtocolVersion(req, resp)) {
            return;
        }

        String sessionId = req.getHeader(MCP_SESSION_ID_HEADER);
        if (sessionId == null || sessionId.isBlank()) {
            sendPlainError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing Mcp-Session-Id header");
            return;
        }

        McpStreamableServerSession session = sessions.remove(sessionId);
        if (session == null) {
            sendPlainError(resp, HttpServletResponse.SC_NOT_FOUND, "Session not found: " + sessionId);
            return;
        }

        try {
            session.delete().block();
        } catch (Exception e) {
            log.warn("Error deleting session {}", sessionId, e);
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        log.info("Session deleted: {}", sessionId);
    }

    // ==================== Message Handlers ====================

    private void handleInitialize(HttpServletRequest req, HttpServletResponse resp,
                                  McpSchema.JSONRPCRequest request) throws IOException {
        try {
            // Deserialize init params
            McpSchema.InitializeRequest initRequest = jsonMapper.convertValue(
                    request.params(), new TypeRef<McpSchema.InitializeRequest>() {});

            // Create session
            McpStreamableServerSession.McpStreamableServerSessionInit sessionInit =
                    sessionFactory.startSession(initRequest);

            McpStreamableServerSession session = sessionInit.session();
            sessions.put(session.getId(), session);

            // Get the init result
            McpSchema.InitializeResult initResult = sessionInit.initResult().block();

            // Build the JSON-RPC response
            McpSchema.JSONRPCResponse jsonRpcResponse = new McpSchema.JSONRPCResponse(
                    McpSchema.JSONRPC_VERSION,
                    request.id(),
                    initResult,
                    null  // no error
            );

            // Return as JSON with session ID header
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType(APPLICATION_JSON);
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader(MCP_SESSION_ID_HEADER, session.getId());

            String json = jsonMapper.writeValueAsString(jsonRpcResponse);
            resp.getWriter().write(json);
            resp.getWriter().flush();

            log.info("Session initialized: {}", session.getId());

        } catch (Exception e) {
            log.error("Failed to initialize session", e);
            sendJsonRpcError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    -32603, "Internal error: " + e.getMessage());
        }
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp,
                               McpSchema.JSONRPCRequest request) throws IOException {
        if (!validateProtocolVersion(req, resp)) {
            return;
        }

        String sessionId = req.getHeader(MCP_SESSION_ID_HEADER);
        if (sessionId == null || sessionId.isBlank()) {
            sendJsonRpcError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    -32600, "Missing Mcp-Session-Id header");
            return;
        }

        McpStreamableServerSession session = sessions.get(sessionId);
        if (session == null) {
            sendJsonRpcError(resp, HttpServletResponse.SC_NOT_FOUND,
                    -32001, "Session not found: " + sessionId);
            return;
        }

        // Open SSE stream for the response
        resp.setContentType(TEXT_EVENT_STREAM);
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");
        resp.setHeader("X-Accel-Buffering", "no");

        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(30000); // 30 second timeout for request processing

        PrintWriter writer = resp.getWriter();
        StreamableSessionTransport transport = new StreamableSessionTransport(
                sessionId, asyncContext, writer, jsonMapper);

        // Process the request through the session - responses stream back via SSE
        session.responseStream(request, transport)
                .doOnSuccess(v -> {
                    transport.close();
                })
                .doOnError(e -> {
                    log.error("Error processing request for session {}", sessionId, e);
                    transport.close();
                })
                .subscribe();
    }

    private void handleNotification(HttpServletRequest req, HttpServletResponse resp,
                                    McpSchema.JSONRPCNotification notification) throws IOException {
        if (!validateProtocolVersion(req, resp)) {
            return;
        }

        String sessionId = req.getHeader(MCP_SESSION_ID_HEADER);

        // "initialized" notification may come without session ID on some clients
        if (sessionId == null || sessionId.isBlank()) {
            if ("notifications/initialized".equals(notification.method())) {
                // Accept without a session - some clients send this early
                resp.setStatus(HttpServletResponse.SC_ACCEPTED);
                return;
            }
            sendJsonRpcError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    -32600, "Missing Mcp-Session-Id header");
            return;
        }

        McpStreamableServerSession session = sessions.get(sessionId);
        if (session == null) {
            sendJsonRpcError(resp, HttpServletResponse.SC_NOT_FOUND,
                    -32001, "Session not found: " + sessionId);
            return;
        }

        try {
            session.accept(notification).block();
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        } catch (Exception e) {
            log.error("Error processing notification for session {}", sessionId, e);
            sendJsonRpcError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    -32603, "Internal error: " + e.getMessage());
        }
    }

    private void handleResponse(HttpServletRequest req, HttpServletResponse resp,
                                McpSchema.JSONRPCResponse response) throws IOException {
        if (!validateProtocolVersion(req, resp)) {
            return;
        }

        String sessionId = req.getHeader(MCP_SESSION_ID_HEADER);
        if (sessionId == null || sessionId.isBlank()) {
            sendJsonRpcError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    -32600, "Missing Mcp-Session-Id header");
            return;
        }

        McpStreamableServerSession session = sessions.get(sessionId);
        if (session == null) {
            sendJsonRpcError(resp, HttpServletResponse.SC_NOT_FOUND,
                    -32001, "Session not found: " + sessionId);
            return;
        }

        try {
            session.accept(response).block();
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        } catch (Exception e) {
            log.error("Error processing response for session {}", sessionId, e);
            sendJsonRpcError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    -32603, "Internal error: " + e.getMessage());
        }
    }

    // ==================== Helpers ====================

    private String readRequestBody(HttpServletRequest req) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    private boolean validatePostAccept(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String accept = req.getHeader(ACCEPT_HEADER);
        if (accept == null || !accept.contains(APPLICATION_JSON) || !accept.contains(TEXT_EVENT_STREAM)) {
            sendPlainError(resp, HttpServletResponse.SC_NOT_ACCEPTABLE,
                    "Accept header must include application/json and text/event-stream");
            return false;
        }
        return true;
    }

    private boolean validateProtocolVersion(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String protocolVersion = req.getHeader(MCP_PROTOCOL_VERSION_HEADER);
        if (protocolVersion == null || protocolVersion.isBlank()) {
            return true;
        }

        if (!SUPPORTED_PROTOCOL_VERSIONS.contains(protocolVersion.trim())) {
            sendPlainError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Unsupported MCP protocol version: " + protocolVersion);
            return false;
        }

        return true;
    }

    private boolean validateAuthorization(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!oauthEnabled) {
            return true;
        }

        String authHeader = req.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            McpOAuthHelper.addBearerChallenge(resp, resourceMetadataUrl, requiredScopes);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(APPLICATION_JSON);
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"error\":\"Bearer token required\"}");
            return false;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        if (bearerTokenValidator != null) {
            var validation = bearerTokenValidator.validate(token);
            if (!validation.isValid()) {
                McpOAuthHelper.addBearerChallenge(resp, resourceMetadataUrl, requiredScopes,
                        "invalid_token", validation.errorDescription());
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType(APPLICATION_JSON);
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write("{\"error\":\"Invalid bearer token\"}");
                return false;
            }

            if (validation.subject() != null && !validation.subject().isBlank())
                req.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, validation.subject());
        }

        return true;
    }

    private boolean validateOrigin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String origin = req.getHeader(ORIGIN_HEADER);
        if (origin == null || origin.isBlank()) {
            return true;
        }

        if (allowAnyOrigin || allowedOrigins.contains(origin) || origin.equals(getRequestOrigin(req))) {
            return true;
        }

        sendPlainError(resp, HttpServletResponse.SC_FORBIDDEN, "Origin not allowed");
        return false;
    }

    private String getRequestOrigin(HttpServletRequest req) {
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80) ||
                ("https".equalsIgnoreCase(scheme) && port == 443);

        return scheme + "://" + host + (defaultPort ? "" : ":" + port);
    }

    private McpSchema.JSONRPCMessage parseJsonRpcMessage(String json) throws IOException {
        if (json.contains("\"method\"")) {
            if (json.contains("\"id\"")) {
                return jsonMapper.readValue(json, McpSchema.JSONRPCRequest.class);
            } else {
                return jsonMapper.readValue(json, McpSchema.JSONRPCNotification.class);
            }
        } else if (json.contains("\"result\"") || json.contains("\"error\"")) {
            return jsonMapper.readValue(json, McpSchema.JSONRPCResponse.class);
        }

        throw new IOException("Invalid JSON-RPC message: cannot determine type");
    }

    private void sendJsonRpcError(HttpServletResponse resp, int httpStatus,
                                  int errorCode, String message) throws IOException {
        resp.setStatus(httpStatus);
        resp.setContentType(APPLICATION_JSON);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(String.format(
                "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":%d,\"message\":\"%s\"}}",
                errorCode, escapeJson(message)
        ));
    }

    private void sendPlainError(HttpServletResponse resp, int httpStatus, String message) throws IOException {
        resp.setStatus(httpStatus);
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(message);
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // ==================== Session Transport ====================

    /**
     * Per-request/stream transport that implements McpStreamableServerTransport.
     * Each POST request or GET SSE stream gets its own transport instance.
     * Messages are sent back to the client as SSE events.
     */
    static class StreamableSessionTransport implements McpStreamableServerTransport {

        private final String sessionId;
        private final AsyncContext asyncContext;
        private final PrintWriter writer;
        private final McpJsonMapper jsonMapper;
        private final ReentrantLock writeLock = new ReentrantLock();
        private volatile boolean closed = false;

        StreamableSessionTransport(String sessionId, AsyncContext asyncContext, PrintWriter writer,
                                   McpJsonMapper jsonMapper) {
            this.sessionId = sessionId;
            this.asyncContext = asyncContext;
            this.writer = writer;
            this.jsonMapper = jsonMapper;
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return sendMessage(message, null);
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
            return Mono.fromRunnable(() -> {
                if (closed) return;

                writeLock.lock();
                try {
                    if (closed) return;

                    String json = jsonMapper.writeValueAsString(message);

                    writer.write("event: ");
                    writer.write(MESSAGE_EVENT_TYPE);
                    writer.write("\n");

                    // Include message ID for replay support
                    if (messageId != null) {
                        writer.write("id: ");
                        writer.write(messageId);
                        writer.write("\n");
                    } else {
                        writer.write("id: ");
                        writer.write(sessionId);
                        writer.write("\n");
                    }

                    writer.write("data: ");
                    writer.write(json);
                    writer.write("\n\n");
                    writer.flush();

                    if (writer.checkError()) {
                        log.warn("Write error for session {}", sessionId);
                        closed = true;
                    }
                } catch (IOException e) {
                    log.warn("Failed to send message to session {}: {}", sessionId, e.getMessage());
                    closed = true;
                } finally {
                    writeLock.unlock();
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return jsonMapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(this::close);
        }

        @Override
        public void close() {
            if (closed) return;
            writeLock.lock();
            try {
                if (closed) return;
                closed = true;
                try {
                    asyncContext.complete();
                } catch (Exception e) {
                    // Already completed
                }
            } finally {
                writeLock.unlock();
            }
        }
    }
}
