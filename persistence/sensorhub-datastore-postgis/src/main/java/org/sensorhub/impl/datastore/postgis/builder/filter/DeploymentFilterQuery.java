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

import org.sensorhub.api.datastore.deployment.DeploymentFilter;
import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.system.IDeploymentWithDesc;
import org.vast.ogc.gml.IFeature;

import java.util.stream.Collectors;

public class DeploymentFilterQuery extends BaseFeatureFilterQuery<IDeploymentWithDesc, DeploymentFilter> {
    protected DeploymentFilterQuery(String tableName, FilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public FilterQueryGenerator build(DeploymentFilter filter) {
        this.filterQueryGenerator = super.build(filter);
        this.handleSystemFilter(filter.getSystemFilter());
        return this.filterQueryGenerator;
    }

    protected void handleSystemFilter(SystemFilter parentFilter) {
        if (parentFilter != null) {
            if (parentFilter.getInternalIDs() != null && !parentFilter.getInternalIDs().isEmpty()) {
                filterQueryGenerator.addCondition(tableName+".parentId in (" +
                        parentFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                        ")");
            }
            if(parentFilter.getUniqueIDs() != null) {
                filterQueryGenerator.addInnerJoin(this.tableName+ " t2 ON " + this.tableName + ".parentId" + " = t2.id");
                for(String uid: parentFilter.getUniqueIDs()) {
                    filterQueryGenerator.addCondition("t2.data->'properties'->>'uid' = '"+uid+"'");
                }
            }
        }
    }
}
