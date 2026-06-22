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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.AggregateProcess;
import net.opengis.sensorml.v20.Settings;
import net.opengis.sensorml.v20.impl.SettingsImpl;
import org.vast.sensorML.SimpleProcessImpl;
import net.opengis.sensorml.v20.impl.ValueSettingImpl;
import net.opengis.gml.v32.impl.ReferenceImpl;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.processing.IProcessProvider;
import org.sensorhub.api.processing.IProcessingManager;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.processing.CommandStreamSink;
import org.sensorhub.impl.processing.DataStreamSource;
import org.sensorhub.impl.processing.SMLProcessConfig;
import org.sensorhub.impl.processing.SMLProcessImpl;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessInfo;
import org.vast.sensorML.AggregateProcessImpl;
import org.vast.sensorML.LinkImpl;
import org.vast.sensorML.SMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;


/**
 * MCP Tool for creating SensorML process chains through natural language.
 * Allows listing available process implementations, building AggregateProcess
 * descriptions, adding components and connections, and loading them into
 * SMLProcessImpl modules for execution.
 */
public class ProcessTool extends AbstractMcpTool {

    private static final Logger log = LoggerFactory.getLogger(ProcessTool.class);

    public static final String NAME = "process";
    public static final String TITLE = "Process Chain Tool";
    public static final String DESC = """
        Tool to create and manage SensorML process chains in OpenSensorHub.

        A process chain (AggregateProcess) connects multiple processing components
        together via data connections. Each component has inputs, outputs, and parameters.

        Workflow for building a process chain:
        1. Use 'list_processes' to see available process implementations
        2. Use 'create_chain' to start a new empty AggregateProcess
        3. Use 'add_datasource' to add data source components (reads from system datastreams)
        4. Use 'add_commandsink' to add command sink components (sends commands to systems)
        5. Use 'add_component' to add processing components (math, filters, etc.)
        6. Use 'add_connection' to connect component outputs to inputs
        7. Use 'configure_parameter' to set component parameters
        8. Use 'get_chain' to inspect the current chain state
        9. Use 'get_connection_paths' to see all available connection endpoints
        10. Use 'save_and_load' to save the SensorML file and load it as an SMLProcessImpl module

        Connection path format: "components/{componentName}/inputs/{inputName}" or
        "components/{componentName}/outputs/{outputName}"
        """;

    private final ISensorHub hub;
    private final IProcessingManager processingManager;
    private final ModuleRegistry moduleRegistry;
    private final Gson gson;

    // In-memory process chains being built, keyed by name
    private final Map<String, AggregateProcessImpl> activeChains = new LinkedHashMap<>();

    // Resolved executable process instances per component, keyed by "chainName/componentName"
    // Used to discover I/O structure since simplified SensorML components don't inline I/O
    private final Map<String, ExecutableProcessImpl> resolvedOutputs = new LinkedHashMap<>();

    public ProcessTool(ISensorHub hub) {
        this.hub = hub;
        this.processingManager = hub.getProcessingManager();
        this.moduleRegistry = hub.getModuleRegistry();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();
    }

    public enum Action {
        LIST_PROCESSES,       // List all available process implementations
        CREATE_CHAIN,         // Create a new empty AggregateProcess
        ADD_DATASOURCE,       // Add a DataStreamSource component
        ADD_COMMANDSINK,      // Add a CommandStreamSink component
        ADD_COMPONENT,        // Add a processing component by URI
        ADD_CONNECTION,       // Connect two component ports
        CONFIGURE_PARAMETER,  // Set a parameter value on a component
        ADD_INPUT,            // Add an input to the aggregate process
        ADD_OUTPUT,           // Add an output to the aggregate process
        REMOVE_COMPONENT,     // Remove a component from the chain
        REMOVE_CONNECTION,    // Remove a connection
        GET_CHAIN,            // Get current state of a chain
        GET_CONNECTION_PATHS, // List all possible connection endpoints
        SAVE_AND_LOAD,        // Save to SensorML file and load as module
        LIST_CHAINS           // List all active in-memory chains
    }

    @Override
    public String getName() { return NAME; }

    @Override
    public String getTitle() { return TITLE; }

    @Override
    public String getDescription() { return DESC; }

    @Override
    public McpSchema.JsonSchema getInputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("action", Map.of(
                "type", "string",
                "enum", List.of("list_processes", "create_chain", "add_datasource",
                        "add_commandsink", "add_component", "add_connection",
                        "configure_parameter", "add_input", "add_output",
                        "remove_component", "remove_connection",
                        "get_chain", "get_connection_paths", "save_and_load", "list_chains"),
                "description", "The action to perform"
        ));
        properties.put("chainName", Map.of("type", "string", "description", "Name of the process chain (used as identifier)"));
        properties.put("componentName", Map.of("type", "string", "description", "Name for the component being added (must be unique within the chain)"));
        properties.put("processURI", Map.of("type", "string", "description", "URI of the process implementation to use (from list_processes)"));
        properties.put("systemUID", Map.of("type", "string", "description", "System unique ID for datasource/commandsink components"));
        properties.put("outputName", Map.of("type", "string", "description", "Output name on the source system (for datasource)"));
        properties.put("inputName", Map.of("type", "string", "description", "Input name on the target system (for commandsink)"));
        properties.put("source", Map.of("type", "string", "description", "Source connection path (e.g. 'components/sensor/outputs/temperature')"));
        properties.put("destination", Map.of("type", "string", "description", "Destination connection path (e.g. 'components/filter/inputs/value')"));
        properties.put("parameterPath", Map.of("type", "string", "description", "Parameter path relative to the component (e.g. 'parameters/threshold')"));
        properties.put("parameterValue", Map.of("type", "string", "description", "Value to set for the parameter"));
        properties.put("filePath", Map.of("type", "string", "description", "File path for saving the SensorML description (for save_and_load)"));
        properties.put("autoStart", Map.of("type", "boolean", "description", "Whether to auto-start the loaded module (default true)"));
        properties.put("uniqueID", Map.of("type", "string", "description", "Unique identifier (URI) for the process chain"));

        return new McpSchema.JsonSchema(
                "object",
                properties,
                List.of("action"),
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
                        "success", Map.of("type", "boolean"),
                        "message", Map.of("type", "string"),
                        "processes", Map.of("type", "array", "description", "Available process implementations"),
                        "chain", Map.of("type", "object", "description", "Process chain state"),
                        "paths", Map.of("type", "array", "description", "Connection endpoint paths")
                ),
                "required", List.of("success", "message")
        );
    }

    @Override
    public McpSchema.ToolAnnotations getToolAnnotations() {
        return new McpSchema.ToolAnnotations(
                null,
                false,  // readOnlyHint
                true,   // destructiveHint
                false,  // idempotentHint
                false,  // openWorldHint
                false   // localHint
        );
    }

    @Override
    public Map<String, Object> getMetaData() {
        return Map.of(
                "category", "processing",
                "version", "1.0.0"
        );
    }

    @Override
    public BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> getCallHandler() {
        return (exchange, callRequest) -> Mono.fromCallable(() -> {
            Map<String, Object> request = callRequest.arguments();
            String actionStr = (String) request.get("action");
            Action action = Action.valueOf(actionStr.toUpperCase());

            return switch (action) {
                case LIST_PROCESSES -> listProcesses();
                case CREATE_CHAIN -> createChain(request);
                case ADD_DATASOURCE -> addDataSource(request);
                case ADD_COMMANDSINK -> addCommandSink(request);
                case ADD_COMPONENT -> addComponent(request);
                case ADD_CONNECTION -> addConnection(request);
                case CONFIGURE_PARAMETER -> configureParameter(request);
                case ADD_INPUT -> addInput(request);
                case ADD_OUTPUT -> addOutput(request);
                case REMOVE_COMPONENT -> removeComponent(request);
                case REMOVE_CONNECTION -> removeConnection(request);
                case GET_CHAIN -> getChain(request);
                case GET_CONNECTION_PATHS -> getConnectionPaths(request);
                case SAVE_AND_LOAD -> saveAndLoad(request);
                case LIST_CHAINS -> listChains();
            };
        }).onErrorResume(e -> Mono.just(errorResult(e)));
    }

    // ==================== Actions ====================

    private McpSchema.CallToolResult listProcesses() {
        List<Map<String, Object>> processes = new ArrayList<>();

        // Add built-in DataStreamSource and CommandStreamSink
        Map<String, Object> dsSource = new LinkedHashMap<>();
        dsSource.put("uri", DataStreamSource.INFO.getUri());
        dsSource.put("name", DataStreamSource.INFO.getName());
        dsSource.put("description", "Reads observations from a system's data stream output. Requires systemUID and outputName parameters.");
        dsSource.put("type", "datasource");
        processes.add(dsSource);

        Map<String, Object> cmdSink = new LinkedHashMap<>();
        cmdSink.put("uri", CommandStreamSink.INFO.getUri());
        cmdSink.put("name", CommandStreamSink.INFO.getName());
        cmdSink.put("description", "Sends commands to a system's control input. Requires systemUID and inputName parameters.");
        cmdSink.put("type", "commandsink");
        processes.add(cmdSink);

        // Add all registered process providers
        for (IProcessProvider provider : processingManager.getAllProcessingPackages()) {
            Map<String, ProcessInfo> processMap = provider.getProcessMap();
            if (processMap != null) {
                for (Map.Entry<String, ProcessInfo> entry : processMap.entrySet()) {
                    ProcessInfo info = entry.getValue();
                    Map<String, Object> procInfo = new LinkedHashMap<>();
                    procInfo.put("uri", info.getUri());
                    procInfo.put("name", info.getName());
                    procInfo.put("description", info.getDescription());
                    procInfo.put("type", "process");

                    // Show inputs/outputs/parameters of the process
                    try {
                        var impl = info.getImplementationClass().getDeclaredConstructor().newInstance();
                        procInfo.put("inputs", describeIOList(impl.getInputList()));
                        procInfo.put("outputs", describeIOList(impl.getOutputList()));
                        procInfo.put("parameters", describeIOList(impl.getParameterList()));
                    } catch (Exception e) {
                        // Some processes may not be instantiable without context
                        log.debug("Could not instantiate process {} for inspection: {}", info.getUri(), e.getMessage());
                    }

                    processes.add(procInfo);
                }
            }
        }

        return resultBuilder()
                .success(true)
                .message(String.format("Found %d available process implementation(s)", processes.size()))
                .put("processes", processes)
                .build();
    }

    private McpSchema.CallToolResult createChain(Map<String, Object> args) {
        String chainName = (String) args.get("chainName");
        if (chainName == null || chainName.isBlank())
            return errorResult("chainName is required for create_chain");

        if (activeChains.containsKey(chainName))
            return errorResult("A chain with name '" + chainName + "' already exists. Use a different name or get_chain to inspect it.");

        AggregateProcessImpl chain = new AggregateProcessImpl();

        String uniqueID = (String) args.get("uniqueID");
        if (uniqueID != null && !uniqueID.isBlank())
            chain.setUniqueIdentifier(uniqueID);
        else
            chain.setUniqueIdentifier("urn:osh:process:" + chainName + ":" + UUID.randomUUID());

        chain.setName(chainName);

        activeChains.put(chainName, chain);

        return resultBuilder()
                .success(true)
                .message(String.format("Created process chain '%s' with UID '%s'", chainName, chain.getUniqueIdentifier()))
                .put("chain", serializeChain(chain, (String) args.get("chainName")))
                .build();
    }

    private McpSchema.CallToolResult addDataSource(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        String chainName = (String) args.get("chainName");
        String componentName = (String) args.get("componentName");
        if (componentName == null || componentName.isBlank())
            return errorResult("componentName is required");

        String systemUID = (String) args.get("systemUID");
        if (systemUID == null || systemUID.isBlank())
            return errorResult("systemUID is required for add_datasource");

        String outputName = (String) args.get("outputName");
        if (outputName == null || outputName.isBlank())
            return errorResult("outputName is required for add_datasource");

        // Strip nested path from outputName if present
        if (outputName.contains("/"))
            outputName = outputName.substring(0, outputName.indexOf("/"));

        // Resolve the executable DataStreamSource to discover the output structure,
        // but build a simplified SensorML component with only typeOf + settings.
        DataStreamSource dataSource = new DataStreamSource();
        for (var param : dataSource.getParameterList())
            ((DataComponent) param).assignNewDataBlock();
        dataSource.getParameterList()
                .getComponent(DataStreamSource.SYSTEM_UID_PARAM)
                .getData()
                .setStringValue(systemUID);
        dataSource.getParameterList()
                .getComponent(DataStreamSource.OUTPUT_NAME_PARAM)
                .getData()
                .setStringValue(outputName);
        dataSource.setParentHub(hub);
        dataSource.notifyParamChange();

        // Store the resolved output structure for later use (output resolution, connection paths)
        resolvedOutputs.put(chainName + "/" + componentName, dataSource);

        // Build simplified SensorML: typeOf + configuration only (no inlined I/O)
        SimpleProcessImpl process = new SimpleProcessImpl();
        process.setName(dataSource.getProcessInfo().getName());
        process.setTypeOf(new ReferenceImpl(DataStreamSource.INFO.getUri()));

        Settings paramSettings = new SettingsImpl();
        paramSettings.addSetValue(new ValueSettingImpl("parameters/" + DataStreamSource.SYSTEM_UID_PARAM, systemUID));
        paramSettings.addSetValue(new ValueSettingImpl("parameters/" + DataStreamSource.OUTPUT_NAME_PARAM, outputName));
        process.setConfiguration(paramSettings);

        chain.addComponent(componentName, process);

        return resultBuilder()
                .success(true)
                .message(String.format("Added data source '%s' reading from %s/%s", componentName, systemUID, outputName))
                .put("chain", serializeChain(chain, (String) args.get("chainName")))
                .build();
    }

    private McpSchema.CallToolResult addCommandSink(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        String chainName = (String) args.get("chainName");
        String componentName = (String) args.get("componentName");
        if (componentName == null || componentName.isBlank())
            return errorResult("componentName is required");

        String systemUID = (String) args.get("systemUID");
        if (systemUID == null || systemUID.isBlank())
            return errorResult("systemUID is required for add_commandsink");

        String inputName = (String) args.get("inputName");
        if (inputName == null || inputName.isBlank())
            return errorResult("inputName is required for add_commandsink");

        // Resolve the executable CommandStreamSink to discover I/O structure
        CommandStreamSink sink = new CommandStreamSink();
        for (var param : sink.getParameterList())
            ((DataComponent) param).assignNewDataBlock();
        sink.getParameterList()
                .getComponent(CommandStreamSink.SYSTEM_UID_PARAM)
                .getData()
                .setStringValue(systemUID);
        sink.getParameterList()
                .getComponent(CommandStreamSink.OUTPUT_NAME_PARAM)
                .getData()
                .setStringValue(inputName);
        sink.setParentHub(hub);
        sink.notifyParamChange();

        // Store for connection path resolution
        resolvedOutputs.put(chainName + "/" + componentName, sink);

        // Build simplified SensorML: typeOf + configuration only
        SimpleProcessImpl process = new SimpleProcessImpl();
        process.setName(sink.getProcessInfo().getName());
        process.setTypeOf(new ReferenceImpl(CommandStreamSink.INFO.getUri()));

        Settings paramSettings = new SettingsImpl();
        paramSettings.addSetValue(new ValueSettingImpl("parameters/" + CommandStreamSink.SYSTEM_UID_PARAM, systemUID));
        paramSettings.addSetValue(new ValueSettingImpl("parameters/" + CommandStreamSink.OUTPUT_NAME_PARAM, inputName));
        process.setConfiguration(paramSettings);

        chain.addComponent(componentName, process);

        return resultBuilder()
                .success(true)
                .message(String.format("Added command sink '%s' sending to %s/%s", componentName, systemUID, inputName))
                .put("chain", serializeChain(chain, (String) args.get("chainName")))
                .build();
    }

    private McpSchema.CallToolResult addComponent(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        String chainName = (String) args.get("chainName");
        String componentName = (String) args.get("componentName");
        if (componentName == null || componentName.isBlank())
            return errorResult("componentName is required");

        String processURI = (String) args.get("processURI");
        if (processURI == null || processURI.isBlank())
            return errorResult("processURI is required for add_component");

        try {
            // Find the ProcessInfo by URI
            ProcessInfo processInfo = findProcessInfo(processURI);
            if (processInfo == null)
                return errorResult("Process implementation not found for URI: " + processURI);

            // Instantiate the executable to discover I/O structure
            var impl = (ExecutableProcessImpl) processInfo.getImplementationClass()
                    .getDeclaredConstructor().newInstance();
            resolvedOutputs.put(chainName + "/" + componentName, impl);

            // Build simplified SensorML: typeOf only (no inlined I/O)
            SimpleProcessImpl process = new SimpleProcessImpl();
            process.setName(processInfo.getName());
            process.setTypeOf(new ReferenceImpl(processURI));

            chain.addComponent(componentName, process);

            return resultBuilder()
                    .success(true)
                    .message(String.format("Added component '%s' (process: %s)", componentName, processInfo.getName()))
                    .put("chain", serializeChain(chain, (String) args.get("chainName")))
                    .build();
        } catch (Exception e) {
            return errorResult("Failed to add component: " + e.getMessage());
        }
    }

    private McpSchema.CallToolResult addConnection(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        String source = (String) args.get("source");
        String destination = (String) args.get("destination");
        if (source == null || source.isBlank() || destination == null || destination.isBlank())
            return errorResult("Both 'source' and 'destination' connection paths are required");

        chain.addConnection(new LinkImpl(source, destination));

        return resultBuilder()
                .success(true)
                .message(String.format("Added connection: %s -> %s", source, destination))
                .put("chain", serializeChain(chain, (String) args.get("chainName")))
                .build();
    }

    private McpSchema.CallToolResult configureParameter(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        String componentName = (String) args.get("componentName");
        if (componentName == null || componentName.isBlank())
            return errorResult("componentName is required");

        String parameterPath = (String) args.get("parameterPath");
        if (parameterPath == null || parameterPath.isBlank())
            return errorResult("parameterPath is required (e.g. 'parameters/threshold')");

        String parameterValue = (String) args.get("parameterValue");
        if (parameterValue == null)
            return errorResult("parameterValue is required");

        try {
            AbstractProcess component = chain.getComponent(componentName);
            if (component == null)
                return errorResult("Component not found: " + componentName);

            // Add a setValue to the component's Settings configuration
            // This works with both simplified (typeOf) and verbose SensorML formats
            Settings settings = component.getConfiguration();
            if (settings == null) {
                settings = new SettingsImpl();
                component.setConfiguration(settings);
            }
            settings.addSetValue(new ValueSettingImpl(parameterPath, parameterValue));

            return resultBuilder()
                    .success(true)
                    .message(String.format("Set parameter '%s' on component '%s' to '%s'", parameterPath, componentName, parameterValue))
                    .put("chain", serializeChain(chain, (String) args.get("chainName")))
                    .build();
        } catch (Exception e) {
            return errorResult("Failed to configure parameter: " + e.getMessage());
        }
    }

    private McpSchema.CallToolResult addInput(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        String chainName = (String) args.get("chainName");
        String inputName = (String) args.get("componentName");
        if (inputName == null || inputName.isBlank())
            return errorResult("componentName (used as input name) is required");

        // Optionally resolve structure from a source component's input
        String source = (String) args.get("source");
        DataComponent input = resolveDataComponent(chainName, source);
        if (input != null) {
            input = input.copy();
            input.setName(inputName);
        } else {
            // Fallback: create an empty DataRecord placeholder
            input = new org.vast.data.DataRecordImpl();
            input.setName(inputName);
        }
        chain.addInput(inputName, input);

        return resultBuilder()
                .success(true)
                .message(String.format("Added input '%s' to chain", inputName))
                .put("chain", serializeChain(chain, (String) args.get("chainName")))
                .build();
    }

    private McpSchema.CallToolResult addOutput(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        String chainName = (String) args.get("chainName");
        String outputName = (String) args.get("componentName");
        if (outputName == null || outputName.isBlank())
            return errorResult("componentName (used as output name) is required");

        // Optionally resolve structure from a source component's output
        String source = (String) args.get("source");
        DataComponent output = resolveDataComponent(chainName, source);
        if (output != null) {
            output = output.copy();
            output.setName(outputName);
        } else {
            // Fallback: create an empty DataRecord placeholder
            output = new org.vast.data.DataRecordImpl();
            output.setName(outputName);
        }
        chain.addOutput(outputName, output);

        return resultBuilder()
                .success(true)
                .message(String.format("Added output '%s' to chain", outputName))
                .put("chain", serializeChain(chain, (String) args.get("chainName")))
                .build();
    }

    private McpSchema.CallToolResult removeComponent(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        String componentName = (String) args.get("componentName");
        if (componentName == null || componentName.isBlank())
            return errorResult("componentName is required");

        String chainName = (String) args.get("chainName");
        chain.getComponentList().remove(componentName);
        resolvedOutputs.remove(chainName + "/" + componentName);

        return resultBuilder()
                .success(true)
                .message(String.format("Removed component '%s'", componentName))
                .put("chain", serializeChain(chain, chainName))
                .build();
    }

    private McpSchema.CallToolResult removeConnection(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        String source = (String) args.get("source");
        String destination = (String) args.get("destination");
        if (source == null || destination == null)
            return errorResult("Both 'source' and 'destination' are required to identify the connection to remove");

        for (int i = 0; i < chain.getNumConnections(); i++) {
            var conn = chain.getConnectionList().get(i);
            if (conn.getSource().equals(source) && conn.getDestination().equals(destination)) {
                chain.getConnectionList().remove(i);
                return resultBuilder()
                        .success(true)
                        .message(String.format("Removed connection: %s -> %s", source, destination))
                        .put("chain", serializeChain(chain, (String) args.get("chainName")))
                        .build();
            }
        }

        return errorResult("Connection not found: " + source + " -> " + destination);
    }

    private McpSchema.CallToolResult getChain(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        return resultBuilder()
                .success(true)
                .message("Process chain state")
                .put("chain", serializeChain(chain, (String) args.get("chainName")))
                .build();
    }

    private McpSchema.CallToolResult getConnectionPaths(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        String chainName = (String) args.get("chainName");
        List<String> paths = collectConnectionPaths(chain, chainName);

        return resultBuilder()
                .success(true)
                .message(String.format("Found %d connection endpoint(s)", paths.size()))
                .put("paths", paths)
                .build();
    }

    private McpSchema.CallToolResult saveAndLoad(Map<String, Object> args) {
        AggregateProcessImpl chain = resolveChain(args);
        if (chain == null) return errorResult("chainName is required and must reference an existing chain");

        String filePath = (String) args.get("filePath");
        if (filePath == null || filePath.isBlank())
            return errorResult("filePath is required for save_and_load");

        String chainName = (String) args.get("chainName");

        try {
            // Auto-resolve any empty top-level outputs from connection sources
            for (int i = 0; i < chain.getNumConnections(); i++) {
                var conn = chain.getConnectionList().get(i);
                String dest = conn.getDestination();
                // If connection destination is a top-level output, resolve its structure
                if (dest.startsWith("outputs/")) {
                    String outName = dest.substring("outputs/".length());
                    // Check if the output exists but is empty (no fields)
                    DataComponent existingOut = null;
                    try { existingOut = chain.getOutputList().getComponent(outName); } catch (Exception ignored) {}
                    if (existingOut != null && existingOut.getComponentCount() == 0) {
                        // Resolve from the source connection path
                        DataComponent resolved = resolveDataComponent(chainName, conn.getSource());
                        if (resolved != null) {
                            DataComponent copy = resolved.copy();
                            copy.setName(outName);
                            chain.getOutputList().remove(outName);
                            chain.addOutput(outName, copy);
                        }
                    } else if (existingOut == null) {
                        // Output doesn't exist yet, create it from source
                        DataComponent resolved = resolveDataComponent(chainName, conn.getSource());
                        if (resolved != null) {
                            DataComponent copy = resolved.copy();
                            copy.setName(outName);
                            chain.addOutput(outName, copy);
                        }
                    }
                }
                // Same for inputs
                if (dest.startsWith("inputs/")) {
                    String inName = dest.substring("inputs/".length());
                    DataComponent existingIn = null;
                    try { existingIn = chain.getInputList().getComponent(inName); } catch (Exception ignored) {}
                    if (existingIn != null && existingIn.getComponentCount() == 0) {
                        DataComponent resolved = resolveDataComponent(chainName, conn.getSource());
                        if (resolved != null) {
                            DataComponent copy = resolved.copy();
                            copy.setName(inName);
                            chain.getInputList().remove(inName);
                            chain.addInput(inName, copy);
                        }
                    }
                }
            }

            // Save to SensorML XML file
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(filePath))) {
                new SMLUtils(SMLUtils.V2_0).writeProcess(os, chain, true);
            }

            // Create SMLProcessConfig and load as module
            SMLProcessConfig config = new SMLProcessConfig();
            config.id = UUID.randomUUID().toString();
            config.name = chain.getName() != null ? chain.getName() : "Process Chain";
            config.sensorML = filePath;
            config.moduleClass = SMLProcessImpl.class.getName();

            Object autoStartObj = args.get("autoStart");
            config.autoStart = autoStartObj == null || Boolean.TRUE.equals(autoStartObj);

            IModule<?> module = moduleRegistry.loadModule(config);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("moduleId", module.getLocalID());
            result.put("moduleName", module.getName());
            result.put("sensorMLPath", filePath);
            result.put("state", module.getCurrentState().name());

            // Remove from active chains and resolved outputs since it's now persisted
            activeChains.remove(chainName);
            resolvedOutputs.entrySet().removeIf(e -> e.getKey().startsWith(chainName + "/"));

            return resultBuilder()
                    .success(true)
                    .message(String.format("Saved process chain to '%s' and loaded as module '%s' (id: %s)",
                            filePath, module.getName(), module.getLocalID()))
                    .put("module", result)
                    .build();
        } catch (Exception e) {
            return errorResult("Failed to save and load process chain: " + e.getMessage());
        }
    }

    private McpSchema.CallToolResult listChains() {
        List<Map<String, Object>> chains = new ArrayList<>();
        for (Map.Entry<String, AggregateProcessImpl> entry : activeChains.entrySet()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", entry.getKey());
            info.put("uniqueID", entry.getValue().getUniqueIdentifier());
            info.put("numComponents", entry.getValue().getNumComponents());
            info.put("numConnections", entry.getValue().getNumConnections());
            chains.add(info);
        }

        return resultBuilder()
                .success(true)
                .message(String.format("Found %d active chain(s)", chains.size()))
                .put("chains", chains)
                .build();
    }

    // ==================== Helpers ====================

    private AggregateProcessImpl resolveChain(Map<String, Object> args) {
        String chainName = (String) args.get("chainName");
        if (chainName == null || chainName.isBlank()) return null;
        return activeChains.get(chainName);
    }

    /**
     * Resolves a DataComponent from a connection-style path using resolvedOutputs.
     * Path format: "components/{componentName}/outputs/{outputName}" or
     * "components/{componentName}/inputs/{inputName}"
     */
    private DataComponent resolveDataComponent(String chainName, String path) {
        if (path == null || path.isBlank()) return null;

        String[] parts = path.split("/");
        // Expected: components/{name}/{ioType}/{ioName}[/nested...]
        if (parts.length < 4 || !parts[0].equals("components")) return null;

        String compName = parts[1];
        String ioType = parts[2];   // "inputs", "outputs", or "parameters"
        String ioName = parts[3];

        ExecutableProcessImpl exec = resolvedOutputs.get(chainName + "/" + compName);
        if (exec == null) return null;

        var ioList = switch (ioType) {
            case "inputs" -> exec.getInputList();
            case "outputs" -> exec.getOutputList();
            case "parameters" -> exec.getParameterList();
            default -> null;
        };
        if (ioList == null) return null;

        DataComponent comp = ioList.getComponent(ioName);
        if (comp == null) return null;

        // Navigate nested path if present
        for (int i = 4; i < parts.length; i++) {
            comp = comp.getComponent(parts[i]);
            if (comp == null) return null;
        }
        return comp;
    }

    private ProcessInfo findProcessInfo(String uri) {
        for (IProcessProvider provider : processingManager.getAllProcessingPackages()) {
            Map<String, ProcessInfo> processMap = provider.getProcessMap();
            if (processMap != null && processMap.containsKey(uri)) {
                return processMap.get(uri);
            }
        }
        return null;
    }

    private List<Map<String, Object>> describeIOList(net.opengis.OgcPropertyList<?> ioList) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < ioList.size(); i++) {
            Object item = ioList.get(i);
            if (item instanceof DataComponent comp) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", comp.getName());
                if (comp.getLabel() != null) info.put("label", comp.getLabel());
                if (comp.getDescription() != null) info.put("description", comp.getDescription());
                info.put("type", comp.getClass().getSimpleName());
                if (comp.getComponentCount() > 0) {
                    List<Map<String, Object>> fields = new ArrayList<>();
                    for (int j = 0; j < comp.getComponentCount(); j++) {
                        DataComponent child = comp.getComponent(j);
                        Map<String, Object> fieldInfo = new LinkedHashMap<>();
                        fieldInfo.put("name", child.getName());
                        fieldInfo.put("type", child.getClass().getSimpleName());
                        if (child.getLabel() != null) fieldInfo.put("label", child.getLabel());
                        fields.add(fieldInfo);
                    }
                    info.put("fields", fields);
                }
                result.add(info);
            }
        }
        return result;
    }

    private List<String> collectConnectionPaths(AggregateProcess process, String chainName) {
        List<String> paths = new ArrayList<>();
        collectFromIOList("inputs", process.getInputList(), paths);
        collectFromIOList("outputs", process.getOutputList(), paths);
        collectFromIOList("parameters", process.getParameterList(), paths);

        for (int i = 0; i < process.getNumComponents(); i++) {
            var prop = process.getComponentList().getProperties().get(i);
            String compName = prop.getName();
            String prefix = "components/" + compName + "/";

            // Use resolved executable I/O if available (simplified SensorML won't have inlined I/O)
            ExecutableProcessImpl exec = resolvedOutputs.get(chainName + "/" + compName);
            if (exec != null) {
                List<String> compPaths = new ArrayList<>();
                collectFromExecIOList("inputs", exec.getInputList(), compPaths);
                collectFromExecIOList("outputs", exec.getOutputList(), compPaths);
                collectFromExecIOList("parameters", exec.getParameterList(), compPaths);
                for (String p : compPaths) {
                    paths.add(prefix + p);
                }
            } else {
                // Fallback to SensorML component I/O (for verbose components)
                AbstractProcess component = prop.getValue();
                List<String> compPaths = new ArrayList<>();
                collectFromIOList("inputs", component.getInputList(), compPaths);
                collectFromIOList("outputs", component.getOutputList(), compPaths);
                collectFromIOList("parameters", component.getParameterList(), compPaths);
                for (String p : compPaths) {
                    paths.add(prefix + p);
                }
            }
        }
        return paths;
    }

    private void collectFromExecIOList(String prefix, net.opengis.OgcPropertyList<?> ioList, List<String> paths) {
        for (int i = 0; i < ioList.size(); i++) {
            Object item = ioList.get(i);
            if (item instanceof DataComponent component) {
                String topPath = prefix + "/" + component.getName();
                paths.add(topPath);
                collectNestedPaths(topPath, component, paths);
            }
        }
    }

    private void collectFromIOList(String prefix, net.opengis.sensorml.v20.IOPropertyList ioList, List<String> paths) {
        for (int i = 0; i < ioList.size(); i++) {
            DataComponent component = ioList.getComponent(i);
            String topPath = prefix + "/" + component.getName();
            paths.add(topPath);
            collectNestedPaths(topPath, component, paths);
        }
    }

    private void collectNestedPaths(String parentPath, DataComponent component, List<String> paths) {
        for (int i = 0; i < component.getComponentCount(); i++) {
            DataComponent child = component.getComponent(i);
            String childPath = parentPath + "/" + child.getName();
            paths.add(childPath);
            collectNestedPaths(childPath, child, paths);
        }
    }

    // ==================== Serialization ====================

    private Map<String, Object> serializeChain(AggregateProcess chain, String chainName) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", chain.getName());
        map.put("uniqueID", chain.getUniqueIdentifier());

        // Inputs
        List<Map<String, Object>> inputs = new ArrayList<>();
        for (int i = 0; i < chain.getNumInputs(); i++) {
            DataComponent input = chain.getInputList().getComponent(i);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", input.getName());
            info.put("type", input.getClass().getSimpleName());
            inputs.add(info);
        }
        map.put("inputs", inputs);

        // Outputs
        List<Map<String, Object>> outputs = new ArrayList<>();
        for (int i = 0; i < chain.getNumOutputs(); i++) {
            DataComponent output = chain.getOutputList().getComponent(i);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", output.getName());
            info.put("type", output.getClass().getSimpleName());
            outputs.add(info);
        }
        map.put("outputs", outputs);

        // Parameters
        List<Map<String, Object>> params = new ArrayList<>();
        for (int i = 0; i < chain.getNumParameters(); i++) {
            DataComponent param = chain.getParameterList().getComponent(i);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", param.getName());
            info.put("type", param.getClass().getSimpleName());
            params.add(info);
        }
        map.put("parameters", params);

        // Components
        List<Map<String, Object>> components = new ArrayList<>();
        for (int i = 0; i < chain.getNumComponents(); i++) {
            var prop = chain.getComponentList().getProperties().get(i);
            AbstractProcess process = prop.getValue();
            Map<String, Object> compInfo = new LinkedHashMap<>();
            compInfo.put("componentName", prop.getName());
            compInfo.put("processName", process.getName());
            if (process.getTypeOf() != null && process.getTypeOf().getHref() != null)
                compInfo.put("processURI", process.getTypeOf().getHref());

            // Show I/O from resolved executable if available (simplified SensorML won't have inlined I/O)
            ExecutableProcessImpl exec = chainName != null ? resolvedOutputs.get(chainName + "/" + prop.getName()) : null;
            if (exec != null) {
                compInfo.put("inputs", describeIOList(exec.getInputList()));
                compInfo.put("outputs", describeIOList(exec.getOutputList()));
                compInfo.put("parameters", describeIOList(exec.getParameterList()));
            } else {
                compInfo.put("inputs", describeIOList(process.getInputList()));
                compInfo.put("outputs", describeIOList(process.getOutputList()));
                compInfo.put("parameters", describeIOList(process.getParameterList()));
            }

            components.add(compInfo);
        }
        map.put("components", components);

        // Connections
        List<Map<String, Object>> connections = new ArrayList<>();
        for (int i = 0; i < chain.getNumConnections(); i++) {
            var conn = chain.getConnectionList().get(i);
            Map<String, Object> connInfo = new LinkedHashMap<>();
            connInfo.put("source", conn.getSource());
            connInfo.put("destination", conn.getDestination());
            connections.add(connInfo);
        }
        map.put("connections", connections);

        return map;
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

        ResultBuilder put(String key, Object value) {
            result.put(key, value);
            return this;
        }

        McpSchema.CallToolResult build() {
            String json = gson.toJson(result);
            boolean isError = !Boolean.TRUE.equals(result.get("success"));
            return McpSchema.CallToolResult.builder()
                    .content(List.of(new McpSchema.TextContent(json)))
                    .structuredContent(result)
                    .isError(isError)
                    .build();
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
}
