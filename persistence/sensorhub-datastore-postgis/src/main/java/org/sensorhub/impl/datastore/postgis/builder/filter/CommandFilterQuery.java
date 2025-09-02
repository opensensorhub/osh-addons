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

import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.CommandStatusFilter;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.vast.util.Asserts;

import java.util.SortedSet;
import java.util.stream.Collectors;

import static org.sensorhub.api.datastore.command.ICommandStore.CommandField.ISSUE_TIME;
public class CommandFilterQuery extends FilterQuery {

    public CommandFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public FilterQueryGenerator build(CommandFilter filter) {
        this.handleInternalIDs(filter.getInternalIDs());
        this.handleSenderIDs(filter.getSenderIDs());
        this.handleIssueTimeTemporalFilter(filter.getIssueTime());
        this.handleCommandStreamFilter(filter.getCommandStreamFilter());
        this.handleCommandStatusFilter(filter.getStatusFilter());
        return this.filterQueryGenerator;
    }

    protected void handleSenderIDs(SortedSet<String> ids) {
        if (ids != null && !ids.isEmpty()) {
            filterQueryGenerator.addCondition(this.tableName + ".sendId in (" +
                    ids.stream().collect(Collectors.joining(",")) +
                    ")");
        }
    }

    protected void handleIssueTimeTemporalFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            if (temporalFilter.isLatestTime()) {
                filterQueryGenerator.addDistinct(this.tableName + ".commandstreamid");
                filterQueryGenerator.addOrderBy(this.tableName + ".commandstreamid");
                filterQueryGenerator.addOrderBy(this.tableName + ".issuetime DESC ");
            } else {
                filterQueryGenerator.addCondition(this.tableName + "." + ISSUE_TIME + "::timestamptz BETWEEN " +
                        "'" + temporalFilter.getMin().toString() + "' AND '" + temporalFilter.getMax().toString() + "'");
            }
        }
    }

    protected void handleCommandStreamFilter(CommandStreamFilter commandStreamFilter) {
        if (commandStreamFilter != null) {
            // create join
            Asserts.checkNotNull(this.commandStreamTableName, "commandStreamTableName should not be null");

            this.filterQueryGenerator.addInnerJoin(
                    this.commandStreamTableName + " ON " + this.tableName + ".commandstreamid = " + this.commandStreamTableName + ".id");
            CommandStreamFilterQuery commandStreamFilterQuery = new CommandStreamFilterQuery(this.commandStreamTableName, filterQueryGenerator);
            commandStreamFilterQuery.setCommandTableName(this.tableName);
            commandStreamFilterQuery.setSysDescTableName(this.sysDescTableName);
            this.filterQueryGenerator = commandStreamFilterQuery.build(commandStreamFilter);
        }
    }

    protected void handleCommandStatusFilter(CommandStatusFilter commandStatusFilter) {
        if(commandStatusFilter != null) {
            // create join
            Asserts.checkNotNull(this.commandStatusTableName, "commandStatusTableName should not be null");

            this.filterQueryGenerator.addInnerJoin(
                    this.commandStatusTableName+" ON ("+this.tableName+".id) = ("+this.commandStatusTableName+".data->'command@id'->'id')::bigint");
            CommandStatusFilterQuery commandStatusFilterQuery = new CommandStatusFilterQuery(this.commandStatusTableName, filterQueryGenerator);
            commandStatusFilterQuery.setCommandStatusTableName(this.tableName);
            commandStatusFilterQuery.setSysDescTableName(this.sysDescTableName);
            this.filterQueryGenerator = commandStatusFilterQuery.build(commandStatusFilter);
        }
    }
}
