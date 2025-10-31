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

import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.vast.ogc.gml.IFeature;
import org.vast.util.Asserts;

import java.util.stream.Collectors;

public class FoiFilterQuery extends BaseFeatureFilterQuery<IFeature, FoiFilter> {

    protected FoiFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected FoiFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator, FilterQueryGenerator.InnerJoin innerJoin) {
        super(tableName, filterQueryGenerator, innerJoin);
    }

    @Override
    public FilterQueryGenerator build(FoiFilter filter) {
        this.filterQueryGenerator = super.build(filter);
//        filter.getObservationFilter()
//        filter.getSampledFeatureFilter()
//        filter.getParentFilter()
        this.handleObsFilter(filter.getObservationFilter());
        this.handleParentFilter(filter.getParentFilter());
        return this.filterQueryGenerator;
    }

    protected void handleObsFilter(ObsFilter obsFilter) {
        if (obsFilter != null) {
            // create join
            Asserts.checkNotNull(obsTableName, "obsTableName should not be null");

            FilterQueryGenerator.InnerJoin innerJoin1 = new FilterQueryGenerator.InnerJoin(
                    this.obsTableName + " ON " + this.tableName + ".id = " + this.obsTableName + ".foiid"
            );
            this.filterQueryGenerator.addInnerJoin(innerJoin1);
            ObsFilterQuery obsFilterQuery = new ObsFilterQuery(this.obsTableName, filterQueryGenerator,innerJoin1);
            obsFilterQuery.setSysDescTableName(this.sysDescTableName);
            obsFilterQuery.setDataStreamTableName(this.dataStreamTableName);
            obsFilterQuery.setObsTableName(this.obsTableName);
            this.filterQueryGenerator = obsFilterQuery.build(obsFilter);
        }
    }

    protected void handleParentFilter(SystemFilter parentFilter) {
        if (parentFilter != null) {
            if (parentFilter.getInternalIDs() != null && !parentFilter.getInternalIDs().isEmpty()) {
                addCondition(tableName+".parentId in (" +
                        parentFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                        ")");
            }
            if(parentFilter.getUniqueIDs() != null) {
                FilterQueryGenerator.InnerJoin innerJoin1 = new FilterQueryGenerator.InnerJoin(
                        this.tableName+ " t2 ON " + this.tableName + ".parentId" + " = t2.id"
                );
                filterQueryGenerator.addInnerJoin(innerJoin1);
                for(String uid: parentFilter.getUniqueIDs()) {
                    innerJoin1.addCondition("t2.data->'properties'->>'uid' = '"+uid+"'");
                }
            }
        }
    }
}
