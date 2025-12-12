package com.botts.impl.service.mcp.tools;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;

public abstract class AbstractMCPTool {

    public McpSchema.Tool getTool() {
        return McpSchema.Tool.builder()
                .name(getName())
                .title(getTitle())
                .description(getDescription())
                .inputSchema(getInputSchema())
                .outputSchema(getOutputSchema())
                .annotations(getToolAnnotations())
                .meta(getMetaData())
                .build();
    }


    // Tool description

    public abstract String getName();

    public abstract String getTitle();

    public abstract String getDescription();

    public abstract McpSchema.JsonSchema getInputSchema();

    public abstract Map<String, Object> getOutputSchema();

    public abstract McpSchema.ToolAnnotations getToolAnnotations();

    public abstract Map<String, Object> getMetaData();

    public abstract BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> getCallHandler();

}
