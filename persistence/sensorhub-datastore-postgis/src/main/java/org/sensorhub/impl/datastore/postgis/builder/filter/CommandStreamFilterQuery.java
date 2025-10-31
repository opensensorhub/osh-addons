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
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.vast.util.Asserts;

import java.util.SortedSet;
import java.util.stream.Collectors;

public class CommandStreamFilterQuery extends FilterQuery {

    protected CommandStreamFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected CommandStreamFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator, FilterQueryGenerator.InnerJoin innerJoin) {
        super(tableName, filterQueryGenerator, innerJoin);
    }

    public FilterQueryGenerator build(CommandStreamFilter filter) {
        this.handleInternalIDs(filter.getInternalIDs());
        this.handleControlInputNames(filter.getControlInputNames());
        this.handleValidTimeFilter(filter.getValidTimeFilter());
        this.handleFullTextFilter(filter.getFullTextFilter());
        this.handleCommandFilter(filter.getCommandFilter());
        this.handleSystemFilter(filter.getSystemFilter());
        this.handleTaskableProperties(filter.getTaskableProperties());
        return this.filterQueryGenerator;
    }

    protected  void handleControlInputNames(SortedSet<String> names) {
        if (names != null && !names.isEmpty()) {
            addCondition("("+this.tableName+".data->>'name') in (" +
                    names.stream().map(name -> "'" + name + "'").collect(Collectors.joining(",")) +
                    ")");
        }
    }

    protected void handleValidTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            addCondition(tableName+".data->'validTime'->'begin' IS NOT NULL");
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
                        " && " +
                        "'[" + min + "," + max + "]'::tstzrange";
                addCondition(sb);
            }
        }
    }

    protected void handleFullTextFilter(FullTextFilter fullTextFilter) {
        if (fullTextFilter != null) {
            String sb = "(" + this.tableName + ".data->'recordSchema'->>'description') ~* '(" +
                    fullTextFilter.getKeywords().stream().collect(Collectors.joining("|")) +
                    ")'";
            addCondition(sb);
        }
    }

    protected void handleCommandFilter(CommandFilter commandFilter) {
        if(commandFilter != null) {
            // create join
            Asserts.checkNotNull(this.commandTableName, "commandTableName should not be null");

            FilterQueryGenerator.InnerJoin innerJoin1 = new FilterQueryGenerator.InnerJoin(
                    this.commandTableName+" ON "+this.tableName+".id = "+this.commandTableName+".commandstreamid"
            );
            this.filterQueryGenerator.addInnerJoin(innerJoin1);
            CommandFilterQuery commandFilterQuery = new CommandFilterQuery(this.commandTableName, filterQueryGenerator,innerJoin1);
            commandFilterQuery.setCommandStreamTableName(this.tableName);
            this.filterQueryGenerator = commandFilterQuery.build(commandFilter);
        }
    }

    protected void handleSystemFilter(SystemFilter systemFilter) {
        if(systemFilter != null) {
            if(this.sysDescTableName != null) {
                // create JOIN
                // TODO
                throw new UnsupportedOperationException();
            } else {
                // otherwise
                if (systemFilter.getInternalIDs() != null || systemFilter.getUniqueIDs() != null) {
                    // handle UNIQUE IDS
                    if (systemFilter.getUniqueIDs() != null && !systemFilter.getUniqueIDs().isEmpty()) {
                        String sb = "(" + tableName + ".data->'system@id'->>'uniqueID') in ('" +
                                String.join("','", systemFilter.getUniqueIDs()) +
                                "')";
                        addCondition(sb);
                    }

                    // handle internal IDS
                    if (systemFilter.getInternalIDs() != null && !systemFilter.getInternalIDs().isEmpty()) {
                        String sb = "(" + tableName + ".data->'system@id'->'internalID'->'id')::bigint in (" +
                                systemFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                                ")";
                        addCondition(sb);
                    }
                }
                if (systemFilter.getParentFilter() != null || systemFilter.getProcedureFilter() != null
                        || systemFilter.getDataStreamFilter() != null || systemFilter.getFullTextFilter() != null
                        || systemFilter.getLocationFilter() != null || systemFilter.getValidTime() != null) {
                    throw new IllegalStateException("No linked system store");
                }
            }
        }
    }

    protected void handleTaskableProperties(SortedSet<String> properties) {
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
}
