package com.botts.impl.service.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.ModuleRegistry;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModuleTool extends AbstractMCPTool {

    public static final String NAME = "module";
    public static final String TITLE = "Module Tool";
    public static final String DESC = """
        Tool to interact with OSH modules. Modules can be identified by:
        - moduleId: Direct module ID (if known)
        - name: Module name (partial match, case-insensitive)
        - description: Module description (partial match, case-insensitive)  
        - moduleClass: Module class name (partial match on simple or fully qualified name)
        - state: Filter by module state
        
        Use 'find' or 'list' actions first if you're unsure which module to target.
        Mutating actions (start, stop, etc.) require exactly one matching module.
        """;

    private final ModuleRegistry registry;
    private final Gson gson;

    public ModuleTool(ModuleRegistry registry) {
        this.registry = registry;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeNulls()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .create();
    }

    public enum Action {
        FIND,       // Search for modules matching criteria
        LIST,       // List all modules (optionally filtered by state)
        INFO,       // Get detailed info about a specific module
        START,
        STOP,
        RESTART,
        LOAD,
        UNLOAD,
        CONFIGURE
    }

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
        return new McpSchema.JsonSchema(
                "object",
                Map.of(
                        "action", Map.of(
                                "type", "string",
                                "enum", List.of("find", "list", "info", "start", "stop", "restart", "load", "unload", "configure"),
                                "description", "The action to perform"
                        ),
                        "moduleId", Map.of(
                                "type", "string",
                                "description", "Direct module ID (if known)"
                        ),
                        "name", Map.of(
                                "type", "string",
                                "description", "Module name to search for (partial match, case-insensitive)"
                        ),
                        "description", Map.of(
                                "type", "string",
                                "description", "Module description to search for (partial match, case-insensitive)"
                        ),
                        "moduleClass", Map.of(
                                "type", "string",
                                "description", "Module class name to search for (partial match on simple or fully qualified name)"
                        ),
                        "state", Map.of(
                                "type", "string",
                                "enum", List.of("LOADED", "INITIALIZED", "STARTING", "STARTED", "STOPPING", "STOPPED"),
                                "description", "Filter by module state"
                        ),
                        "config", Map.of(
                                "type", "object",
                                "description", "Configuration object as JSON (for 'load' and 'configure' actions). Must include 'objClass' field specifying the ModuleConfig subclass.",
                                "additionalProperties", true
                        )
                ),
                List.of("action"),  // Only action is required
                false,
                Map.of(),
                Map.of()
        );
    }

    @Override
    public Map<String, Object> getOutputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "success", Map.of(
                                "type", "boolean",
                                "description", "Whether the operation succeeded"
                        ),
                        "message", Map.of(
                                "type", "string",
                                "description", "Result message or error description"
                        ),
                        "modules", Map.of(
                                "type", "array",
                                "description", "List of matching modules (for find/list actions)",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "id", Map.of("type", "string"),
                                                "name", Map.of("type", "string"),
                                                "description", Map.of("type", "string"),
                                                "state", Map.of("type", "string"),
                                                "moduleClass", Map.of("type", "string")
                                        )
                                )
                        ),
                        "module", Map.of(
                                "type", "object",
                                "description", "Detailed module information (for info/mutating actions)"
                        ),
                        "config", Map.of(
                                "type", "object",
                                "description", "Full module configuration as JSON"
                        )
                ),
                "required", List.of("success", "message")
        );
    }

    @Override
    public McpSchema.ToolAnnotations getToolAnnotations() {
        return new McpSchema.ToolAnnotations(
                null,   // title
                false,  // readOnlyHint - false because we can mutate
                true,   // destructiveHint - stop/unload are destructive
                false,  // idempotentHint
                false,  // openWorldHint
                false   // localHint
        );
    }

    @Override
    public Map<String, Object> getMetaData() {
        return Map.of(
                "category", "module-management",
                "version", "1.0.0"
        );
    }

    @Override
    public BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> getCallHandler() {
        return (exchange, request) -> Mono.fromCallable(() -> {
            String actionStr = (String) request.get("action");
            Action action = Action.valueOf(actionStr.toUpperCase());

            return switch (action) {
                case FIND -> findModules(request);
                case LIST -> listModules(request);
                case INFO -> getModuleInfo(request);
                case START -> executeOnModule(request, this::startModule);
                case STOP -> executeOnModule(request, this::stopModule);
                case RESTART -> executeOnModule(request, this::restartModule);
                case LOAD -> loadModule(request);
                case UNLOAD -> executeOnModule(request, this::unloadModule);
                case CONFIGURE -> executeOnModule(request, this::configureModule);
            };
        }).onErrorResume(e -> Mono.just(errorResult(e)));
    }

    // ==================== Module Resolution ====================

    private List<IModule<?>> resolveModules(Map<String, Object> args) {
        String moduleId = (String) args.get("moduleId");
        String name = (String) args.get("name");
        String description = (String) args.get("description");
        String moduleClass = (String) args.get("moduleClass");
        String state = (String) args.get("state");

        // Direct ID lookup - fastest path
        if (moduleId != null && !moduleId.isBlank()) {
            IModule<?> module = null;
            try {
                module = registry.getModuleById(moduleId);
            } catch (SensorHubException e) {
                throw new RuntimeException(e);
            }
            return module != null ? List.of(module) : List.of();
        }

        // Filter-based search
        return registry.getLoadedModules().stream()
                .filter(module -> matchesName(module, name))
                .filter(module -> matchesDescription(module, description))
                .filter(module -> matchesClass(module, moduleClass))
                .filter(module -> matchesState(module, state))
                .collect(Collectors.toList());
    }

    private ResolveResult resolveSingleModule(Map<String, Object> args) {
        List<IModule<?>> matches = resolveModules(args);

        if (matches.isEmpty()) {
            return new ResolveResult(null, "No modules found matching the criteria");
        }
        if (matches.size() > 1) {
            String moduleList = matches.stream()
                    .map(m -> String.format("  - %s (id: %s, class: %s)",
                            m.getName(), m.getLocalID(), m.getClass().getSimpleName()))
                    .collect(Collectors.joining("\n"));
            return new ResolveResult(null,
                    String.format("Multiple modules (%d) match the criteria. Please be more specific:\n%s",
                            matches.size(), moduleList));
        }
        return new ResolveResult(matches.get(0), null);
    }

    private record ResolveResult(IModule<?> module, String error) {
        boolean isSuccess() { return module != null; }
    }

    // ==================== Matchers ====================

    private boolean matchesName(IModule<?> module, String pattern) {
        if (pattern == null || pattern.isBlank()) return true;
        String name = module.getName();
        return name != null && containsIgnoreCase(name, pattern);
    }

    private boolean matchesDescription(IModule<?> module, String pattern) {
        if (pattern == null || pattern.isBlank()) return true;
        ModuleConfig config = module.getConfiguration();
        String desc = config != null ? config.description : null;
        return desc != null && containsIgnoreCase(desc, pattern);
    }

    private boolean matchesClass(IModule<?> module, String pattern) {
        if (pattern == null || pattern.isBlank()) return true;
        String fqcn = module.getClass().getName();
        String simpleName = module.getClass().getSimpleName();
        return containsIgnoreCase(fqcn, pattern) || containsIgnoreCase(simpleName, pattern);
    }

    private boolean matchesState(IModule<?> module, String state) {
        if (state == null || state.isBlank()) return true;
        return module.getCurrentState().name().equalsIgnoreCase(state);
    }

    private boolean containsIgnoreCase(String text, String pattern) {
        return Pattern.compile(Pattern.quote(pattern), Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .find();
    }

    // ==================== Actions ====================

    private McpSchema.CallToolResult findModules(Map<String, Object> args) {
        List<IModule<?>> matches = resolveModules(args);

        if (matches.isEmpty()) {
            return resultBuilder()
                    .success(true)
                    .message("No modules found matching the criteria")
                    .modules(List.of())
                    .build();
        }

        return resultBuilder()
                .success(true)
                .message(String.format("Found %d matching module(s)", matches.size()))
                .modules(matches)
                .build();
    }

    private McpSchema.CallToolResult listModules(Map<String, Object> args) {
        String state = (String) args.get("state");

        List<IModule<?>> modules = registry.getLoadedModules().stream()
                .filter(m -> matchesState(m, state))
                .collect(Collectors.toList());

        return resultBuilder()
                .success(true)
                .message(String.format("Found %d module(s)", modules.size()))
                .modules(modules)
                .build();
    }

    private McpSchema.CallToolResult getModuleInfo(Map<String, Object> args) {
        ResolveResult result = resolveSingleModule(args);
        if (!result.isSuccess()) {
            return errorResult(result.error());
        }

        IModule<?> module = result.module();
        return resultBuilder()
                .success(true)
                .message(String.format("Module info for '%s'", module.getName()))
                .moduleDetail(module)
                .config(module.getConfiguration())
                .build();
    }

    private McpSchema.CallToolResult executeOnModule(Map<String, Object> args, ModuleAction action) {
        ResolveResult result = resolveSingleModule(args);
        if (!result.isSuccess()) {
            return errorResult(result.error());
        }
        return action.execute(result.module(), args);
    }

    @FunctionalInterface
    private interface ModuleAction {
        McpSchema.CallToolResult execute(IModule<?> module, Map<String, Object> args);
    }

    private McpSchema.CallToolResult startModule(IModule<?> module, Map<String, Object> args) {
        try {
            module.start();
            return resultBuilder()
                    .success(true)
                    .message(String.format("Module '%s' started successfully", module.getName()))
                    .moduleDetail(module)
                    .build();
        } catch (SensorHubException e) {
            return errorResult("Failed to start module: " + e.getMessage());
        }
    }

    private McpSchema.CallToolResult stopModule(IModule<?> module, Map<String, Object> args) {
        try {
            module.stop();
            return resultBuilder()
                    .success(true)
                    .message(String.format("Module '%s' stopped successfully", module.getName()))
                    .moduleDetail(module)
                    .build();
        } catch (SensorHubException e) {
            return errorResult("Failed to stop module: " + e.getMessage());
        }
    }

    private McpSchema.CallToolResult restartModule(IModule<?> module, Map<String, Object> args) {
        try {
            module.stop();
            module.start();
            return resultBuilder()
                    .success(true)
                    .message(String.format("Module '%s' restarted successfully", module.getName()))
                    .moduleDetail(module)
                    .build();
        } catch (SensorHubException e) {
            return errorResult("Failed to restart module: " + e.getMessage());
        }
    }

    private McpSchema.CallToolResult unloadModule(IModule<?> module, Map<String, Object> args) {
        String name = module.getName();
        String id = module.getLocalID();

        try {
            registry.destroyModule(id);
            return resultBuilder()
                    .success(true)
                    .message(String.format("Module '%s' (id: %s) unloaded successfully", name, id))
                    .build();
        } catch (SensorHubException e) {
            return errorResult("Failed to unload module: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private McpSchema.CallToolResult loadModule(Map<String, Object> args) {
        Map<String, Object> configMap = (Map<String, Object>) args.get("config");

        if (configMap == null || configMap.isEmpty()) {
            return errorResult("config is required for load action");
        }

        // The config must contain objClass for Gson to deserialize to the correct ModuleConfig subclass
        if (!configMap.containsKey("objClass")) {
            // If moduleClass is provided, use it as objClass
            String moduleClass = (String) args.get("moduleClass");
            if (moduleClass != null) {
                // Try to infer config class from module class (convention: ModuleClass -> ModuleClassConfig)
                String configClass = moduleClass + "Config";
                configMap = new LinkedHashMap<>(configMap);
                configMap.put("objClass", configClass);
            } else {
                return errorResult("config must include 'objClass' field specifying the ModuleConfig subclass, " +
                        "or provide 'moduleClass' to infer it");
            }
        }

        try {
            // Convert map to JsonElement then deserialize with Gson
            // This leverages the RuntimeTypeAdapterFactory pattern from ModuleConfigJsonFile
            JsonElement jsonElement = gson.toJsonTree(configMap);

            // Get the config class and deserialize
            String objClass = configMap.get("objClass").toString();

            var installedModules = registry.getInstalledModuleTypes(Class.forName(objClass));
            if (installedModules.isEmpty())
                return errorResult("Could not find module matching object class: " + objClass);

            Class<?> configClass = installedModules.stream().toList().get(0).getModuleConfigClass();

            ModuleConfig config = (ModuleConfig) gson.fromJson(jsonElement, configClass);

            // Generate ID if not provided
            if (config.id == null || config.id.isBlank()) {
                config.id = UUID.randomUUID().toString();
            }

            // Load the module through the registry
            IModule<?> newModule = registry.loadModule(config);

            return resultBuilder()
                    .success(true)
                    .message(String.format("Module '%s' loaded successfully with id: %s",
                            newModule.getName(), newModule.getLocalID()))
                    .moduleDetail(newModule)
                    .config(config)
                    .build();

        } catch (ClassNotFoundException e) {
            return errorResult("Config class not found: " + e.getMessage());
        } catch (SensorHubException e) {
            return errorResult("Failed to load module: " + e.getMessage());
        } catch (Exception e) {
            return errorResult("Failed to parse config: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private McpSchema.CallToolResult configureModule(IModule<?> module, Map<String, Object> args) {
        Map<String, Object> configMap = (Map<String, Object>) args.get("config");

        if (configMap == null || configMap.isEmpty()) {
            return errorResult("config is required for configure action");
        }

        try {
            // Get current config and merge with updates
            ModuleConfig currentConfig = module.getConfiguration();

            // Serialize current config to JSON, merge with updates, then deserialize
            JsonObject currentJson = (JsonObject) gson.toJsonTree(currentConfig);
            JsonObject updates = (JsonObject) gson.toJsonTree(configMap);

            // Merge: updates override current values
            for (Map.Entry<String, JsonElement> entry : updates.entrySet()) {
                currentJson.add(entry.getKey(), entry.getValue());
            }

            // Ensure objClass is preserved for proper deserialization
            if (!currentJson.has("objClass")) {
                currentJson.addProperty("objClass", currentConfig.getClass().getName());
            }

            // Deserialize merged config
            ModuleConfig newConfig = gson.fromJson(
                    currentJson,
                    currentConfig.getClass()
            );

            // Preserve the module ID
            newConfig.id = module.getLocalID();

            // Apply the new configuration
            ((IModule<ModuleConfig>) module).updateConfig(newConfig);

            return resultBuilder()
                    .success(true)
                    .message(String.format("Module '%s' configured successfully", module.getName()))
                    .moduleDetail(module)
                    .config(newConfig)
                    .build();

        } catch (SensorHubException e) {
            return errorResult("Failed to configure module: " + e.getMessage());
        } catch (Exception e) {
            return errorResult("Failed to parse/apply config: " + e.getMessage());
        }
    }

    // ==================== Result Builders ====================

    private ResultBuilder resultBuilder() {
        return new ResultBuilder();
    }

    private class ResultBuilder {
        private final Map<String, Object> result = new LinkedHashMap<>();

        ResultBuilder success(boolean success) {
            result.put("success", success);
            return this;
        }

        ResultBuilder message(String message) {
            result.put("message", message);
            return this;
        }

        ResultBuilder modules(List<IModule<?>> modules) {
            result.put("modules", modules.stream()
                    .map(ModuleTool.this::toModuleSummary)
                    .collect(Collectors.toList()));
            return this;
        }

        ResultBuilder moduleDetail(IModule<?> module) {
            result.put("module", toModuleDetail(module));
            return this;
        }

        ResultBuilder config(ModuleConfig config) {
            if (config != null) {
                // Use Gson to serialize the full config including objClass
                JsonElement configJson = gson.toJsonTree(config);
                result.put("config", gson.fromJson(configJson, Map.class));
            }
            return this;
        }

        McpSchema.CallToolResult build() {
            String json = gson.toJson(result);
            boolean isError = !Boolean.TRUE.equals(result.get("success"));
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(json)), isError);
        }
    }

    private McpSchema.CallToolResult errorResult(String message) {
        return resultBuilder()
                .success(false)
                .message(message)
                .build();
    }

    private McpSchema.CallToolResult errorResult(Throwable e) {
        return errorResult(e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    private Map<String, Object> toModuleSummary(IModule<?> module) {
        ModuleConfig config = module.getConfiguration();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", module.getLocalID());
        summary.put("name", Optional.ofNullable(module.getName()).orElse(""));
        summary.put("description", Optional.ofNullable(config).map(c -> c.description).orElse(""));
        summary.put("state", module.getCurrentState().name());
        summary.put("moduleClass", module.getClass().getSimpleName());
        return summary;
    }

    private Map<String, Object> toModuleDetail(IModule<?> module) {
        ModuleConfig config = module.getConfiguration();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", module.getLocalID());
        detail.put("name", Optional.ofNullable(module.getName()).orElse(""));
        detail.put("description", Optional.ofNullable(config).map(c -> c.description).orElse(""));
        detail.put("state", module.getCurrentState().name());
        detail.put("moduleClass", module.getClass().getName());
        detail.put("configClass", config != null ? config.getClass().getName() : null);
        detail.put("autoStart", config != null && config.autoStart);
        return detail;
    }
}