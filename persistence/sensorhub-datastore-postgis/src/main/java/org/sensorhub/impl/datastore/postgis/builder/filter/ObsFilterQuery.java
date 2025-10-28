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

import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.RangeFilter;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.vast.util.Asserts;

import java.util.stream.Collectors;

import static org.sensorhub.api.datastore.obs.IObsStore.ObsField.FOI_ID;

public class ObsFilterQuery extends FilterQuery {
    protected ObsFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected ObsFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator, FilterQueryGenerator.InnerJoin innerJoin) {
        super(tableName, filterQueryGenerator, innerJoin);
    }

    public FilterQueryGenerator build(ObsFilter filter) {
        this.handleDataStreamFilter(filter.getDataStreamFilter());
        this.handlePhenomenonTimeFilter(filter.getPhenomenonTime());
        this.handleResulTimeFilter(filter.getResultTime());
        this.handleFoiFilter(filter.getFoiFilter(), filter);
        return this.filterQueryGenerator;
    }

    protected void handleDataStreamFilter(DataStreamFilter dataStreamFilter) {
        if (dataStreamFilter != null) {
            // create join
            Asserts.checkNotNull(dataStreamTableName, "dataStreamTableName should not be null");

            FilterQueryGenerator.InnerJoin innerJoin1 = new FilterQueryGenerator.InnerJoin(this.dataStreamTableName + " ON " + this.tableName + ".datastreamid = " + this.dataStreamTableName + ".id");
            this.filterQueryGenerator.addInnerJoin(innerJoin1);
            DataStreamFilterQuery dataStreamFilterQuery = new DataStreamFilterQuery(this.dataStreamTableName, filterQueryGenerator, innerJoin1);
            dataStreamFilterQuery.setSysDescTableName(this.sysDescTableName);
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
                        "tstzrange('"+temporalFilter.getMin()+"','"+temporalFilter.getMax()+"', '[]') @> "+this.tableName+".phenomenonTime");
            }
        }
    }

    protected void handleResulTimeFilter(TemporalFilter temporalFilter) {
        if (temporalFilter != null) {
            if (temporalFilter.isLatestTime()) {
                filterQueryGenerator.addDistinct(this.tableName + ".datastreamid");
                filterQueryGenerator.addOrderBy(this.tableName + ".datastreamid");
                filterQueryGenerator.addOrderBy(this.tableName + ".phenomenonTime DESC ");
            } else {
                String min = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMin());
                String max = PostgisUtils.checkAndGetValidInstant(temporalFilter.getMax());
                addCondition(
                        "tstzrange('"+min+"','"+max+"', '[]') @> "+this.tableName+".resultTime");
            }
        }
    }

    protected void handleFoiFilter(FoiFilter foiFilter, ObsFilter obsFilter) {
        if (foiFilter != null) {
            if (this.foiTableName != null) {
                // create JOIN
                Asserts.checkNotNull(foiFilter, "foiTableName should not be null");

                FilterQueryGenerator.InnerJoin innerJoin1 = new FilterQueryGenerator.InnerJoin(
                        this.foiTableName + " ON " + this.tableName + ".foiid = " + this.foiTableName + ".id"
                );
                this.filterQueryGenerator.addInnerJoin(innerJoin1);
                FoiFilterQuery foiFilterQuery = new FoiFilterQuery(this.foiTableName, filterQueryGenerator,innerJoin1);
                this.filterQueryGenerator = foiFilterQuery.build(foiFilter);

                // Workaround, had validTime filter to Foi because it is owned by ObsFilter instead of FoiFilter
                foiFilterQuery.handleValidTimeFilter(obsFilter.getPhenomenonTime(), "<@");

            } else {
                // otherwise
                if (foiFilter.getInternalIDs() != null || foiFilter.getUniqueIDs() != null) {
                    // handle Internal IDs
                    if(foiFilter.getInternalIDs() != null) {
                        if (foiFilter.getInternalIDs().contains(BigId.NONE)) {
                            addCondition(this.tableName + "." + FOI_ID + " IS NULL");
                        } else {
                            addCondition(this.tableName + "." + FOI_ID + " in (" +
                                    foiFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                                    ")");
                        }
                    }
                }
                if (foiFilter.getParentFilter() != null || foiFilter.getObservationFilter() != null ||
                foiFilter.getLocationFilter() != null || foiFilter.getSampledFeatureFilter() != null ||
                foiFilter.getFullTextFilter() != null || foiFilter.getValidTime() != null)  {
                    throw new IllegalStateException("No linked foi store");
                }
            }
        }
    }
}
