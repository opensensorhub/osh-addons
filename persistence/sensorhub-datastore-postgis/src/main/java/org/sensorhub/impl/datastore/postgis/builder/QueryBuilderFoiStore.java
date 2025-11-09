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

import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.impl.datastore.postgis.builder.query.feature.RemoveEntriesFoiQuery;
import org.sensorhub.impl.datastore.postgis.builder.query.feature.SelectEntriesFoiQuery;
import org.vast.ogc.gml.IFeature;

import java.util.Set;

public class QueryBuilderFoiStore extends QueryBuilderBaseFeatureStore<IFeature, IFoiStore.FoiField, FoiFilter> {
    public final static String FEATURE_TABLE_NAME = "foi";

    public QueryBuilderFoiStore() {
        super(FEATURE_TABLE_NAME);
    }

    public QueryBuilderFoiStore(String tableName) {
        super(tableName);
    }

    @Override
    public String createSelectEntriesQuery(FoiFilter filter, Set<IFoiStore.FoiField> fields) {
        SelectEntriesFoiQuery selectEntriesFoiQuery = new SelectEntriesFoiQuery.Builder()
                .tableName(this.getStoreTableName())
                .linkTo(this.systemStore)
                .linkTo(this.dataStreamStore)
                .linkTo(this.obsStore)
                .withFields(fields)
                .withFoiFilter(filter)
                .build();
        return selectEntriesFoiQuery.toQuery();
    }

    @Override
    public String createRemoveEntriesQuery(FoiFilter filter) {
        RemoveEntriesFoiQuery removeEntriesFoiQuery = new RemoveEntriesFoiQuery.Builder()
                .tableName(this.getStoreTableName())
                .linkTo(this.systemStore)
                .linkTo(this.dataStreamStore)
                .linkTo(this.obsStore)
                .withFoiFilter(filter)
                .build();
        return removeEntriesFoiQuery.toQuery();
    }
}
