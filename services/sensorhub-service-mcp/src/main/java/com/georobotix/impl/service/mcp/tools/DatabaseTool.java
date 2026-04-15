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

import com.georobotix.impl.service.mcp.JavaxMcpStreamableTransport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.api.datastore.command.CommandStreamKey;
import org.sensorhub.api.datastore.deployment.DeploymentFilter;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.api.system.IDeploymentWithDesc;
import org.sensorhub.api.system.ISystemWithDesc;
import org.sensorhub.impl.common.IdEncodersBase32;
import org.sensorhub.impl.service.consys.deployment.DeploymentBindingGeoJson;
import org.sensorhub.impl.service.consys.feature.FoiBindingGeoJson;
import org.sensorhub.impl.service.consys.obs.DataStreamBindingJson;
import org.sensorhub.impl.service.consys.obs.ObsBindingOmJson;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.system.SystemBindingGeoJson;
import org.sensorhub.impl.service.consys.task.CommandBindingJson;
import org.sensorhub.impl.service.consys.task.CommandStreamBindingJson;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.vast.ogc.gml.IFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    private final IEventBus eventBus;
    private final String writeDatabaseId;
    private final IdEncoders idEncoders;
    private final Gson gson;

    public DatabaseTool(IDatabaseRegistry databaseRegistry, IEventBus eventBus, String writeDatabaseId) {
        this.databaseRegistry = databaseRegistry;
        this.eventBus = eventBus;
        this.writeDatabaseId = writeDatabaseId;
        this.idEncoders = new IdEncodersBase32();
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
            List<String> results = stream
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
            List<String> results = stream
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
            List<String> results = stream
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
            List<String> results = stream
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
            List<String> results = stream
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

        try (var stream = db.getCommandStreamStore().selectEntries(builder.build())) {
            List<String> results = stream
                    .skip(offset)
                    .limit(limit)
                    .map(e -> serializeCommandStream(e.getKey().getInternalID(), e.getValue()))
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
            List<String> results = stream
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

            String resource = switch (type) {
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
                        yield stream.findFirst().map(cs -> serializeCommandStream(id, cs)).orElse(null);
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

            // Build observation
            String foiIdStr = (String) args.get("foiId");
            var obsBuilder = new ObsData.Builder()
                    .withDataStream(dsId)
                    .withPhenomenonTime(phenomenonTime)
                    .withResult(dataBlock);

            if (foiIdStr != null && !foiIdStr.isBlank())
                obsBuilder.withFoi(BigId.fromString32(foiIdStr));

            // Submit through transaction handler so ObsEvent is published to subscribers
            var txHandler = new SystemDatabaseTransactionHandler(eventBus,
                    databaseRegistry.getFederatedDatabase());
            var dsHandler = txHandler.getDataStreamHandler(dsId);
            if (dsHandler == null)
                return errorResult("Datastream handler not found for: " + datastreamId);

            BigId obsId = dsHandler.addObs(obsBuilder.build());

            String resultJson = "{\"observationId\":\"" + BigId.toString32(obsId)
                    + "\",\"datastreamId\":\"" + escapeJson(datastreamId)
                    + "\",\"phenomenonTime\":\"" + escapeJson(phenomenonTime.toString()) + "\"}";

            return resultBuilder()
                    .success(true)
                    .message("Observation written successfully")
                    .resource(resultJson)
                    .build();
        } catch (Exception e) {
            return errorResult("Failed to write observation: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private McpSchema.CallToolResult writeCommand(Map<String, Object> args) {
        String commandstreamId = (String) args.get("commandstreamId");
        if (commandstreamId == null || commandstreamId.isBlank())
            return errorResult("commandstreamId is required for write_command");

        Map<String, Object> commandData = (Map<String, Object>) args.get("commandData");
        if (commandData == null || commandData.isEmpty())
            return errorResult("commandData is required for write_command");

        try {
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
                    .withSender(JavaxMcpStreamableTransport.getCurrentUser())
                    .withIssueTime(Instant.now())
                    .withParams(dataBlock);

            String foiIdStr = (String) args.get("foiId");
            if (foiIdStr != null && !foiIdStr.isBlank())
                cmdBuilder.withFoi(BigId.fromString32(foiIdStr));

            // Submit command through the event bus so it reaches the live driver
            var txHandler = new SystemDatabaseTransactionHandler(eventBus,
                    databaseRegistry.getFederatedDatabase());
            var csHandler = txHandler.getCommandStreamHandler(csId);
            if (csHandler == null)
                return errorResult("Command stream handler not found for: " + commandstreamId);

            long correlationID = System.nanoTime();
            var status = csHandler.submitCommand(correlationID, cmdBuilder.build(),
                    5, TimeUnit.SECONDS).get();

            String resultJson = "{\"commandId\":\"" + BigId.toString32(status.getCommandID())
                    + "\",\"commandstreamId\":\"" + escapeJson(commandstreamId)
                    + "\",\"statusCode\":\"" + escapeJson(status.getStatusCode().toString())
                    + "\",\"issueTime\":\"" + escapeJson(Instant.now().toString()) + "\"}";

            return resultBuilder()
                    .success(true)
                    .message("Command submitted successfully (status: " + status.getStatusCode() + ")")
                    .resource(resultJson)
                    .build();
        } catch (Exception e) {
            return errorResult("Failed to write command: " + e.getMessage());
        }
    }

    // ==================== Serialization ====================

    private String serializeSystem(ISystemWithDesc system) {
        try {
            var buffer = new ByteArrayOutputStream();
            var ctx = new RequestContext(buffer);
            var db = databaseRegistry.getFederatedDatabase();
            var binding = new SystemBindingGeoJson(ctx, idEncoders, db, false);
            binding.serialize((FeatureKey) null, system, false);
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to serialize system: {}", e.getMessage());
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String serializeDataStream(IDataStreamInfo ds) {
        try {
            var buffer = new ByteArrayOutputStream();
            var ctx = new RequestContext(buffer);
            var db = databaseRegistry.getFederatedDatabase();
            var binding = new DataStreamBindingJson(ctx, idEncoders, db, false, Collections.emptyMap());
            binding.serialize((DataStreamKey) null, ds, false);
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to serialize datastream: {}", e.getMessage());
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String serializeObservation(IObsData obs, IObsSystemDatabase db) {
        try {
            var buffer = new ByteArrayOutputStream();
            var ctx = new RequestContext(buffer);
            var binding = new ObsBindingOmJson(ctx, idEncoders, false, db.getObservationStore());
            binding.serialize((BigId) null, obs, false);
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to serialize observation: {}", e.getMessage());
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String serializeFeature(IFeature feature) {
        try {
            var buffer = new ByteArrayOutputStream();
            var ctx = new RequestContext(buffer);
            var db = databaseRegistry.getFederatedDatabase();
            var binding = new FoiBindingGeoJson(ctx, idEncoders, db, false);
            binding.serialize((FeatureKey) null, feature, false);
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to serialize feature: {}", e.getMessage());
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String serializeCommand(ICommandData cmd, IObsSystemDatabase db) {
        try {
            var buffer = new ByteArrayOutputStream();
            var ctx = new RequestContext(buffer);
            var binding = new CommandBindingJson(ctx, idEncoders, false, db.getCommandStore());
            binding.serialize((BigId) null, cmd, false);
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to serialize command: {}", e.getMessage());
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String serializeCommandStream(BigId id, ICommandStreamInfo cs) {
        try {
            var buffer = new ByteArrayOutputStream();
            var ctx = new RequestContext(buffer);
            var db = databaseRegistry.getFederatedDatabase();
            var binding = new CommandStreamBindingJson(ctx, idEncoders, db, false);
            binding.serialize(new CommandStreamKey(id), cs, false);
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to serialize command stream: {}", e.getMessage());
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String serializeDeployment(IDeploymentWithDesc dep) {
        try {
            var buffer = new ByteArrayOutputStream();
            var ctx = new RequestContext(buffer);
            var db = databaseRegistry.getFederatedDatabase();
            var binding = new DeploymentBindingGeoJson(ctx, idEncoders, db, false);
            binding.serialize((FeatureKey) null, dep, false);
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to serialize deployment: {}", e.getMessage());
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    // ==================== Data Component Helpers ====================

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
        private boolean success;
        private String message;
        private List<String> jsonResults;
        private String jsonResource;

        ResultBuilder success(boolean success) { this.success = success; return this; }
        ResultBuilder message(String message) { this.message = message; return this; }
        ResultBuilder results(List<String> items) { this.jsonResults = items; return this; }
        ResultBuilder resource(String json) { this.jsonResource = json; return this; }

        McpSchema.CallToolResult build() {
            var sb = new StringBuilder();
            sb.append("{\"success\":").append(success);
            sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");
            if (jsonResults != null) {
                sb.append(",\"count\":").append(jsonResults.size());
                sb.append(",\"results\":[");
                sb.append(String.join(",", jsonResults));
                sb.append("]");
            }
            if (jsonResource != null) {
                sb.append(",\"resource\":").append(jsonResource);
            }
            sb.append("}");
            String json = sb.toString();
            boolean isError = !success;
            return McpSchema.CallToolResult.builder()
                    .content(List.of(new McpSchema.TextContent(json)))
                    .isError(isError)
                    .build();
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
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
