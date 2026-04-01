/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2024 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package com.georobotix.impl.service.mcp.resources;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.database.IFederatedDatabase;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;


/**
 * MCP Resource template that provides datastreams for a given system.
 *
 * URI Template: datastore://systems/{systemUID}/datastreams
 */
public class   DataStreamListResource extends AbstractMcpResource {

    public static final String URI_TEMPLATE = "datastore://systems/{systemUID}/datastreams";
    public static final String NAME = "System DataStreams";
    public static final String DESCRIPTION = "List of data streams produced by a specific system, including their record structures, time ranges, and output names.";

    private final IFederatedDatabase fedDb;

    public DataStreamListResource(IFederatedDatabase fedDb) {
        super();
        this.fedDb = fedDb;
    }

    @Override
    public String getUri() { return URI_TEMPLATE; }

    @Override
    public String getName() { return NAME; }

    @Override
    public String getDescription() { return DESCRIPTION; }

    @Override
    public String getMimeType() { return "application/json"; }

    public McpSchema.ResourceTemplate toResourceTemplateSchema() {
        return new McpSchema.ResourceTemplate(
                URI_TEMPLATE,
                NAME,
                getDescription(),
                getMimeType(),
                null
        );
    }

    @Override
    public Mono<McpSchema.ReadResourceResult> read(McpAsyncServerExchange exchange) {
        return Mono.error(new UnsupportedOperationException(
                "Use readWithSystemUID() for template resources"
        ));
    }

    public Mono<McpSchema.ReadResourceResult> readWithSystemUID(String systemUID) {
        return Mono.fromCallable(() -> {
            var filter = new DataStreamFilter.Builder()
                    .withSystems().withUniqueIDs(systemUID).done()
                    .withCurrentVersion()
                    .withLimit(1000)
                    .build();

            List<Map<String, Object>> datastreams;
            try (var stream = fedDb.getDataStreamStore().select(filter)) {
                datastreams = stream
                        .map(this::toSummary)
                        .collect(Collectors.toList());
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("systemUID", systemUID);
            response.put("count", datastreams.size());
            response.put("datastreams", datastreams);

            String json = gson.toJson(response);
            String resolvedUri = URI_TEMPLATE.replace("{systemUID}", systemUID);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(resolvedUri, getMimeType(), json))
            );
        });
    }

    private Map<String, Object> toSummary(IDataStreamInfo ds) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("outputName", ds.getOutputName());
        if (ds.getSystemID() != null)
            map.put("systemUID", ds.getSystemID().getUniqueID());
        if (ds.getValidTime() != null) {
            map.put("validTimeBegin", ds.getValidTime().begin().toString());
            map.put("validTimeEnd", ds.getValidTime().endsNow() ? "now" : ds.getValidTime().end().toString());
        }
        if (ds.getPhenomenonTimeRange() != null) {
            map.put("phenomenonTimeBegin", ds.getPhenomenonTimeRange().begin().toString());
            map.put("phenomenonTimeEnd", ds.getPhenomenonTimeRange().endsNow() ? "now" : ds.getPhenomenonTimeRange().end().toString());
        }

        DataComponent recordStruct = ds.getRecordStructure();
        if (recordStruct != null) {
            map.put("recordStructure", serializeDataComponent(recordStruct));
        }
        return map;
    }

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
}
