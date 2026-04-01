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
import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.database.IFederatedDatabase;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.system.ISystemWithDesc;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;


/**
 * MCP Resource that provides a list of all systems in the federated database.
 *
 * URI: datastore://systems
 */
public class SystemListResource extends AbstractMcpResource {

    public static final String URI = "datastore://systems";
    public static final String NAME = "System List";
    public static final String DESCRIPTION = "List of all sensor systems registered in the OpenSensorHub federated database, including their descriptions, inputs, and outputs.";

    private final IFederatedDatabase fedDb;

    public SystemListResource(IFederatedDatabase fedDb) {
        super();
        this.fedDb = fedDb;
    }

    @Override
    public String getUri() { return URI; }

    @Override
    public String getName() { return NAME; }

    @Override
    public String getDescription() { return DESCRIPTION; }

    @Override
    public String getMimeType() { return "application/json"; }

    @Override
    public Mono<McpSchema.ReadResourceResult> read(McpAsyncServerExchange exchange) {
        return Mono.fromCallable(() -> {
            var filter = new SystemFilter.Builder()
                    .withLimit(1000)
                    .build();

            List<Map<String, Object>> systems;
            try (var stream = fedDb.getSystemDescStore().select(filter)) {
                systems = stream
                        .map(this::toSummary)
                        .sorted(Comparator.comparing(
                                m -> (String) m.getOrDefault("name", ""),
                                String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("count", systems.size());
            response.put("systems", systems);

            String json = gson.toJson(response);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(URI, getMimeType(), json))
            );
        });
    }

    private Map<String, Object> toSummary(ISystemWithDesc system) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uniqueID", system.getUniqueIdentifier());
        map.put("name", system.getName());
        map.put("description", system.getDescription());
        map.put("type", system.getType());
        if (system.getValidTime() != null) {
            map.put("validTimeBegin", system.getValidTime().begin().toString());
            map.put("validTimeEnd", system.getValidTime().endsNow() ? "now" : system.getValidTime().end().toString());
        }

        AbstractProcess sml = system.getFullDescription();
        if (sml != null) {
            if (sml.getNumOutputs() > 0) {
                List<String> outputs = new ArrayList<>();
                for (int i = 0; i < sml.getOutputList().size(); i++)
                    outputs.add(sml.getOutputList().getComponent(i).getName());
                map.put("outputs", outputs);
            }
            if (sml.getNumInputs() > 0) {
                List<String> inputs = new ArrayList<>();
                for (int i = 0; i < sml.getInputList().size(); i++)
                    inputs.add(sml.getInputList().getComponent(i).getName());
                map.put("inputs", inputs);
            }
        }
        return map;
    }
}
