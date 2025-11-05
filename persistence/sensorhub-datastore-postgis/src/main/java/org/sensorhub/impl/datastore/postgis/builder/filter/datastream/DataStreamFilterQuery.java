/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.datastream;

import org.sensorhub.api.datastore.FullTextFilter;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.FilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;
import org.vast.util.Asserts;

import java.util.SortedSet;
import java.util.stream.Collectors;

public abstract class DataStreamFilterQuery<F extends FilterQueryGenerator> extends FilterQuery<F> {
    private Long dsId;

    public DataStreamFilterQuery(String tableName, F filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public F build(DataStreamFilter filter) {
        if(this.dsId != null) {
            this.handleId(this.dsId);
        } else {
            this.handleInternalIDs(filter.getInternalIDs());
        }
        this.handleFullTextFilter(filter.getFullTextFilter());
        this.handleValidTimeFilter(filter.getValidTimeFilter());
        this.handleOutputNames(filter.getOutputNames());
        this.handleSystemFilter(filter.getSystemFilter());
        this.handleObsFilter(filter.getObservationFilter());
        this.handleObservedPropertiesFilter(filter.getObservedProperties());

        return this.filterQueryGenerator;
    }

    protected void handleId(long id) {
        filterQueryGenerator.addCondition(this.tableName+".id = "+id);
    }

    protected void handleOutputNames(SortedSet<String> names) {
        if (names != null && !names.isEmpty()) {
            addCondition("("+tableName+".data->>'outputName') in (" +
                    names.stream().map(name -> "'" + name + "'").collect(Collectors.joining(",")) +
                    ")");
        }
    }

    protected abstract void handleValidTimeFilter(TemporalFilter temporalFilter);

    protected void handleFullTextFilter(FullTextFilter fullTextFilter) {
        if (fullTextFilter != null) {
            addCondition( "(" + tableName + ".data->'recordSchema'->>'description') ~* '(" +
                    fullTextFilter.getKeywords().stream().collect(Collectors.joining("|")) +
                    ")'");
        }
    }

    /*
        protected void handleUniqueIds(SortedSet<String> uniqueIds) {
        if (uniqueIds != null) {
            StringBuilder sb = new StringBuilder();
            // Id can be regex
            // we have to use ILIKE behind trigram INDEX
            String currentId;
            int i = 0;
            sb.append("(");
            for(String uid: uniqueIds) {
                // ILIKE use % OPERATOR
                currentId = uid.replaceAll("\\*","%");
                sb.append("(").append(tableName).append(".data->>'uniqueId') ILIKE '%").append(currentId).append("'");
                if(++i < uniqueIds.size()) {
                    sb.append(" OR ");
                }
            }
            sb.append(")");
            filterQueryGenerator.addCondition(sb.toString());
        }
    }
     */
    protected void handleSystemFilter(SystemFilter systemFilter) {
        if (systemFilter != null) {
            if (systemFilter.getInternalIDs() != null || systemFilter.getUniqueIDs() != null) {
                // handle UNIQUE IDS
                if (systemFilter.getUniqueIDs() != null && !systemFilter.getUniqueIDs().isEmpty()) {
//                            "))";
                    SortedSet<String> uniqueIds = systemFilter.getUniqueIDs();
                    StringBuilder sb = new StringBuilder();
                    // Id can be regex
                    // we have to use ILIKE behind trigram INDEX
                    String currentId;
                    int i = 0;
                    sb.append("(");
                    for(String uid: uniqueIds) {
                        // ILIKE use % OPERATOR
                        String operator = "=";
                        currentId = uid;
                        if(uid.contains("*")) {
                            operator = "ILIKE";
                            currentId = uid.replaceAll("\\*","%");
                        }

                        sb.append("(").append(tableName).append(".data->'system@id'->>'uniqueID') "+operator+" '").append(currentId).append("'");
                        if(++i < uniqueIds.size()) {
                            sb.append(" OR ");
                        }
                    }
                    sb.append(")");
                    addCondition(sb.toString());
                }

                // handle internal IDS
                if (systemFilter.getInternalIDs() != null && !systemFilter.getInternalIDs().isEmpty()) {
                    String joinedIds = systemFilter.getInternalIDs().stream()
                            .map(bigId -> String.valueOf(bigId.getIdAsLong()))
                            .collect(Collectors.joining(","));

                    StringBuilder sb = new StringBuilder("(");

                    if (systemFilter.includeMembers()) {
                        // Use join
                        addJoin(sysDescTableName + " s ON (" + tableName +
                                ".data->'system@id'->'internalID'->>'id')::bigint = s.id");

                        sb.append("s.id IN (").append(joinedIds).append(")")
                                .append(" OR s.parentid IN (").append(joinedIds).append(")");
                    } else {
                        sb.append("(").append(tableName)
                                .append(".data->'system@id'->'internalID'->>'id')::bigint IN (")
                                .append(joinedIds).append(")");
                    }

                    sb.append(")");
                    addCondition(sb.toString());
                }
            }
            if (systemFilter.getParentFilter() != null || systemFilter.getProcedureFilter() != null
                    || systemFilter.getDataStreamFilter() != null || systemFilter.getFullTextFilter() != null
                    || systemFilter.getLocationFilter() != null || systemFilter.getValidTime() != null) {
                Asserts.checkState(sysDescTableName != null, "No linked system store");
                throw new UnsupportedOperationException();
            }
        }
    }

    protected void handleObsFilter(ObsFilter obsFilter) {
        if (obsFilter != null) {
            throw new UnsupportedOperationException();
        }
    }

    protected void handleObservedPropertiesFilter(SortedSet<String> properties) {
        if(properties != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("jsonb_path_exists(").append(tableName).append(".data, '$.** ? (");
            boolean first=true;
            for(String property : properties) {
                if(!first) {
                    // prepend operator
                    sb.append(" || ");
                }
                sb.append("@ == \"").append(property).append("\"");
                first = false;
            }
            sb.append(")')");
            addCondition(sb.toString());
        }
    }

    public void setDataStreamId(long dsId) {
        this.dsId = dsId;
    }

    public static boolean hasOnlyInternalIds(DataStreamFilter dataStreamFilter) {
        return (dataStreamFilter.getObservationFilter() == null &&
                dataStreamFilter.getSystemFilter() == null &&
                dataStreamFilter.getFullTextFilter() == null &&
                dataStreamFilter.getValidTimeFilter() == null &&
                dataStreamFilter.getOutputNames() == null &&
                dataStreamFilter.getObservedProperties() == null &&
                (dataStreamFilter.getInternalIDs() != null && !dataStreamFilter.getInternalIDs().isEmpty()));

    }
}
