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

import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.CommandStatusFilter;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.vast.util.Asserts;

import java.util.Set;
import java.util.stream.Collectors;

public class CommandStatusFilterQuery extends FilterQuery {

    protected CommandStatusFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator, FilterQueryGenerator.InnerJoin innerJoin) {
        super(tableName, filterQueryGenerator, innerJoin);
    }

    protected CommandStatusFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public FilterQueryGenerator build(CommandStatusFilter filter) {
        this.handleReportTimeFilter(filter.getReportTime());
        this.handleExecutionTimeFilter(filter.getExecutionTime());
        this.handleStatusCodes(filter.getStatusCodes());
        this.handleLatest();
        this.handleCommandFilter(filter.getCommandFilter());
        return this.filterQueryGenerator;
    }

    protected void handleReportTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            addCondition(this.tableName+".data->'reportTime' IS NOT NULL");
            if (temporalFilter.isLatestTime()) {
                filterQueryGenerator.addDistinct(this.tableName+".data->'command@id'");
                filterQueryGenerator.addDistinct(this.tableName+".data->'reportTime'");
                filterQueryGenerator.addOrderBy(this.tableName+".data->'command@id'");
                filterQueryGenerator.addOrderBy(this.tableName+".data->'reportTime' DESC ");
            } else {
                String min = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMin());
                String max = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMax());

                String sb = "tstzrange((" +
                        tableName +
                        ".data->'reportTime'->>'begin')::timestamptz,(" +
                        tableName +
                        ".data->'reportTime'->>'end')::timestamptz, '[]')" +
                        " && " +
                        "'[" + min + "," + max + "]'::tstzrange";
                addCondition(sb);
            }
        }
    }

    protected void handleExecutionTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            addCondition(this.tableName+".data->'executionTime' IS NOT NULL");
            if (temporalFilter.isLatestTime()) {
                filterQueryGenerator.addDistinct(this.tableName+".data->'command@id'");
                filterQueryGenerator.addDistinct(this.tableName+".data->'executionTime'");
                filterQueryGenerator.addOrderBy(this.tableName+".data->'command@id'");
                filterQueryGenerator.addOrderBy(this.tableName+".data->'executionTime' DESC ");
            } else {
                String min = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMin());
                String max = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMax());

                String sb = "tstzrange((" +
                        tableName +
                        ".data->'executionTime'->>'begin')::timestamptz,(" +
                        tableName +
                        ".data->'executionTime'->>'end')::timestamptz, '[]')" +
                        " && " +
                        "'[" + min + "," + max + "]'::tstzrange";
                addCondition(sb);
            }
        }
    }

    protected void handleStatusCodes(Set<ICommandStatus.CommandStatusCode> statusCodes) {
        if(statusCodes != null && !statusCodes.isEmpty()) {
            addCondition("("+this.tableName+".data->>'statusCode') in (" +
                    statusCodes.stream().map(name -> "'"+name.name()+"'").collect(Collectors.joining(",")) +
                    ")");
        }
    }

    protected void handleLatest() {
        filterQueryGenerator.addDistinct(this.tableName+".data->'command@id'->>'id'");
        filterQueryGenerator.addDistinct(this.tableName+".data->'command@id'->>'scope'");
        filterQueryGenerator.addDistinct(this.tableName+".data->'reportTime'");
        filterQueryGenerator.addOrderBy(this.tableName+".data->'command@id'->>'id'");
        filterQueryGenerator.addOrderBy(this.tableName+".data->'command@id'->>'scope'");
        filterQueryGenerator.addOrderBy(this.tableName+".data->'reportTime' DESC ");
    }

    protected void handleCommandFilter(CommandFilter commandFilter) {
        if(commandFilter != null) {
            // create join
            Asserts.checkNotNull(this.commandTableName, "Command table name should not be null");
            FilterQueryGenerator.InnerJoin innerJoin1 = new FilterQueryGenerator.InnerJoin(
                    this.commandTableName+" ON ("+this.tableName+".data->'command@id'->'id')::bigint = "+this.commandTableName+".id"
            );
            this.filterQueryGenerator.addInnerJoin(innerJoin1);
            CommandFilterQuery commandFilterQuery = new CommandFilterQuery(this.commandTableName, filterQueryGenerator,innerJoin1);
            commandFilterQuery.setCommandStatusTableName(this.tableName);
            commandFilterQuery.setCommandStreamTableName(this.commandStreamTableName);
            this.filterQueryGenerator = commandFilterQuery.build(commandFilter);
        }
    }
}
