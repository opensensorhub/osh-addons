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
import org.sensorhub.impl.datastore.postgis.builder.filter.deployment.SelectDeploymentFilterQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.SelectEntriesQuery;

public class SelectEntriesDeploymentQuery extends SelectEntriesQuery {
    protected static abstract class Init<T extends SelectEntriesDeploymentQuery.Init<T>> extends SelectEntriesQuery.Init<T> {

        public T withDeploymentFilter(DeploymentFilter filter) {
            if(filter != null) {
                SelectDeploymentFilterQuery filterQuery = new SelectDeploymentFilterQuery(this.tableName, filterQueryGenerator);
                filterQueryGenerator = filterQuery.build(filter);
            }
            return self();
        }

        public SelectEntriesDeploymentQuery build() {
            return new SelectEntriesDeploymentQuery(this);
        }
    }

    public static class Builder extends SelectEntriesDeploymentQuery.Init<SelectEntriesDeploymentQuery.Builder> {
        @Override
        protected SelectEntriesDeploymentQuery.Builder self() {
            return this;
        }
    }

    protected SelectEntriesDeploymentQuery(SelectEntriesDeploymentQuery.Init<?> init) {
        super(init);
    }
}
