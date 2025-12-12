package com.botts.impl.service.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Javax-compatible SSE transport provider for MCP.
 *
 * Implements McpServerTransportProvider to integrate with the MCP SDK's
 * server architecture while using javax.servlet instead of jakarta.servlet.
 *
 * Endpoints:
 * - GET /sse - Establishes SSE event stream for server-to-client messages
 * - POST /message?sessionId=xxx - Receives client-to-server JSON-RPC messages
 */
public class JavaxMcpSseTransport extends HttpServlet implements McpServerTransportProvider {

    private static final Logger log = LoggerFactory.getLogger(JavaxMcpSseTransport.class);

    private final ObjectMapper objectMapper;
    private final Map<String, SessionTransport> sessions = new ConcurrentHashMap<>();
    private McpServerSession.Factory sessionFactory;

    public JavaxMcpSseTransport() {
        this.objectMapper = new ObjectMapper();
    }

    public JavaxMcpSseTransport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ==================== McpServerTransportProvider Implementation ====================

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (sessions.isEmpty()) {
            return Mono.empty();
        }

        McpSchema.JSONRPCNotification notification = new McpSchema.JSONRPCNotification(
                McpSchema.JSONRPC_VERSION,
                method,
                params
        );

        return Mono.fromRunnable(() -> {
            try {
                String json = objectMapper.writeValueAsString(notification);
                sessions.values().forEach(session -> session.sendEvent("message", json));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize notification", e);
            }
        });
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            sessions.values().forEach(SessionTransport::close);
            sessions.clear();
        });
    }

    // ==================== Servlet Implementation ====================

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();

        if ("/sse".equals(pathInfo) || pathInfo == null || pathInfo.isEmpty()) {
            handleSseConnection(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();

        if ("/message".equals(pathInfo)) {
            handleMessage(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleSseConnection(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        if (sessionFactory == null) {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "MCP server not initialized");
            return;
        }

        String sessionId = UUID.randomUUID().toString();

        // Configure SSE response
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");
        resp.setHeader("X-Accel-Buffering", "no");

        // Enable async processing
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(0);

        PrintWriter writer = resp.getWriter();

        // Create transport for this session
        SessionTransport transport = new SessionTransport(sessionId, asyncContext, writer, objectMapper);
        sessions.put(sessionId, transport);

        // Create MCP session using the factory
        McpServerSession mcpSession = sessionFactory.create(transport);
        transport.setMcpSession(mcpSession);

        // Send endpoint event telling client where to POST messages
        String messageEndpoint = buildMessageEndpoint(req, sessionId);
        transport.sendEvent("endpoint", messageEndpoint);

        log.info("SSE client connected: {}", sessionId);

        // Handle disconnect
        asyncContext.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                cleanupSession(sessionId);
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                cleanupSession(sessionId);
            }

            @Override
            public void onError(AsyncEvent event) {
                cleanupSession(sessionId);
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
            }
        });
    }

    private void handleMessage(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String sessionId = req.getParameter("sessionId");

        if (sessionId == null || sessionId.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing sessionId parameter");
            return;
        }

        SessionTransport transport = sessions.get(sessionId);
        if (transport == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid sessionId: " + sessionId);
            return;
        }

        // Read request body
        String body = readRequestBody(req);

        try {
            McpSchema.JSONRPCMessage message = parseJsonRpcMessage(body);

            // Handle the message through the MCP session
            transport.handleIncomingMessage(message)
                    .doOnError(error -> log.error("Error processing message for session {}", sessionId, error))
                    .subscribe();

            // Acknowledge receipt
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"jsonrpc\":\"2.0\",\"result\":\"accepted\"}");

        } catch (Exception e) {
            log.error("Failed to process message for session {}", sessionId, e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.getWriter().write(String.format(
                    "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32700,\"message\":\"%s\"}}",
                    escapeJson(e.getMessage())
            ));
        }
    }

    private String buildMessageEndpoint(HttpServletRequest req, String sessionId) {
        StringBuilder endpoint = new StringBuilder();

        String contextPath = req.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            endpoint.append(contextPath);
        }

        String servletPath = req.getServletPath();
        if (servletPath != null && !servletPath.isEmpty()) {
            endpoint.append(servletPath);
        }

        endpoint.append("/message?sessionId=").append(sessionId);
        return endpoint.toString();
    }

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

    private McpSchema.JSONRPCMessage parseJsonRpcMessage(String json) throws IOException {
        JsonNode node = objectMapper.readTree(json);

        if (node.has("method")) {
            if (node.has("id") && !node.get("id").isNull()) {
                return objectMapper.treeToValue(node, McpSchema.JSONRPCRequest.class);
            } else {
                return objectMapper.treeToValue(node, McpSchema.JSONRPCNotification.class);
            }
        } else if (node.has("result") || node.has("error")) {
            return objectMapper.treeToValue(node, McpSchema.JSONRPCResponse.class);
        }

        throw new IOException("Invalid JSON-RPC message: missing method, result, or error");
    }

    private void cleanupSession(String sessionId) {
        SessionTransport transport = sessions.remove(sessionId);
        if (transport != null) {
            transport.close();
            log.info("SSE client disconnected: {}", sessionId);
        }
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
     * Per-session transport that implements McpServerTransport.
     * Each SSE connection gets its own transport instance.
     */
    private static class SessionTransport implements McpServerTransport {

        private final String sessionId;
        private final AsyncContext asyncContext;
        private final PrintWriter writer;
        private final ObjectMapper objectMapper;
        private final Sinks.Many<McpSchema.JSONRPCMessage> incomingSink;

        private io.modelcontextprotocol.spec.McpServerSession mcpSession;
        private volatile boolean closed = false;

        SessionTransport(String sessionId, AsyncContext asyncContext, PrintWriter writer, ObjectMapper objectMapper) {
            this.sessionId = sessionId;
            this.asyncContext = asyncContext;
            this.writer = writer;
            this.objectMapper = objectMapper;
            this.incomingSink = Sinks.many().unicast().onBackpressureBuffer();
        }

        void setMcpSession(McpServerSession mcpSession) {
            this.mcpSession = mcpSession;
        }

        Mono<Void> handleIncomingMessage(McpSchema.JSONRPCMessage message) {
            if (mcpSession == null) {
                return Mono.error(new IllegalStateException("MCP session not initialized"));
            }
            return mcpSession.handle(message);
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                if (closed) return;

                try {
                    String json = objectMapper.writeValueAsString(message);
                    sendEvent("message", json);
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize message for session {}", sessionId, e);
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, io.modelcontextprotocol.json.TypeRef<T> typeRef) {
            return objectMapper.convertValue(data, objectMapper.constructType(typeRef.getType()));
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(this::close);
        }

        synchronized void sendEvent(String eventType, String data) {
            if (closed) return;

            try {
                writer.write("event: ");
                writer.write(eventType);
                writer.write("\ndata: ");
                writer.write(data);
                writer.write("\n\n");
                writer.flush();

                if (writer.checkError()) {
                    log.warn("Write error detected for session {}", sessionId);
                    close();
                }
            } catch (Exception e) {
                log.warn("Failed to send event to session {}: {}", sessionId, e.getMessage());
                close();
            }
        }

        public synchronized void close() {
            if (closed) return;
            closed = true;

            incomingSink.tryEmitComplete();

            try {
                asyncContext.complete();
            } catch (Exception e) {
                // Already closed
            }

            if (mcpSession != null) {
                mcpSession.closeGracefully().subscribe();
            }
        }
    }
}