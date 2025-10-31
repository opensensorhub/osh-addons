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
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.vast.util.Asserts;

import static org.sensorhub.api.datastore.obs.IObsStore.ObsField.FOI_ID;

public class ObsStatsFilterQuery extends FilterQuery {
    Long dataStreamId;
    Long foiId;

    protected ObsStatsFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator, FilterQueryGenerator.InnerJoin innerJoin) {
        super(tableName, filterQueryGenerator, innerJoin);
    }

    protected ObsStatsFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }


    public void setDataStreamId(long dataStreamId) {
        this.dataStreamId = dataStreamId;
    }

    public void setFoiId(long foiId) {
        this.foiId = foiId;
    }

    public FilterQueryGenerator build(ObsFilter filter) {
        // only datastream filter used
        /*
        if (dataStreamId != null && foiId == null) {
            this.handleDataStreamFilter(filter.getDataStreamFilter());
        } else if (foiId != null && dataStreamId == null) {
            this.handleFoiFilter(filter.getFoiFilter(), foiId);
        } else {
            this.handleFoiFilter(filter.getFoiFilter(), foiId);
            this.handleDataStreamFilter(filter.getDataStreamFilter());
        }

*/
        this.handleDataStreamFilter(filter.getDataStreamFilter());
        this.handleFoiFilter(filter.getFoiFilter());
        this.handlePhenomenonTimeFilter(filter.getPhenomenonTime());
        this.handleResultTimeFilter(filter.getResultTime());

        return this.filterQueryGenerator;
    }

    protected void handleDataStreamFilter(DataStreamFilter dataStreamFilter) {
        if (dataStreamFilter != null) {
            // create join
            Asserts.checkNotNull(dataStreamTableName, "dataStreamTableName should not be null");

            FilterQueryGenerator.InnerJoin innerJoin1 = new FilterQueryGenerator.InnerJoin(
                    this.dataStreamTableName + " ON " + this.tableName + ".datastreamid = " + this.dataStreamTableName + ".id"
            );
            this.filterQueryGenerator.addInnerJoin(innerJoin1);
            DataStreamFilterQuery dataStreamFilterQuery = new DataStreamFilterQuery(this.dataStreamTableName, filterQueryGenerator,innerJoin1);
            dataStreamFilterQuery.setSysDescTableName(this.sysDescTableName);
            this.filterQueryGenerator = dataStreamFilterQuery.build(dataStreamFilter);
        }
    }

    protected void handleDataStreamFilter(DataStreamFilter dataStreamFilter, long dsId) {
        if (dataStreamFilter != null) {
            // create join
            Asserts.checkNotNull(dataStreamTableName, "dataStreamTableName should not be null");

            FilterQueryGenerator.InnerJoin innerJoin1 = new FilterQueryGenerator.InnerJoin(
                    this.dataStreamTableName + " ON " + this.tableName + ".datastreamid = " + this.dataStreamTableName + ".id"
            );
            this.filterQueryGenerator.addInnerJoin(innerJoin1);
            DataStreamFilterQuery dataStreamFilterQuery = new DataStreamFilterQuery(this.dataStreamTableName, filterQueryGenerator,innerJoin1);
            dataStreamFilterQuery.setSysDescTableName(this.sysDescTableName);
            dataStreamFilterQuery.setDataStreamId(this.dataStreamId);

            this.filterQueryGenerator = dataStreamFilterQuery.build(dataStreamFilter);
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

    protected void handleFoiFilter(FoiFilter foiFilter) {
        if (foiFilter != null) {
            if (this.foiTableName != null) {
                // create JOIN
                // TODO
            } else {
                // otherwise
                addCondition(this.tableName + "." + FOI_ID + " = " + foiId);
            }
            if (foiFilter.getParentFilter() != null || foiFilter.getObservationFilter() != null ||
                    foiFilter.getLocationFilter() != null || foiFilter.getSampledFeatureFilter() != null ||
                    foiFilter.getFullTextFilter() != null || foiFilter.getValidTime() != null) {
                throw new IllegalStateException("No linked foi store");
            }
        }
    }
}
