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
import org.sensorhub.api.datastore.command.CommandStatusFilter;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.vast.util.Asserts;

public class SelectCommandStatusFilterQuery extends BaseCommandStatusFilterQuery<SelectFilterQueryGenerator> {

    public SelectCommandStatusFilterQuery(String tableName, SelectFilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public SelectFilterQueryGenerator build(CommandStatusFilter filter) {
        filterQueryGenerator = super.build(filter);
        this.handleLatest();
        return filterQueryGenerator;
    }

    protected void handleReportTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            addCondition(this.tableName+".reportTime IS NOT NULL");
            if (temporalFilter.isLatestTime()) {
                filterQueryGenerator.addDistinct(this.tableName+".commandID");
                filterQueryGenerator.addDistinct(this.tableName+".reportTime");
                filterQueryGenerator.addOrderBy(this.tableName+".commandID");
                filterQueryGenerator.addOrderBy(this.tableName+".reportTime DESC ");
            } else {
                String min = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMin());
                String max = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMax());

                String sb = tableName + ".reportTime BETWEEN '"
                        + min + "'::timestamp AND '"
                        + max + "'::timestamp";
                addCondition(sb);
            }
        }
    }

    protected void handleExecutionTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            addCondition(this.tableName+".executionTime IS NOT NULL");
            if (temporalFilter.isLatestTime()) {
                filterQueryGenerator.addDistinct(this.tableName+".commandID");
                filterQueryGenerator.addDistinct(this.tableName+".executionTime");
                filterQueryGenerator.addOrderBy(this.tableName+".commandID");
                filterQueryGenerator.addOrderBy(this.tableName+".executionTime DESC ");
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

    protected void handleLatest() {
        filterQueryGenerator.addDistinct(this.tableName+".commandID");
        filterQueryGenerator.addDistinct(this.tableName+".reportTime");
        filterQueryGenerator.addOrderBy(this.tableName+".commandID");
        filterQueryGenerator.addOrderBy(this.tableName+".reportTime DESC ");
    }

    protected void handleCommandFilter(CommandFilter commandFilter) {
        if(commandFilter != null) {
            // create join
            Asserts.checkNotNull(this.commandTableName, "Command table name should not be null");
            this.filterQueryGenerator.addInnerJoin(this.commandTableName+" ON ("+this.tableName+".commandID)::bigint = "+this.commandTableName+".id");
            SelectCommandFilterQuery commandFilterQuery = new SelectCommandFilterQuery(this.commandTableName, filterQueryGenerator);
            commandFilterQuery.setCommandStatusTableName(this.tableName);
            commandFilterQuery.setCommandStreamTableName(this.commandStreamTableName);
            this.filterQueryGenerator = commandFilterQuery.build(commandFilter);
        }
    }
}
