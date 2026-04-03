/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.filter.deployment;

import org.sensorhub.api.datastore.deployment.DeploymentFilter;
import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.system.IDeploymentWithDesc;
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.BaseFeatureFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.generator.SelectFilterQueryGenerator;

import java.util.stream.Collectors;

public class SelectDeploymentFilterQuery extends DeploymentFilterQuery<SelectFilterQueryGenerator> {

    public SelectDeploymentFilterQuery(String tableName, SelectFilterQueryGenerator filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public SelectFilterQueryGenerator build(DeploymentFilter filter) {
        this.filterQueryGenerator = super.build(filter);
        this.handleSystemFilter(filter.getSystemFilter());
        return this.filterQueryGenerator;
    }

    @Override
    protected void handleParentFilter(FeatureFilter parentFilter) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void handleParentFilter(SystemFilter parentFilter) {
        throw new UnsupportedOperationException();
    }

    protected void handleSystemFilter(SystemFilter parentFilter) {
        if (parentFilter != null) {
            if (parentFilter.getInternalIDs() != null && !parentFilter.getInternalIDs().isEmpty()) {
                addCondition(tableName+".parentId in (" +
                        parentFilter.getInternalIDs().stream().map(bigId -> String.valueOf(bigId.getIdAsLong())).collect(Collectors.joining(",")) +
                        ")");
            }
            if(parentFilter.getUniqueIDs() != null) {
                this.filterQueryGenerator.addInnerJoin(this.tableName+ " ON " + this.tableName + ".parentId" + " = "+this.sysDescTableName+".id");
                //TODO condition on the JOIN faster than on the WHERE?
                for(String uid: parentFilter.getUniqueIDs()) {
                    this.filterQueryGenerator.addCondition(this.tableName+".data->'properties'->>'uid' = '"+uid+"'");
                }
            }
        }
    }
}
