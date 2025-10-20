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

import org.sensorhub.api.database.IFeatureDatabase;
import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.feature.IFeatureStoreBase;
import org.sensorhub.impl.datastore.postgis.builder.filter.SelectEntriesFeatureQuery;
import org.vast.ogc.gml.IFeature;

import java.util.Set;

import static org.sensorhub.api.datastore.feature.IFeatureStoreBase.FeatureField.GEOMETRY;
import static org.sensorhub.api.datastore.feature.IFeatureStoreBase.FeatureField.VALID_TIME;

public class QueryBuilderFeatureStore extends QueryBuilderBaseFeatureStore<IFeature, IFeatureStoreBase.FeatureField, FeatureFilter> {
    public final static String FEATURE_TABLE_NAME = "feature";

    public QueryBuilderFeatureStore() {
        super(FEATURE_TABLE_NAME);
    }

    public QueryBuilderFeatureStore(String tableName) {
        super(tableName);
    }

    @Override
    public String createSelectEntriesQuery(FeatureFilter filter, Set<IFeatureStoreBase.FeatureField> fields) {
        SelectEntriesFeatureQuery selectEntriesFeatureQuery = new SelectEntriesFeatureQuery.Builder()
                .tableName(this.getStoreTableName())
                .withFields(fields)
                .withFeatureFilter(filter)
                .build();
        return selectEntriesFeatureQuery.toQuery();
    }
}
