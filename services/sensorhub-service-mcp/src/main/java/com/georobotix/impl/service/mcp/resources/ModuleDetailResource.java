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

/**
 * MCP Resource that provides detailed information about a specific module.
 *
 * URI Template: modules://detail/{moduleId}
 *
 * Returns detailed JSON information about a single module including
 * its full configuration.
 */
public class ModuleDetailResource extends AbstractMcpResource {

    public static final String URI_TEMPLATE = "modules://detail/{moduleId}";
    public static final String NAME = "Module Detail";
    public static final String DESCRIPTION = "Detailed information about a specific OpenSensorHub module, including its full configuration.";

    private final ModuleRegistry registry;

    public ModuleDetailResource(ModuleRegistry registry) {
        super();
        this.registry = registry;
    }

    public ModuleDetailResource(ModuleRegistry registry, Gson gson) {
        super(gson);
        this.registry = registry;
    }

    @Override
    public String getUri() {
        return URI_TEMPLATE;
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

    /**
     * Convert to a resource template schema for registration.
     */
    public McpSchema.ResourceTemplate toResourceTemplateSchema() {
        return new McpSchema.ResourceTemplate(
                URI_TEMPLATE,
                NAME,
                getDescription(),
                getMimeType(),
                null  // annotations
        );
    }

    @Override
    public Mono<McpSchema.ReadResourceResult> read(McpAsyncServerExchange exchange) {
        return Mono.error(new UnsupportedOperationException(
                "Use readWithId() for template resources"
        ));
    }

    /**
     * Read module details for a specific module ID.
     */
    public Mono<McpSchema.ReadResourceResult> readWithId(String moduleId) {
        return Mono.fromCallable(() -> {
            IModule<?> module = registry.getModuleById(moduleId);

            if (module == null) {
                throw new NoSuchElementException("Module not found: " + moduleId);
            }

            ModuleDetail detail = toDetail(module);
            String json = gson.toJson(detail);
            String resolvedUri = URI_TEMPLATE.replace("{moduleId}", moduleId);

            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(resolvedUri, getMimeType(), json))
            );
        });
    }

    private ModuleDetail toDetail(IModule<?> module) {
        ModuleConfig config = module.getConfiguration();

        Map<String, Object> configMap = new LinkedHashMap<>();
        if (config != null) {
            configMap.put("id", config.id);
            configMap.put("name", config.name);
            configMap.put("description", config.description);
            configMap.put("autoStart", config.autoStart);
            configMap.put("configClass", config.getClass().getName());

            // Serialize full config to capture all fields
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> fullConfig = gson.fromJson(gson.toJson(config), Map.class);
                configMap.put("properties", fullConfig);
            } catch (Exception e) {
                configMap.put("properties", Map.of("error", "Failed to serialize: " + e.getMessage()));
            }
        }

        return new ModuleDetail(
                module.getLocalID(),
                Optional.ofNullable(module.getName()).orElse(""),
                Optional.ofNullable(config).map(c -> c.description).orElse(""),
                module.getClass().getName(),
                module.getClass().getSimpleName(),
                module.getCurrentState().name(),
                module.getCurrentError() != null ? module.getCurrentError().getMessage() : null,
                configMap
        );
    }

    /**
     * Detailed information about a module.
     */
    public record ModuleDetail(
            String id,
            String name,
            String description,
            String className,
            String type,
            String state,
            String errorMessage,
            Map<String, Object> configuration
    ) {}
}