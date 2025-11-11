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
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.impl.datastore.postgis.builder.generator.RemoveFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;

public class RemoveCommandStatusFilterQuery extends BaseCommandStatusFilterQuery<RemoveFilterQueryGenerator> {

    public RemoveCommandStatusFilterQuery(String tableName, RemoveFilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected void handleReportTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            addCondition(this.tableName+".reportTime IS NOT NULL");
            if (temporalFilter.isLatestTime()) {
                throw new UnsupportedOperationException("ReportTimeFilter is not supported for latest into REMOVE clause");
            } else {
                String min = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMin());
                String max = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMax());

                String sb = tableName + ".reportTime BETWEEN '"
                        + min + "'::timestamptz AND '"
                        + max + "'::timestamptz";
                addCondition(sb);
            }
        }
    }

    protected void handleExecutionTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            addCondition(this.tableName+".executionTime IS NOT NULL");
            if (temporalFilter.isLatestTime()) {
                throw new UnsupportedOperationException("ExecutionTimeFilter is not supported for latest into REMOVE clause");
            } else {
                String min = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMin());
                String max = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMax());

                String sb = tableName + ".executionTime "
                        + PostgisUtils.getOperator(temporalFilter.getOperator())
                        + " tsrange('" + min + "','" + max + "', '[]')";
                addCondition(sb);
            }
        }
    }

    protected void handleCommandFilter(CommandFilter commandFilter) {
        if(commandFilter != null) {
            // create join
            this.filterQueryGenerator.addUsing(this.commandTableName);
            this.filterQueryGenerator.addCondition("("+this.tableName+".commandID)::bigint = "+this.commandTableName+".id");

            RemoveCommandFilterQuery commandFilterQuery = new RemoveCommandFilterQuery(this.commandTableName, filterQueryGenerator);
            commandFilterQuery.setCommandStatusTableName(this.tableName);
            commandFilterQuery.setCommandStreamTableName(this.commandStreamTableName);
            this.filterQueryGenerator = commandFilterQuery.build(commandFilter);
        }
    }
}
