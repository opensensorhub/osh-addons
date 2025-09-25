/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter;

import org.sensorhub.api.datastore.FullTextFilter;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.vast.util.Asserts;

import java.util.SortedSet;
import java.util.stream.Collectors;

public class DataStreamFilterQuery extends FilterQuery {
    private Long dsId;

    public DataStreamFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public FilterQueryGenerator build(DataStreamFilter filter) {
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
            filterQueryGenerator.addCondition("("+tableName+".data->>'outputName') in (" +
                    names.stream().map(name -> "'" + name + "'").collect(Collectors.joining(",")) +
                    ")");
        }
    }

    protected void handleValidTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            filterQueryGenerator.addCondition(tableName+".data->'validTime'->'begin' IS NOT NULL");
            if (temporalFilter.isLatestTime()) {
                filterQueryGenerator.addDistinct("("+tableName+".data->>'name')");
                filterQueryGenerator.addDistinct("("+tableName+".data->'system@id'->'internalID'->'id')::bigint");
                filterQueryGenerator.addOrderBy(tableName+".data->>'name'");
                filterQueryGenerator.addOrderBy("("+tableName+".data->'system@id'->'internalID'->'id')::bigint");
                filterQueryGenerator.addOrderBy("("+tableName+".data->'validTime'->>'end')::timestamptz DESC ");
            } else {
                String min = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMin());
                String max = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMax());

                String sb = "tstzrange((" +
                        tableName +
                        ".data->'validTime'->>'begin')::timestamptz,(" +
                        tableName +
                        ".data->'validTime'->>'end')::timestamptz)" +
                        " "+PostgisUtils.getOperator(temporalFilter)+" " +
                        "'[" + min + "," + max + "]'::tstzrange";
                filterQueryGenerator.addCondition(sb);
            }
        }
    }

    protected void handleFullTextFilter(FullTextFilter fullTextFilter) {
        if (fullTextFilter != null) {
            String sb = "(" + tableName + ".data->'recordSchema'->>'description') ~* '(" +
                    fullTextFilter.getKeywords().stream().collect(Collectors.joining("|")) +
                    ")'";
            filterQueryGenerator.addCondition(sb);
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
                        currentId = uid.replaceAll("\\*","%");
                        sb.append("(").append(tableName).append(".data->'system@id'->>'uniqueID') ILIKE '%").append(currentId).append("'");
                        if(++i < uniqueIds.size()) {
                            sb.append(" OR ");
                        }
                    }
                    sb.append(")");
                    filterQueryGenerator.addCondition(sb.toString());
                }

                // handle internal IDS
                if (systemFilter.getInternalIDs() != null && !systemFilter.getInternalIDs().isEmpty()) {
                    String sb = "(" + tableName + ".data->'system@id'->'internalID'->'id')::bigint in (" +
                            systemFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                            ")";
                    filterQueryGenerator.addCondition(sb);
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
            filterQueryGenerator.addCondition(sb.toString());
        }
    }

    public void setDataStreamId(long dsId) {
        this.dsId = dsId;
    }
}
