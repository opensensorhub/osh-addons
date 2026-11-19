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
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.datastore.postgis.builder.generator.RemoveFilterQueryGenerator;
import org.vast.ogc.gml.IFeature;

import java.util.stream.Collectors;

public class RemoveFeatureFilterQuery extends BaseFeatureFilterQuery<IFeature,FeatureFilter, RemoveFilterQueryGenerator> {
    public RemoveFeatureFilterQuery(String tableName, RemoveFilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public RemoveFilterQueryGenerator build(FeatureFilter filter) {
        this.filterQueryGenerator = super.build(filter);
        this.handleParentFilter(filter.getParentFilter());
        return this.filterQueryGenerator;
    }

    protected void handleParentFilter(FeatureFilter parentFilter) {
        if (parentFilter != null) {
            if (parentFilter.getInternalIDs() != null && !parentFilter.getInternalIDs().isEmpty()) {
                addCondition(tableName+".parentId in (" +
                        parentFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                        ")");
            }
            if(parentFilter.getUniqueIDs() != null) {
                this.filterQueryGenerator.addUsing(this.tableName);
                for(String uid: parentFilter.getUniqueIDs()) {
                    this.filterQueryGenerator.addCondition(this.tableName+".data->'properties'->>'uid' = '"+uid+"'");
                }
            }
        }
    }

    @Override
    protected void handleParentFilter(SystemFilter parentFilter) {
        throw new UnsupportedOperationException();
    }
}
