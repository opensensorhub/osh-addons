/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.command;

import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.command.CommandStatusFilter;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.impl.datastore.postgis.builder.generator.RemoveFilterQueryGenerator;
import org.vast.util.Asserts;

import static org.sensorhub.api.datastore.command.ICommandStore.CommandField.ISSUE_TIME;

public class RemoveCommandFilterQuery extends BaseCommandFilterQuery<RemoveFilterQueryGenerator> {

    public RemoveCommandFilterQuery(String tableName, RemoveFilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected void handleIssueTimeTemporalFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            if (temporalFilter.isLatestTime()) {
               throw new UnsupportedOperationException("IssueTimeTemporalFilter not supported into REMOVE");
            } else {
                addCondition(this.tableName + "." + ISSUE_TIME + "::timestamptz BETWEEN " +
                        "'" + temporalFilter.getMin().toString() + "' AND '" + temporalFilter.getMax().toString() + "'");
            }
        }
    }

    protected void handleCommandStreamFilter(CommandStreamFilter commandStreamFilter) {
        if (commandStreamFilter != null) {
            // create join
            Asserts.checkNotNull(this.commandStreamTableName, "commandStreamTableName should not be null");

            this.filterQueryGenerator.addUsing(this.commandStreamTableName);
            this.filterQueryGenerator.addCondition(this.tableName + ".commandstreamid = " + this.commandStreamTableName + ".id");

            RemoveCommandStreamFilterQuery commandStreamFilterQuery = new RemoveCommandStreamFilterQuery(this.commandStreamTableName, filterQueryGenerator);
            commandStreamFilterQuery.setCommandTableName(this.tableName);
            commandStreamFilterQuery.setSysDescTableName(this.sysDescTableName);
            this.filterQueryGenerator = commandStreamFilterQuery.build(commandStreamFilter);
        }
    }

    protected void handleCommandStatusFilter(CommandStatusFilter commandStatusFilter) {
        if(commandStatusFilter != null) {
            // create join
            Asserts.checkNotNull(this.commandStatusTableName, "commandStatusTableName should not be null");

            this.filterQueryGenerator.addUsing(this.commandStatusTableName);
            this.filterQueryGenerator.addCondition("("+this.tableName+".id) = ("+this.commandStatusTableName+".commandID)::bigint");

            RemoveCommandStatusFilterQuery commandStatusFilterQuery = new RemoveCommandStatusFilterQuery(this.commandStatusTableName, filterQueryGenerator);
            commandStatusFilterQuery.setCommandStatusTableName(this.tableName);
            commandStatusFilterQuery.setSysDescTableName(this.sysDescTableName);
            this.filterQueryGenerator = commandStatusFilterQuery.build(commandStatusFilter);
        }
    }
}
