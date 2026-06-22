/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.georobotix.impl.service.mcp.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

/**
 * Abstract base class for MCP resources.
 */
public abstract class AbstractMcpResource {

    protected final Gson gson;

    protected AbstractMcpResource() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();
    }

    protected AbstractMcpResource(Gson gson) {
        this.gson = gson;
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
        return McpSchema.Resource.builder()
                .uri(getUri())
                .name(getName())
                .description(getDescription())
                .mimeType(getMimeType())
                .build();
    }
}