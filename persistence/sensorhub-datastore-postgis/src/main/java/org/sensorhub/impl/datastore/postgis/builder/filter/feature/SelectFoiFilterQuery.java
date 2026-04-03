/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.feature;

import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.obs.SelectObsFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;
import org.vast.ogc.gml.IFeature;
import org.vast.util.Asserts;

import java.util.stream.Collectors;

public class SelectFoiFilterQuery extends BaseFeatureFilterQuery<IFeature, FoiFilter, SelectFilterQueryGenerator> {
    public SelectFoiFilterQuery(String tableName, SelectFilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    protected void handleObsFilter(ObsFilter obsFilter) {
        if (obsFilter != null) {
            // create join
            Asserts.checkNotNull(obsTableName, "obsTableName should not be null");

            this.filterQueryGenerator.addInnerJoin(this.obsTableName + " ON " + this.tableName + ".id = " + this.obsTableName + ".foiid");
            SelectObsFilterQuery obsFilterQuery = new SelectObsFilterQuery(this.obsTableName, filterQueryGenerator);
            obsFilterQuery.setSysDescTableName(this.sysDescTableName);
            obsFilterQuery.setDataStreamTableName(this.dataStreamTableName);
            obsFilterQuery.setObsTableName(this.obsTableName);
            this.filterQueryGenerator = obsFilterQuery.build(obsFilter);
        }
    }

    //TODO: move to parent?
    protected void handleParentFilter(FeatureFilter parentFilter) {
        if (parentFilter != null) {
            if (parentFilter.getInternalIDs() != null && !parentFilter.getInternalIDs().isEmpty()) {
                addCondition(tableName+".parentId in (" +
                        parentFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                        ")");
            }
            this.filterQueryGenerator.addInnerJoin(this.tableName+" t1 ON "+this.tableName + ".parentId" + " = t1.id");

            if(parentFilter.getUniqueIDs() != null) {
                for(String uid: parentFilter.getUniqueIDs()) {
                    this.filterQueryGenerator.addCondition("t1.data->'properties'->>'uid' = '"+uid+"'");
                }
            }
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
                this.filterQueryGenerator.addInnerJoin(this.tableName+" t1 ON "+this.tableName + ".parentId" + " = t1.id");

                for(String uid: parentFilter.getUniqueIDs()) {
                    this.filterQueryGenerator.addCondition("t1.data->'properties'->>'uid' = '"+uid+"'");
                }
            }
        }
    }
}
