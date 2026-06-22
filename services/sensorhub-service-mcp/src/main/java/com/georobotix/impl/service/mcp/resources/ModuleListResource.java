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
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.ModuleRegistry;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP Resource that provides a list of all modules in the OSH registry.
 *
 * URI: modules://list
 *
 * Returns a JSON array of module summaries including:
 * - id: Module's local ID
 * - name: Human-readable name
 * - description: Module description
 * - type: Simple class name of the module
 * - state: Current module state (LOADED, STARTED, STOPPED, etc.)
 * - autoStart: Whether the module is configured to auto-start
 */
public class ModuleListResource extends AbstractMcpResource {

    public static final String URI = "modules://list";
    public static final String NAME = "Module List";
    public static final String DESCRIPTION = "List of all modules registered in OpenSensorHub, including their current state and configuration status.";

    private final ModuleRegistry registry;

    public ModuleListResource(ModuleRegistry registry) {
        super();
        this.registry = registry;
    }

    public ModuleListResource(ModuleRegistry registry, Gson gson) {
        super(gson);
        this.registry = registry;
    }

    @Override
    public String getUri() {
        return URI;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public Mono<McpSchema.ReadResourceResult> read(McpAsyncServerExchange exchange) {
        return Mono.fromCallable(() -> {
            List<ModuleSummary> modules = registry.getLoadedModules().stream()
                    .map(this::toSummary)
                    .sorted(Comparator.comparing(ModuleSummary::name, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            ModuleListResponse response = new ModuleListResponse(
                    modules.size(),
                    modules
            );

            String json = gson.toJson(response);

            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(URI, getMimeType(), json))
            );
        });
    }

    private ModuleSummary toSummary(IModule<?> module) {
        ModuleConfig config = module.getConfiguration();

        return new ModuleSummary(
                module.getLocalID(),
                Optional.ofNullable(module.getName()).orElse(""),
                Optional.ofNullable(config).map(c -> c.description).orElse(""),
                module.getClass().getSimpleName(),
                module.getClass().getName(),
                module.getCurrentState().name(),
                config != null && config.autoStart
        );
    }

    /**
     * Response wrapper for the module list.
     */
    public record ModuleListResponse(
            int count,
            List<ModuleSummary> modules
    ) {}

    /**
     * Summary information about a single module.
     */
    public record ModuleSummary(
            String id,
            String name,
            String description,
            String type,
            String className,
            String state,
            boolean autoStart
    ) {}
}