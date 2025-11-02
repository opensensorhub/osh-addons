package com.botts.impl.service.mcp.tools;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ModuleTool extends AbstractMCPTool {

    public static String NAME = "module";
    public static String TITLE = "Module Tool";
    public static String DESC = "Tool to interact with OSH modules";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getDescription() {
        return DESC;
    }

    @Override
    public McpSchema.JsonSchema getInputSchema() {
        return new McpSchema.JsonSchema("title",
                Map.of("", ""),
                List.of("requiredProps"),
                true,
                ;
    }

    @Override
    public Map<String, Object> getOutputSchema() {
        return Map.of();
    }

    @Override
    public McpSchema.ToolAnnotations getToolAnnotations() {
        return null;
    }

    @Override
    public Map<String, Object> getMetaData() {
        return Map.of();
    }

    @Override
    public BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> getCallHandler() {
        return null;
    }
}
