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
            if (systemFilter.getUniqueIDs() != null && !systemFilter.getUniqueIDs().isEmpty()) {
                SortedSet<String> uniqueIds = systemFilter.getUniqueIDs();

                StringBuilder sb = new StringBuilder("(");
                int i = 0;

                for (String uid : uniqueIds) {
                    String operator = "=";
                    String currentId = escapeSqlString(uid);

                    if (uid.contains("*")) {
                        operator = "ILIKE";
                        currentId = escapeSqlString(uid.replace("*", "%"));
                    }

                    if (i > 0) {
                        sb.append(" OR ");
                    }

                    // Direct match on datastream's system
                    sb.append("(").append(tableName).append(".data->'system@id'->>'uniqueID' ")
                            .append(operator).append(" '").append(currentId).append("'");

                    if (systemFilter.includeMembers()) {
                        sb.append(" OR EXISTS (SELECT 1 FROM ")
                                .append(sysDescTableName).append(" s WHERE s.data->>'uniqueId' = ")
                                .append(tableName).append(".data->'system@id'->>'uniqueID'")
                                .append(" AND s.parentid IN (SELECT id FROM ")
                                .append(sysDescTableName).append(" WHERE data->>'uniqueId' ")
                                .append(operator).append(" '").append(currentId).append("'))");
                    }

                    sb.append(")");
                    i++;
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

            if (systemFilter.getParentFilter() != null || systemFilter.getProcedureFilter() != null
                    || systemFilter.getDataStreamFilter() != null || systemFilter.getFullTextFilter() != null
                    || systemFilter.getLocationFilter() != null || systemFilter.getValidTime() != null) {
                Asserts.checkState(sysDescTableName != null, "No linked system store");
                throw new UnsupportedOperationException();
            }
        }
    }

    private String escapeSqlString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    protected void handleObsFilter(ObsFilter obsFilter) {
        if (obsFilter != null) {
            throw new UnsupportedOperationException();
        }
    }

    protected void handleObservedPropertiesFilter(SortedSet<String> properties) {
        if (properties == null || properties.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("jsonb_path_exists(")
                .append(tableName)
                .append(".data->'recordSchema', '$.** ? (");

        boolean first = true;
        for (String property : properties) {
            if (!first) {
                sb.append(" || ");
            }
            sb.append("@.definition == \"").append(escapeJsonString(property)).append("\"");
            first = false;
        }

        sb.append(")')");
        addCondition(sb.toString());
    }

    private String escapeJsonString(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
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
