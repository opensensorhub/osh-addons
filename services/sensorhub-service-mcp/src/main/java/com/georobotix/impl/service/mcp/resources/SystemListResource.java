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

import com.georobotix.impl.service.mcp.ConSysJsonHelper;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.sensorhub.api.database.IFederatedDatabase;
import org.sensorhub.api.datastore.system.SystemFilter;
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
    private final ConSysJsonHelper consysJsonHelper;

    public SystemListResource(IFederatedDatabase fedDb) {
        super();
        this.fedDb = fedDb;
        this.consysJsonHelper = new ConSysJsonHelper(fedDb);
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

            List<Object> systems;
            try (var stream = fedDb.getSystemDescStore().select(filter)) {
                systems = stream
                        .map(system -> consysJsonHelper.parseJsonOrRaw(consysJsonHelper.writeSystem(system, null, false)))
                        .sorted(Comparator.comparing(
                                this::systemSortName,
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

    @SuppressWarnings("unchecked")
    private String systemSortName(Object system) {
        if (!(system instanceof Map<?, ?> map))
            return "";

        Object properties = map.get("properties");
        if (properties instanceof Map<?, ?> props)
            return Objects.toString(props.get("name"), "");

        return Objects.toString(map.get("name"), "");
    }
}
