/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2024 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package com.georobotix.impl.service.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.api.datastore.deployment.DeploymentFilter;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.system.ISystemWithDesc;
import org.vast.ogc.gml.IFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


/**
 * MCP Tool for querying and writing to the OSH datastore.
 * Provides access to systems, datastreams, observations, features of interest,
 * commands, command streams, and deployments via the federated database (reads)
 * and a configured write database (writes).
 */
public class DatabaseTool extends AbstractMcpTool {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTool.class);

    public static final String NAME = "database";
    public static final String TITLE = "Database Tool";
    public static final String DESC = """
        Tool to query and write data in the OpenSensorHub datastore.

        Supported resource types:
        - systems: Query registered sensor systems and their descriptions
        - datastreams: Query data stream metadata (what data a system produces)
        - observations: Query and write observation data
        - fois: Query features of interest
        - commands: Query and write commands
        - commandstreams: Query command stream metadata
        - deployments: Query deployment information

        Query actions use the federated database (read-only, aggregates all databases).
        Write actions use the configured write database.

        Common filter parameters:
        - systemUID: Filter by system unique ID (URI)
        - keyword: Full-text search across resources
        - timeRange: Filter by time range (ISO-8601, e.g. "2024-01-01T00:00:00Z/2024-12-31T23:59:59Z")
        - limit: Max number of results (default 100)

        Use 'query' action to search, 'get' for a specific resource by ID,
        'write_observation' to insert observations, 'write_command' to issue commands.
        """;

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 10000;

    private final IDatabaseRegistry databaseRegistry;
    private final String writeDatabaseId;
    private final Gson gson;

    public DatabaseTool(IDatabaseRegistry databaseRegistry, String writeDatabaseId) {
        this.databaseRegistry = databaseRegistry;
        this.writeDatabaseId = writeDatabaseId;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();
    }

    public enum Action {
        QUERY,              // Query resources with filters
        GET,                // Get a specific resource by internal ID
        WRITE_OBSERVATION,  // Write an observation
        WRITE_COMMAND       // Issue a command
    }

    public enum ResourceType {
        SYSTEMS,
        DATASTREAMS,
        OBSERVATIONS,
        FOIS,
        COMMANDS,
        COMMANDSTREAMS,
        DEPLOYMENTS
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
                "enum", List.of("query", "get", "write_observation", "write_command"),
                "description", "The action to perform"
        ));
        properties.put("resourceType", Map.of(
                "type", "string",
                "enum", List.of("systems", "datastreams", "observations", "fois", "commands", "commandstreams", "deployments"),
                "description", "The type of resource to query"
        ));
        properties.put("resourceId", Map.of("type", "string", "description", "Internal resource ID (for 'get' action)"));
        properties.put("systemUID", Map.of("type", "string", "description", "Filter by system unique ID (URI)"));
        properties.put("keyword", Map.of("type", "string", "description", "Full-text keyword search"));
        properties.put("outputName", Map.of("type", "string", "description", "Filter datastreams/observations by output name"));
        properties.put("observedProperty", Map.of("type", "string", "description", "Filter by observed property URI"));
        properties.put("timeRange", Map.of("type", "string", "description", "Time range filter in ISO-8601 interval format (e.g. '2024-01-01T00:00:00Z/2024-12-31T23:59:59Z'). Use 'latest' for latest result only."));
        properties.put("limit", Map.of("type", "integer", "description", "Maximum number of results (default 100, max 10000)"));
        properties.put("offset", Map.of("type", "integer", "description", "Number of results to skip for pagination"));
        properties.put("datastreamId", Map.of("type", "string", "description", "Datastream ID for writing observations or filtering"));
        properties.put("commandstreamId", Map.of("type", "string", "description", "Command stream ID for writing commands"));
        properties.put("foiId", Map.of("type", "string", "description", "Feature of interest ID"));
        properties.put("phenomenonTime", Map.of("type", "string", "description", "Phenomenon time for observation write (ISO-8601)"));
        properties.put("resultData", Map.of("type", "object", "description", "Result data as key-value pairs matching the datastream record structure."));
        properties.put("commandData", Map.of("type", "object", "description", "Command parameters as key-value pairs matching the command stream record structure."));

        return new McpSchema.JsonSchema(
                "object",
                properties,
                List.of("action", "resourceType"),
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
                        "count", Map.of("type", "integer", "description", "Number of results returned"),
                        "results", Map.of("type", "array", "description", "Query results"),
                        "resource", Map.of("type", "object", "description", "Single resource detail")
                ),
                "required", List.of("success", "message")
        );
    }

    @Override
    public McpSchema.ToolAnnotations getToolAnnotations() {
        return new McpSchema.ToolAnnotations(
                null,
                false,  // readOnlyHint - false because we can write
                true,   // destructiveHint
                false,  // idempotentHint
                false,  // openWorldHint
                false   // localHint
        );
    }

    @Override
    public Map<String, Object> getMetaData() {
        return Map.of(
                "category", "datastore",
                "version", "1.0.0"
        );
    }

    @Override
    public BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> getCallHandler() {
        return (exchange, callRequest) -> Mono.fromCallable(() -> {
            Map<String, Object> request = callRequest.arguments();
            String actionStr = (String) request.get("action");
            String resourceTypeStr = (String) request.get("resourceType");
            Action action = Action.valueOf(actionStr.toUpperCase());
            ResourceType resourceType = ResourceType.valueOf(resourceTypeStr.toUpperCase());

            return switch (action) {
                case QUERY -> queryResources(resourceType, request);
                case GET -> getResource(resourceType, request);
                case WRITE_OBSERVATION -> writeObservation(request);
                case WRITE_COMMAND -> writeCommand(request);
            };
        }).onErrorResume(e -> Mono.just(errorResult(e)));
    }

    // ==================== Query ====================

    private McpSchema.CallToolResult queryResources(ResourceType type, Map<String, Object> args) {
        var fedDb = databaseRegistry.getFederatedDatabase();
        int limit = getLimit(args);
        int offset = getOffset(args);

        return switch (type) {
            case SYSTEMS -> querySystems(fedDb, args, limit, offset);
            case DATASTREAMS -> queryDataStreams(fedDb, args, limit, offset);
            case OBSERVATIONS -> queryObservations(fedDb, args, limit, offset);
            case FOIS -> queryFois(fedDb, args, limit, offset);
            case COMMANDS -> queryCommands(fedDb, args, limit, offset);
            case COMMANDSTREAMS -> queryCommandStreams(fedDb, args, limit, offset);
            case DEPLOYMENTS -> queryDeployments(fedDb, args, limit, offset);
        };
    }

    private McpSchema.CallToolResult querySystems(IObsSystemDatabase db, Map<String, Object> args, int limit, int offset) {
        var builder = new SystemFilter.Builder();

        String systemUID = (String) args.get("systemUID");
        if (systemUID != null && !systemUID.isBlank())
            builder.withUniqueIDs(systemUID);

        String keyword = (String) args.get("keyword");
        if (keyword != null && !keyword.isBlank())
            builder.withKeywords(keyword);

        builder.withLimit(limit + offset);

        try (var stream = db.getSystemDescStore().select(builder.build())) {
            List<Map<String, Object>> results = stream
                    .skip(offset)
                    .limit(limit)
                    .map(this::serializeSystem)
                    .collect(Collectors.toList());

            return resultBuilder()
                    .success(true)
                    .message(String.format("Found %d system(s)", results.size()))
                    .results(results)
                    .build();
        }
    }

    private McpSchema.CallToolResult queryDataStreams(IObsSystemDatabase db, Map<String, Object> args, int limit, int offset) {
        var builder = new DataStreamFilter.Builder();

        String systemUID = (String) args.get("systemUID");
        if (systemUID != null && !systemUID.isBlank())
            builder.withSystems().withUniqueIDs(systemUID).done();

        String outputName = (String) args.get("outputName");
        if (outputName != null && !outputName.isBlank())
            builder.withOutputNames(outputName);

        String observedProperty = (String) args.get("observedProperty");
        if (observedProperty != null && !observedProperty.isBlank())
            builder.withObservedProperties(observedProperty);

        applyTimeFilter(args, builder);
        builder.withLimit(limit + offset);

        try (var stream = db.getDataStreamStore().select(builder.build())) {
            List<Map<String, Object>> results = stream
                    .skip(offset)
                    .limit(limit)
                    .map(this::serializeDataStream)
                    .collect(Collectors.toList());

            return resultBuilder()
                    .success(true)
                    .message(String.format("Found %d datastream(s)", results.size()))
                    .results(results)
                    .build();
        }
    }

    private McpSchema.CallToolResult queryObservations(IObsSystemDatabase db, Map<String, Object> args, int limit, int offset) {
        var builder = new ObsFilter.Builder();

        String systemUID = (String) args.get("systemUID");
        if (systemUID != null && !systemUID.isBlank())
            builder.withSystems().withUniqueIDs(systemUID).done();

        String datastreamId = (String) args.get("datastreamId");
        if (datastreamId != null && !datastreamId.isBlank()) {
            try {
                BigId dsId = BigId.fromString32(datastreamId);
                builder.withDataStreams(dsId);
            } catch (Exception e) {
                return errorResult("Invalid datastreamId format: " + datastreamId);
            }
        }

        String foiId = (String) args.get("foiId");
        if (foiId != null && !foiId.isBlank()) {
            try {
                BigId fId = BigId.fromString32(foiId);
                builder.withFois(fId);
            } catch (Exception e) {
                return errorResult("Invalid foiId format: " + foiId);
            }
        }

        String timeRange = (String) args.get("timeRange");
        if ("latest".equalsIgnoreCase(timeRange)) {
            builder.withLatestResult();
        } else if (timeRange != null && !timeRange.isBlank()) {
            Instant[] range = parseTimeRange(timeRange);
            if (range != null)
                builder.withPhenomenonTimeDuring(range[0], range[1]);
        }

        builder.withLimit(limit + offset);

        try (var stream = db.getObservationStore().select(builder.build())) {
            List<Map<String, Object>> results = stream
                    .skip(offset)
                    .limit(limit)
                    .map(obs -> serializeObservation(obs, db))
                    .collect(Collectors.toList());

            return resultBuilder()
                    .success(true)
                    .message(String.format("Found %d observation(s)", results.size()))
                    .results(results)
                    .build();
        }
    }

    private McpSchema.CallToolResult queryFois(IObsSystemDatabase db, Map<String, Object> args, int limit, int offset) {
        var builder = new FoiFilter.Builder();

        String systemUID = (String) args.get("systemUID");
        if (systemUID != null && !systemUID.isBlank())
            builder.withParents().withUniqueIDs(systemUID).done();

        String keyword = (String) args.get("keyword");
        if (keyword != null && !keyword.isBlank())
            builder.withKeywords(keyword);

        builder.withLimit(limit + offset);

        try (var stream = db.getFoiStore().select(builder.build())) {
            List<Map<String, Object>> results = stream
                    .skip(offset)
                    .limit(limit)
                    .map(this::serializeFeature)
                    .collect(Collectors.toList());

            return resultBuilder()
                    .success(true)
                    .message(String.format("Found %d feature(s) of interest", results.size()))
                    .results(results)
                    .build();
        }
    }

    private McpSchema.CallToolResult queryCommands(IObsSystemDatabase db, Map<String, Object> args, int limit, int offset) {
        var builder = new CommandFilter.Builder();

        String systemUID = (String) args.get("systemUID");
        if (systemUID != null && !systemUID.isBlank())
            builder.withSystems().withUniqueIDs(systemUID).done();

        String commandstreamId = (String) args.get("commandstreamId");
        if (commandstreamId != null && !commandstreamId.isBlank()) {
            try {
                BigId csId = BigId.fromString32(commandstreamId);
                builder.withCommandStreams().withInternalIDs(csId).done();
            } catch (Exception e) {
                return errorResult("Invalid commandstreamId format: " + commandstreamId);
            }
        }

        String timeRange = (String) args.get("timeRange");
        if (timeRange != null && !timeRange.isBlank()) {
            Instant[] range = parseTimeRange(timeRange);
            if (range != null)
                builder.withIssueTimeDuring(range[0], range[1]);
        }

        builder.withLimit(limit + offset);

        try (var stream = db.getCommandStore().select(builder.build())) {
            List<Map<String, Object>> results = stream
                    .skip(offset)
                    .limit(limit)
                    .map(cmd -> serializeCommand(cmd, db))
                    .collect(Collectors.toList());

            return resultBuilder()
                    .success(true)
                    .message(String.format("Found %d command(s)", results.size()))
                    .results(results)
                    .build();
        }
    }

    private McpSchema.CallToolResult queryCommandStreams(IObsSystemDatabase db, Map<String, Object> args, int limit, int offset) {
        var builder = new CommandStreamFilter.Builder();

        String systemUID = (String) args.get("systemUID");
        if (systemUID != null && !systemUID.isBlank())
            builder.withSystems().withUniqueIDs(systemUID).done();

        builder.withLimit(limit + offset);

        try (var stream = db.getCommandStreamStore().select(builder.build())) {
            List<Map<String, Object>> results = stream
                    .skip(offset)
                    .limit(limit)
                    .map(this::serializeCommandStream)
                    .collect(Collectors.toList());

            return resultBuilder()
                    .success(true)
                    .message(String.format("Found %d command stream(s)", results.size()))
                    .results(results)
                    .build();
        }
    }

    private McpSchema.CallToolResult queryDeployments(IObsSystemDatabase db, Map<String, Object> args, int limit, int offset) {
        var builder = new DeploymentFilter.Builder();

        String keyword = (String) args.get("keyword");
        if (keyword != null && !keyword.isBlank())
            builder.withKeywords(keyword);

        builder.withLimit(limit + offset);

        try (var stream = db.getDeploymentStore().select(builder.build())) {
            List<Map<String, Object>> results = stream
                    .skip(offset)
                    .limit(limit)
                    .map(this::serializeDeployment)
                    .collect(Collectors.toList());

            return resultBuilder()
                    .success(true)
                    .message(String.format("Found %d deployment(s)", results.size()))
                    .results(results)
                    .build();
        }
    }

    // ==================== Get by ID ====================

    private McpSchema.CallToolResult getResource(ResourceType type, Map<String, Object> args) {
        String resourceId = (String) args.get("resourceId");
        if (resourceId == null || resourceId.isBlank())
            return errorResult("resourceId is required for 'get' action");

        var fedDb = databaseRegistry.getFederatedDatabase();

        try {
            BigId id = BigId.fromString32(resourceId);

            Map<String, Object> resource = switch (type) {
                case SYSTEMS -> {
                    var filter = new SystemFilter.Builder().withInternalIDs(id).build();
                    try (var stream = fedDb.getSystemDescStore().select(filter)) {
                        yield stream.findFirst().map(this::serializeSystem).orElse(null);
                    }
                }
                case DATASTREAMS -> {
                    var filter = new DataStreamFilter.Builder().withInternalIDs(id).build();
                    try (var stream = fedDb.getDataStreamStore().select(filter)) {
                        yield stream.findFirst().map(this::serializeDataStream).orElse(null);
                    }
                }
                case OBSERVATIONS -> {
                    var filter = new ObsFilter.Builder().withInternalIDs(id).build();
                    try (var stream = fedDb.getObservationStore().select(filter)) {
                        yield stream.findFirst().map(obs -> serializeObservation(obs, fedDb)).orElse(null);
                    }
                }
                case FOIS -> {
                    var filter = new FoiFilter.Builder().withInternalIDs(id).build();
                    try (var stream = fedDb.getFoiStore().select(filter)) {
                        yield stream.findFirst().map(this::serializeFeature).orElse(null);
                    }
                }
                case COMMANDS -> {
                    var filter = new CommandFilter.Builder().withInternalIDs(id).build();
                    try (var stream = fedDb.getCommandStore().select(filter)) {
                        yield stream.findFirst().map(cmd -> serializeCommand(cmd, fedDb)).orElse(null);
                    }
                }
                case COMMANDSTREAMS -> {
                    var filter = new CommandStreamFilter.Builder().withInternalIDs(id).build();
                    try (var stream = fedDb.getCommandStreamStore().select(filter)) {
                        yield stream.findFirst().map(this::serializeCommandStream).orElse(null);
                    }
                }
                case DEPLOYMENTS -> {
                    var filter = new DeploymentFilter.Builder().withInternalIDs(id).build();
                    try (var stream = fedDb.getDeploymentStore().select(filter)) {
                        yield stream.findFirst().map(this::serializeDeployment).orElse(null);
                    }
                }
            };

            if (resource == null)
                return errorResult("Resource not found: " + resourceId);

            return resultBuilder()
                    .success(true)
                    .message("Resource found")
                    .resource(resource)
                    .build();
        } catch (Exception e) {
            return errorResult("Failed to get resource: " + e.getMessage());
        }
    }

    // ==================== Write Operations ====================

    @SuppressWarnings("unchecked")
    private McpSchema.CallToolResult writeObservation(Map<String, Object> args) {
        if (writeDatabaseId == null || writeDatabaseId.isBlank())
            return errorResult("Write operations are disabled: no write database configured");

        String datastreamId = (String) args.get("datastreamId");
        if (datastreamId == null || datastreamId.isBlank())
            return errorResult("datastreamId is required for write_observation");

        String phenomenonTimeStr = (String) args.get("phenomenonTime");
        if (phenomenonTimeStr == null || phenomenonTimeStr.isBlank())
            return errorResult("phenomenonTime is required for write_observation");

        Map<String, Object> resultData = (Map<String, Object>) args.get("resultData");
        if (resultData == null || resultData.isEmpty())
            return errorResult("resultData is required for write_observation");

        try {
            IObsSystemDatabase writeDb = databaseRegistry.getObsDatabaseByModuleID(writeDatabaseId);
            BigId dsId = BigId.fromString32(datastreamId);
            Instant phenomenonTime = Instant.parse(phenomenonTimeStr);

            // Look up the datastream to get its record structure
            var dsFilter = new DataStreamFilter.Builder().withInternalIDs(dsId).build();
            IDataStreamInfo dsInfo;
            try (var stream = databaseRegistry.getFederatedDatabase().getDataStreamStore().select(dsFilter)) {
                dsInfo = stream.findFirst().orElse(null);
            }
            if (dsInfo == null)
                return errorResult("Datastream not found: " + datastreamId);

            DataBlock dataBlock = buildDataBlock(dsInfo.getRecordStructure(), resultData);

            // Build and store observation
            String foiIdStr = (String) args.get("foiId");
            var obsBuilder = new ObsData.Builder()
                    .withDataStream(dsId)
                    .withPhenomenonTime(phenomenonTime)
                    .withResult(dataBlock);

            if (foiIdStr != null && !foiIdStr.isBlank())
                obsBuilder.withFoi(BigId.fromString32(foiIdStr));

            BigId obsId = writeDb.getObservationStore().add(obsBuilder.build());
            writeDb.commit();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("observationId", BigId.toString32(obsId));
            result.put("datastreamId", datastreamId);
            result.put("phenomenonTime", phenomenonTime.toString());

            return resultBuilder()
                    .success(true)
                    .message("Observation written successfully")
                    .resource(result)
                    .build();
        } catch (Exception e) {
            return errorResult("Failed to write observation: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private McpSchema.CallToolResult writeCommand(Map<String, Object> args) {
        if (writeDatabaseId == null || writeDatabaseId.isBlank())
            return errorResult("Write operations are disabled: no write database configured");

        String commandstreamId = (String) args.get("commandstreamId");
        if (commandstreamId == null || commandstreamId.isBlank())
            return errorResult("commandstreamId is required for write_command");

        Map<String, Object> commandData = (Map<String, Object>) args.get("commandData");
        if (commandData == null || commandData.isEmpty())
            return errorResult("commandData is required for write_command");

        try {
            IObsSystemDatabase writeDb = databaseRegistry.getObsDatabaseByModuleID(writeDatabaseId);
            BigId csId = BigId.fromString32(commandstreamId);

            // Look up the command stream to get its record structure
            var csFilter = new CommandStreamFilter.Builder().withInternalIDs(csId).build();
            ICommandStreamInfo csInfo;
            try (var stream = databaseRegistry.getFederatedDatabase().getCommandStreamStore().select(csFilter)) {
                csInfo = stream.findFirst().orElse(null);
            }
            if (csInfo == null)
                return errorResult("Command stream not found: " + commandstreamId);

            DataBlock dataBlock = buildDataBlock(csInfo.getRecordStructure(), commandData);

            var cmdBuilder = new CommandData.Builder()
                    .withCommandStream(csId)
                    .withIssueTime(Instant.now())
                    .withParams(dataBlock);

            String foiIdStr = (String) args.get("foiId");
            if (foiIdStr != null && !foiIdStr.isBlank())
                cmdBuilder.withFoi(BigId.fromString32(foiIdStr));

            BigId cmdId = writeDb.getCommandStore().add(cmdBuilder.build());
            writeDb.commit();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("commandId", BigId.toString32(cmdId));
            result.put("commandstreamId", commandstreamId);
            result.put("issueTime", Instant.now().toString());

            return resultBuilder()
                    .success(true)
                    .message("Command written successfully")
                    .resource(result)
                    .build();
        } catch (Exception e) {
            return errorResult("Failed to write command: " + e.getMessage());
        }
    }

    // ==================== Serialization ====================

    private Map<String, Object> serializeSystem(ISystemWithDesc system) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uniqueID", system.getUniqueIdentifier());
        map.put("name", system.getName());
        map.put("description", system.getDescription());
        map.put("type", system.getType());
        if (system.getValidTime() != null) {
            map.put("validTimeBegin", system.getValidTime().begin().toString());
            map.put("validTimeEnd", system.getValidTime().endsNow() ? "now" : system.getValidTime().end().toString());
        }

        // Include SensorML details if available
        AbstractProcess smlDesc = system.getFullDescription();
        if (smlDesc != null) {
            if (smlDesc.getNumInputs() > 0) {
                List<String> inputs = new ArrayList<>();
                for (int i = 0; i < smlDesc.getInputList().size(); i++)
                    inputs.add(smlDesc.getInputList().getComponent(i).getName());
                map.put("inputs", inputs);
            }
            if (smlDesc.getNumOutputs() > 0) {
                List<String> outputs = new ArrayList<>();
                for (int i = 0; i < smlDesc.getOutputList().size(); i++)
                    outputs.add(smlDesc.getOutputList().getComponent(i).getName());
                map.put("outputs", outputs);
            }
        }
        return map;
    }

    private Map<String, Object> serializeDataStream(IDataStreamInfo ds) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (ds.getSystemID() != null) {
            map.put("systemUID", ds.getSystemID().getUniqueID());
        }
        map.put("outputName", ds.getOutputName());
        if (ds.getValidTime() != null) {
            map.put("validTimeBegin", ds.getValidTime().begin().toString());
            map.put("validTimeEnd", ds.getValidTime().endsNow() ? "now" : ds.getValidTime().end().toString());
        }
        if (ds.getPhenomenonTimeRange() != null) {
            map.put("phenomenonTimeBegin", ds.getPhenomenonTimeRange().begin().toString());
            map.put("phenomenonTimeEnd", ds.getPhenomenonTimeRange().endsNow() ? "now" : ds.getPhenomenonTimeRange().end().toString());
        }

        // Serialize record structure
        DataComponent recordStruct = ds.getRecordStructure();
        if (recordStruct != null) {
            map.put("recordStructure", serializeDataComponent(recordStruct));
        }
        return map;
    }

    private Map<String, Object> serializeObservation(IObsData obs, IObsSystemDatabase db) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("datastreamId", BigId.toString32(obs.getDataStreamID()));
        map.put("phenomenonTime", obs.getPhenomenonTime().toString());
        if (obs.getResultTime() != null && !obs.getResultTime().equals(obs.getPhenomenonTime()))
            map.put("resultTime", obs.getResultTime().toString());
        if (obs.hasFoi())
            map.put("foiId", BigId.toString32(obs.getFoiID()));

        // Serialize result data using the datastream's record structure
        DataBlock result = obs.getResult();
        if (result != null) {
            try {
                var dsFilter = new DataStreamFilter.Builder().withInternalIDs(obs.getDataStreamID()).build();
                try (var stream = db.getDataStreamStore().select(dsFilter)) {
                    IDataStreamInfo dsInfo = stream.findFirst().orElse(null);
                    if (dsInfo != null && dsInfo.getRecordStructure() != null) {
                        map.put("result", deserializeDataBlock(dsInfo.getRecordStructure(), result));
                    } else {
                        map.put("result", dataBlockToList(result));
                    }
                }
            } catch (Exception e) {
                map.put("result", dataBlockToList(result));
            }
        }
        return map;
    }

    private Map<String, Object> serializeFeature(IFeature feature) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uniqueID", feature.getUniqueIdentifier());
        map.put("name", feature.getName());
        map.put("description", feature.getDescription());
        map.put("type", feature.getType());
        if (feature.getGeometry() != null)
            map.put("geometry", feature.getGeometry().toString());
        if (feature.getValidTime() != null) {
            map.put("validTimeBegin", feature.getValidTime().begin().toString());
            map.put("validTimeEnd", feature.getValidTime().endsNow() ? "now" : feature.getValidTime().end().toString());
        }
        return map;
    }

    private Map<String, Object> serializeCommand(ICommandData cmd, IObsSystemDatabase db) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("commandstreamId", BigId.toString32(cmd.getCommandStreamID()));
        map.put("issueTime", cmd.getIssueTime().toString());
        if (cmd.getSenderID() != null)
            map.put("senderId", cmd.getSenderID());
        if (cmd.hasFoi())
            map.put("foiId", BigId.toString32(cmd.getFoiID()));

        DataBlock params = cmd.getParams();
        if (params != null) {
            try {
                var csFilter = new CommandStreamFilter.Builder().withInternalIDs(cmd.getCommandStreamID()).build();
                try (var stream = db.getCommandStreamStore().select(csFilter)) {
                    ICommandStreamInfo csInfo = stream.findFirst().orElse(null);
                    if (csInfo != null && csInfo.getRecordStructure() != null) {
                        map.put("params", deserializeDataBlock(csInfo.getRecordStructure(), params));
                    } else {
                        map.put("params", dataBlockToList(params));
                    }
                }
            } catch (Exception e) {
                map.put("params", dataBlockToList(params));
            }
        }
        return map;
    }

    private Map<String, Object> serializeCommandStream(ICommandStreamInfo cs) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (cs.getSystemID() != null) {
            map.put("systemUID", cs.getSystemID().getUniqueID());
        }
        map.put("controlInputName", cs.getControlInputName());
        if (cs.getValidTime() != null) {
            map.put("validTimeBegin", cs.getValidTime().begin().toString());
            map.put("validTimeEnd", cs.getValidTime().endsNow() ? "now" : cs.getValidTime().end().toString());
        }

        DataComponent recordStruct = cs.getRecordStructure();
        if (recordStruct != null) {
            map.put("recordStructure", serializeDataComponent(recordStruct));
        }
        return map;
    }

    private Map<String, Object> serializeDeployment(org.sensorhub.api.system.IDeploymentWithDesc dep) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uniqueID", dep.getUniqueIdentifier());
        map.put("name", dep.getName());
        map.put("description", dep.getDescription());
        map.put("type", dep.getType());
        if (dep.getValidTime() != null) {
            map.put("validTimeBegin", dep.getValidTime().begin().toString());
            map.put("validTimeEnd", dep.getValidTime().endsNow() ? "now" : dep.getValidTime().end().toString());
        }
        return map;
    }

    // ==================== Data Component Helpers ====================

    private Map<String, Object> serializeDataComponent(DataComponent comp) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", comp.getName());
        if (comp.getLabel() != null) map.put("label", comp.getLabel());
        if (comp.getDescription() != null) map.put("description", comp.getDescription());
        map.put("type", comp.getClass().getSimpleName());

        if (comp.getComponentCount() > 0) {
            List<Map<String, Object>> fields = new ArrayList<>();
            for (int i = 0; i < comp.getComponentCount(); i++) {
                fields.add(serializeDataComponent(comp.getComponent(i)));
            }
            map.put("fields", fields);
        }
        return map;
    }

    private DataBlock buildDataBlock(DataComponent structure, Map<String, Object> values) {
        structure.assignNewDataBlock();
        DataBlock block = structure.getData();
        setDataBlockValues(structure, block, values);
        return block;
    }

    private void setDataBlockValues(DataComponent comp, DataBlock block, Map<String, Object> values) {
        if (comp.getComponentCount() == 0) {
            // Scalar component - set value directly on the component's data
            Object val = values.get(comp.getName());
            if (val != null && comp.hasData()) {
                setScalarOnComponent(comp, val);
            }
        } else {
            // Composite component - recurse
            for (int i = 0; i < comp.getComponentCount(); i++) {
                DataComponent child = comp.getComponent(i);
                Object childVal = values.get(child.getName());
                if (childVal instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> childMap = (Map<String, Object>) childVal;
                    setDataBlockValues(child, block, childMap);
                } else if (childVal != null && child.hasData()) {
                    setScalarOnComponent(child, childVal);
                }
            }
        }
    }

    private void setScalarOnComponent(DataComponent comp, Object value) {
        try {
            DataBlock data = comp.getData();
            if (value instanceof Number num) {
                data.setDoubleValue(num.doubleValue());
            } else if (value instanceof Boolean bool) {
                data.setBooleanValue(bool);
            } else {
                data.setStringValue(value.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to set value on component '{}': {}", comp.getName(), e.getMessage());
        }
    }

    private Map<String, Object> deserializeDataBlock(DataComponent structure, DataBlock block) {
        Map<String, Object> result = new LinkedHashMap<>();
        structure.setData(block);
        extractValues(structure, result);
        return result;
    }

    private void extractValues(DataComponent comp, Map<String, Object> result) {
        if (comp.getComponentCount() == 0) {
            // Scalar - extract value
            try {
                if (comp.hasData()) {
                    result.put(comp.getName(), getScalarValue(comp.getData()));
                }
            } catch (Exception e) {
                result.put(comp.getName(), null);
            }
        } else {
            for (int i = 0; i < comp.getComponentCount(); i++) {
                DataComponent child = comp.getComponent(i);
                if (child.getComponentCount() > 0) {
                    Map<String, Object> nested = new LinkedHashMap<>();
                    extractValues(child, nested);
                    result.put(child.getName(), nested);
                } else {
                    try {
                        if (child.hasData()) {
                            result.put(child.getName(), getScalarValue(child.getData()));
                        }
                    } catch (Exception e) {
                        result.put(child.getName(), null);
                    }
                }
            }
        }
    }

    private Object getScalarValue(DataBlock data) {
        try {
            switch (data.getDataType()) {
                case DOUBLE:
                case FLOAT:
                    return data.getDoubleValue();
                case INT:
                case SHORT:
                case BYTE:
                case LONG:
                    return data.getIntValue();
                case BOOLEAN:
                    return data.getBooleanValue();
                case UTF_STRING:
                case ASCII_STRING:
                    return data.getStringValue();
                default:
                    return data.getStringValue();
            }
        } catch (Exception e) {
            return data.getStringValue();
        }
    }

    private List<Object> dataBlockToList(DataBlock block) {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < block.getAtomCount(); i++) {
            try {
                values.add(block.getStringValue(i));
            } catch (Exception e) {
                values.add(null);
            }
        }
        return values;
    }

    // ==================== Filter Helpers ====================

    private void applyTimeFilter(Map<String, Object> args, DataStreamFilter.Builder builder) {
        String timeRange = (String) args.get("timeRange");
        if (timeRange != null && !timeRange.isBlank()) {
            if ("latest".equalsIgnoreCase(timeRange)) {
                builder.withCurrentVersion();
            } else {
                Instant[] range = parseTimeRange(timeRange);
                if (range != null)
                    builder.withValidTimeDuring(range[0], range[1]);
            }
        }
    }

    private Instant[] parseTimeRange(String timeRange) {
        try {
            String[] parts = timeRange.split("/");
            if (parts.length == 2) {
                Instant begin = "..".equals(parts[0]) ? Instant.MIN : Instant.parse(parts[0]);
                Instant end = "..".equals(parts[1]) ? Instant.MAX : Instant.parse(parts[1]);
                return new Instant[]{begin, end};
            }
        } catch (Exception e) {
            log.warn("Failed to parse time range '{}': {}", timeRange, e.getMessage());
        }
        return null;
    }

    private int getLimit(Map<String, Object> args) {
        Object limitObj = args.get("limit");
        if (limitObj instanceof Number num) {
            return Math.min(Math.max(num.intValue(), 1), MAX_LIMIT);
        }
        return DEFAULT_LIMIT;
    }

    private int getOffset(Map<String, Object> args) {
        Object offsetObj = args.get("offset");
        if (offsetObj instanceof Number num) {
            return Math.max(num.intValue(), 0);
        }
        return 0;
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

        ResultBuilder results(List<Map<String, Object>> items) {
            result.put("count", items.size());
            result.put("results", items);
            return this;
        }

        ResultBuilder resource(Map<String, Object> resource) {
            result.put("resource", resource);
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
