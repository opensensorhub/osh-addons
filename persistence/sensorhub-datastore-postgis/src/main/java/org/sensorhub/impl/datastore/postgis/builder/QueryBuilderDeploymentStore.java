/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.builder;

import org.sensorhub.api.datastore.deployment.DeploymentFilter;
import org.sensorhub.api.datastore.deployment.IDeploymentStore;
import org.sensorhub.api.system.IDeploymentWithDesc;
import org.sensorhub.impl.datastore.postgis.builder.query.deployment.RemoveEntriesDeploymentQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.deployment.SelectEntriesDeploymentQuery;

import java.util.Set;

public class QueryBuilderDeploymentStore extends QueryBuilderBaseFeatureStore<IDeploymentWithDesc, IDeploymentStore.DeploymentField, DeploymentFilter> {
    public final static String FEATURE_TABLE_NAME = "deployment";

    public QueryBuilderDeploymentStore() {
        super(FEATURE_TABLE_NAME);
    }

    public QueryBuilderDeploymentStore(String tableName) {
        super(tableName);
    }

    @Override
    public String createSelectEntriesQuery(DeploymentFilter filter, Set<IDeploymentStore.DeploymentField> fields) {
        SelectEntriesDeploymentQuery selectEntriesDeploymentQuery = new SelectEntriesDeploymentQuery.Builder()
                .tableName(this.getStoreTableName())
                .withFields(fields)
                .withDeploymentFilter(filter)
                .build();
        return selectEntriesDeploymentQuery.toQuery();
    }

    @Override
    public String createRemoveEntriesQuery(DeploymentFilter filter) {
        RemoveEntriesDeploymentQuery removeEntriesDeploymentQuery = new RemoveEntriesDeploymentQuery.Builder()
                .tableName(this.getStoreTableName())
                .withDeploymentFilter(filter)
                .build();
        return removeEntriesDeploymentQuery.toQuery();
    }
}
