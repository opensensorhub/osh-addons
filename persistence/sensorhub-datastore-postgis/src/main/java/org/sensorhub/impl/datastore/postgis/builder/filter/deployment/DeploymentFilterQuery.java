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
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.system.IDeploymentWithDesc;
import org.sensorhub.impl.datastore.postgis.builder.filter.feature.BaseFeatureFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.FilterQueryGenerator;

import java.util.stream.Collectors;

public abstract class DeploymentFilterQuery<F extends FilterQueryGenerator> extends BaseFeatureFilterQuery<IDeploymentWithDesc, DeploymentFilter,F> {

    public DeploymentFilterQuery(String tableName, F filterQueryGenerator) {
        super(tableName, filterQueryGenerator);
    }

    public F build(DeploymentFilter filter) {
        this.filterQueryGenerator = super.build(filter);
        this.handleSystemFilter(filter.getSystemFilter());
        return this.filterQueryGenerator;
    }

    protected abstract void handleSystemFilter(SystemFilter parentFilter);
}
