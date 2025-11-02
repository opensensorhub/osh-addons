package com.botts.impl.service.mcp.tools;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;

public interface MCPTool {

    // Tool description

    String getName();

    String getTitle();

    String getDescription();

    McpSchema.JsonSchema getInputSchema();

    Map<String, Object> getOutputSchema();

    McpSchema.ToolAnnotations getToolAnnotations();

    Map<String, Object> getMetaData();

    BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> getCallHandler();

}
