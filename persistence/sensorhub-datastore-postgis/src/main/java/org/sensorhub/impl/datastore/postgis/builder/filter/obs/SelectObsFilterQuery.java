/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.obs;

import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.datastream.SelectDataStreamFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.SelectFoiFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.vast.util.Asserts;

import java.util.stream.Collectors;

import static org.sensorhub.api.datastore.obs.IObsStore.ObsField.FOI_ID;

public class SelectObsFilterQuery extends BaseObsFilterQuery<SelectFilterQueryGenerator> {
    public SelectObsFilterQuery(String tableName, SelectFilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected void handleDataStreamFilter(DataStreamFilter dataStreamFilter) {
        if (dataStreamFilter != null) {
            // create join
            Asserts.checkNotNull(dataStreamTableName, "dataStreamTableName should not be null");

            this.filterQueryGenerator.addInnerJoin(this.dataStreamTableName + " ON " + this.tableName + ".datastreamid = " + this.dataStreamTableName + ".id");
            SelectDataStreamFilterQuery dataStreamFilterQuery = new SelectDataStreamFilterQuery(this.dataStreamTableName, filterQueryGenerator);
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

    protected void handleResultTimeFilter(TemporalFilter temporalFilter) {
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
                Asserts.checkNotNull(foiFilter, "foiFilter should not be null");

                this.filterQueryGenerator.addInnerJoin(this.foiTableName + " ON " + this.tableName + ".foiid = " + this.foiTableName + ".id");
                SelectFoiFilterQuery foiFilterQuery = new SelectFoiFilterQuery(this.foiTableName, filterQueryGenerator);
                foiFilterQuery.setFoiTableName(this.foiTableName);

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
