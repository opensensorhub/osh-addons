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
import org.vast.util.Asserts;

public class RemoveCommandStreamFilterQuery extends BaseCommandStreamFilterQuery<RemoveFilterQueryGenerator> {

    public RemoveCommandStreamFilterQuery(String tableName, RemoveFilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected void handleValidTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            addCondition(tableName+".data->'validTime'->'begin' IS NOT NULL");
            if (temporalFilter.isLatestTime()) {
              throw new UnsupportedOperationException("ValidTimeFilter does not support Latest for REMOVE operation");
            } else {
                String min = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMin());
                String max = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMax());

                String sb = "tsrange((" +
                        tableName +
                        ".data->'validTime'->>'begin')::timestamp,(" +
                        tableName +
                        ".data->'validTime'->>'end')::timestamp)" +
                        " && " +
                        "'[" + min + "," + max + "]'::tsrange";
                addCondition(sb);
            }
        }
    }

    protected void handleCommandFilter(CommandFilter commandFilter) {
        if(commandFilter != null) {
            // create join
            Asserts.checkNotNull(this.commandTableName, "commandTableName should not be null");

            this.filterQueryGenerator.addUsing(this.commandTableName);
            this.filterQueryGenerator.addCondition(this.tableName+".id = "+this.commandTableName+".commandstreamid");

            RemoveCommandFilterQuery commandFilterQuery = new RemoveCommandFilterQuery(this.commandTableName, filterQueryGenerator);
            commandFilterQuery.setCommandStreamTableName(this.tableName);
            this.filterQueryGenerator = commandFilterQuery.build(commandFilter);
        }
    }
}
