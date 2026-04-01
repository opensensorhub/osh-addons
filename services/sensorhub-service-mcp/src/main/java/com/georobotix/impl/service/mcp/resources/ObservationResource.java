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
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IFederatedDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;


/**
 * MCP Resource template providing latest observations for a system.
 *
 * URI Template: datastore://systems/{systemUID}/observations
 */
public class ObservationResource extends AbstractMcpResource {

    public static final String URI_TEMPLATE = "datastore://systems/{systemUID}/observations";
    public static final String NAME = "System Observations";
    public static final String DESCRIPTION = "Latest observations from a specific system's datastreams, with decoded result values based on the datastream record structure.";

    private final IFederatedDatabase fedDb;

    public ObservationResource(IFederatedDatabase fedDb) {
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
            // Get latest observations for all datastreams of this system
            var obsFilter = new ObsFilter.Builder()
                    .withSystems().withUniqueIDs(systemUID).done()
                    .withLatestResult()
                    .withLimit(100)
                    .build();

            List<Map<String, Object>> observations;
            try (var stream = fedDb.getObservationStore().select(obsFilter)) {
                observations = stream
                        .map(this::toSummary)
                        .collect(Collectors.toList());
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("systemUID", systemUID);
            response.put("count", observations.size());
            response.put("observations", observations);

            String json = gson.toJson(response);
            String resolvedUri = URI_TEMPLATE.replace("{systemUID}", systemUID);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(resolvedUri, getMimeType(), json))
            );
        });
    }

    private Map<String, Object> toSummary(IObsData obs) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("datastreamId", BigId.toString32(obs.getDataStreamID()));
        map.put("phenomenonTime", obs.getPhenomenonTime().toString());
        if (obs.getResultTime() != null && !obs.getResultTime().equals(obs.getPhenomenonTime()))
            map.put("resultTime", obs.getResultTime().toString());
        if (obs.hasFoi())
            map.put("foiId", BigId.toString32(obs.getFoiID()));

        // Decode result using datastream record structure
        DataBlock result = obs.getResult();
        if (result != null) {
            try {
                var dsFilter = new DataStreamFilter.Builder()
                        .withInternalIDs(obs.getDataStreamID())
                        .build();
                try (var stream = fedDb.getDataStreamStore().select(dsFilter)) {
                    IDataStreamInfo dsInfo = stream.findFirst().orElse(null);
                    if (dsInfo != null && dsInfo.getRecordStructure() != null) {
                        map.put("outputName", dsInfo.getOutputName());
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

    private Map<String, Object> deserializeDataBlock(DataComponent structure, DataBlock block) {
        Map<String, Object> result = new LinkedHashMap<>();
        structure.setData(block);
        extractValues(structure, result);
        return result;
    }

    private void extractValues(DataComponent comp, Map<String, Object> result) {
        if (comp.getComponentCount() == 0) {
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
}
