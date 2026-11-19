/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder.query.deployment;

import org.sensorhub.api.datastore.deployment.DeploymentFilter;
import org.sensorhub.impl.datastore.postgis.builder.filter.deployment.DeploymentFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.filter.deployment.RemoveDeploymentFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.generator.RemoveFilterQueryGenerator;
import org.sensorhub.impl.datastore.postgis.builder.query.RemoveEntriesQuery;

public class RemoveEntriesDeploymentQuery extends RemoveEntriesQuery {
    protected static abstract class Init<T extends RemoveEntriesDeploymentQuery.Init<T>> extends RemoveEntriesQuery.Init<T> {

        public T withDeploymentFilter(DeploymentFilter filter) {
            if(filter != null) {
                RemoveDeploymentFilterQuery filterQuery = new RemoveDeploymentFilterQuery(this.tableName, filterQueryGenerator);
                filterQueryGenerator = filterQuery.build(filter);
            }
            return self();
        }

        public RemoveEntriesDeploymentQuery build() {
            return new RemoveEntriesDeploymentQuery(this);
        }
    }

    public static class Builder extends RemoveEntriesDeploymentQuery.Init<RemoveEntriesDeploymentQuery.Builder> {
        @Override
        protected RemoveEntriesDeploymentQuery.Builder self() {
            return this;
        }
    }

    protected RemoveEntriesDeploymentQuery(RemoveEntriesDeploymentQuery.Init<?> init) {
        super(init);
    }
}
