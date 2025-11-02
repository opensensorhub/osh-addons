package com.botts.impl.service.mcp.tools;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

public abstract class AbstractMCPTool implements MCPTool{

    private McpSchema.Tool getTool() {
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

    public McpServerFeatures.AsyncToolSpecification getSpecification() {
        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(getTool())
                .callHandler(getCallHandler())
                .build();
    }

}
