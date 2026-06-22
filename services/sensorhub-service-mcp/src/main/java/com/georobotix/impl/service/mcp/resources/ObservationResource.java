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
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import reactor.core.publisher.Mono;

import java.util.*;


/**
 * MCP Resource template providing latest observations for a system.
 *
 * URI Template: datastore://systems/{systemUID}/observations
 */
public class ObservationResource extends AbstractMcpResource {
    // TODO Add list URI
    // TODO Add URI for datastream observations
    public static final String URI_TEMPLATE = "datastore://systems/{systemUID}/observations";
    public static final String NAME = "System Observations";
    public static final String DESCRIPTION = "Latest observations from a specific system's datastreams, with decoded result values based on the datastream record structure.";

    private final IFederatedDatabase fedDb;
    private final ConSysJsonHelper consysJsonHelper;

    public ObservationResource(IFederatedDatabase fedDb) {
        super();
        this.fedDb = fedDb;
        this.consysJsonHelper = new ConSysJsonHelper(fedDb);
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
            var dsFilter = new DataStreamFilter.Builder()
                    .withSystems().withUniqueIDs(systemUID).done()
                    .withCurrentVersion()
                    .withLimit(100)
                    .build();

            List<Object> observations = new ArrayList<>();
            try (var dataStreams = fedDb.getDataStreamStore().selectEntries(dsFilter)) {
                var iterator = dataStreams.iterator();
                while (iterator.hasNext()) {
                    var dsId = iterator.next().getKey().getInternalID();
                    var obsFilter = new ObsFilter.Builder()
                            .withDataStreams(dsId)
                            .withLatestResult()
                            .withLimit(1)
                            .build();
                    try (var stream = fedDb.getObservationStore().select(obsFilter)) {
                        stream.findFirst()
                                .map(obs -> consysJsonHelper.parseJsonOrRaw(consysJsonHelper.writeObservation(obs, null, false)))
                                .ifPresent(observations::add);
                    }
                }
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
}
