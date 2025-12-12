package com.botts.impl.service.mcp.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

/**
 * Abstract base class for MCP resources.
 */
public abstract class AbstractMCPResource {

    protected final ObjectMapper objectMapper;

    protected AbstractMCPResource() {
        this.objectMapper = new ObjectMapper();
    }

    protected AbstractMCPResource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * The unique URI for this resource.
     */
    public abstract String getUri();

    /**
     * Human-readable name for the resource.
     */
    public abstract String getName();

    /**
     * Description of what this resource provides.
     */
    public abstract String getDescription();

    /**
     * MIME type of the resource content.
     */
    public abstract String getMimeType();

    /**
     * Read the resource content.
     */
    public abstract Mono<McpSchema.ReadResourceResult> read(McpAsyncServerExchange exchange);

    /**
     * Convert this resource to an MCP Resource schema object for registration.
     */
    public McpSchema.Resource toResourceSchema() {
        return new McpSchema.Resource(
                getUri(),
                getName(),
                getDescription(),
                getMimeType(),
                null  // annotations
        );
    }
}