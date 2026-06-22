/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.georobotix.impl.service.mcp.tools;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;

public abstract class AbstractMcpTool {

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

    public abstract BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> getCallHandler();

}
