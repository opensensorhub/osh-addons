/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.stats;

import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.datastream.SelectDataStreamFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterStatsQueryGenerator;
import org.vast.util.Asserts;

public class SelectObsStatsFilterQuery extends BaseObsStatsFilterQuery<SelectFilterStatsQueryGenerator> {

    public SelectObsStatsFilterQuery(String tableName, SelectFilterStatsQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected void handleDataStreamFilter(DataStreamFilter dataStreamFilter) {
        if (dataStreamFilter != null) {
            // create join
            Asserts.checkNotNull(dataStreamTableName, "dataStreamTableName should not be null");

            this.filterQueryGenerator.addInnerJoin(this.dataStreamTableName + " ON " + this.tableName + ".datastreamid = " + this.dataStreamTableName + ".id");
            SelectDataStreamFilterQuery dataStreamFilterQuery = new SelectDataStreamFilterQuery(this.dataStreamTableName, filterQueryGenerator);
            dataStreamFilterQuery.setSysDescTableName(this.sysDescTableName);
            this.filterQueryGenerator = (SelectFilterStatsQueryGenerator) dataStreamFilterQuery.build(dataStreamFilter);
        }
    }

    protected void handleDataStreamFilter(DataStreamFilter dataStreamFilter, long dsId) {
        if (dataStreamFilter != null) {
            // create join
            Asserts.checkNotNull(dataStreamTableName, "dataStreamTableName should not be null");

            this.filterQueryGenerator.addInnerJoin( this.dataStreamTableName + " ON " + this.tableName + ".datastreamid = " + this.dataStreamTableName + ".id");
            SelectDataStreamFilterQuery dataStreamFilterQuery = new SelectDataStreamFilterQuery(this.dataStreamTableName, filterQueryGenerator);
            dataStreamFilterQuery.setSysDescTableName(this.sysDescTableName);
            dataStreamFilterQuery.setDataStreamId(this.dataStreamId);

            this.filterQueryGenerator = (SelectFilterStatsQueryGenerator) dataStreamFilterQuery.build(dataStreamFilter);
        }
    }

    protected void handlePhenomenonTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            if (temporalFilter.isLatestTime()) {
                filterQueryGenerator.addDistinct(this.tableName + ".datastreamid");
                filterQueryGenerator.addOrderBy(this.tableName + ".datastreamid");
                filterQueryGenerator.addOrderBy(this.tableName + ".phenomenonTime DESC ");
            } else {
                addCondition(
                        "tstzrange('" + temporalFilter.getMin() + "','" + temporalFilter.getMax() + "', '[]') @> " + this.tableName + ".phenomenonTime");
            }
        }
    }

    protected void handleResultTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            if (temporalFilter.isLatestTime()) {
                filterQueryGenerator.addDistinct(this.tableName + ".datastreamid");
                filterQueryGenerator.addOrderBy(this.tableName + ".datastreamid");
                filterQueryGenerator.addOrderBy(this.tableName + ".phenomenonTime DESC ");
            } else {
                addCondition(
                        "tstzrange('" + temporalFilter.getMin() + "','" + temporalFilter.getMax() + "', '[]') @> " + this.tableName + ".resultTime");
            }
        }
    }
}
