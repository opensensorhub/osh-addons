/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.store.feature;

import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.feature.IFeatureStore;
import org.sensorhub.api.datastore.feature.IFeatureStoreBase;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderFeatureStore;
import org.sensorhub.impl.datastore.postgis.utils.SerializerUtils;
import org.vast.ogc.gml.IFeature;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class PostgisFeatureStoreImpl extends
        PostgisBaseFeatureStoreImpl<IFeature, IFeatureStoreBase.FeatureField, FeatureFilter, QueryBuilderFeatureStore> implements IFeatureStore {

    public PostgisFeatureStoreImpl(String url, String dbName, String login, String password,
                                   int idScope, IdProviderType dsIdProviderType, boolean useBatch) {
        super(url,dbName, login, password, idScope, dsIdProviderType, new QueryBuilderFeatureStore(), useBatch);
    }

    public PostgisFeatureStoreImpl(String url, String dbName, String login, String password, String dataStoreName,
                                   int idScope, IdProviderType dsIdProviderType, boolean useBatch) {
        super(url,dbName, login, password, idScope, dsIdProviderType, new QueryBuilderFeatureStore(dataStoreName), useBatch);
    }

    protected IFeature readFeature(String data) throws IOException {
        return SerializerUtils.readIFeatureFromJson(data);
    }
    protected  String writeFeature(IFeature feature) throws IOException {
        return SerializerUtils.writeIFeatureToJson(feature);
    }

    @Override
    public Set<Entry<FeatureKey, IFeature>> entrySet() {
        FeatureFilter featureFilter = new FeatureFilter.Builder().build();
        return this.selectEntries(featureFilter, new HashSet<>()).collect(Collectors.toSet());
    }
}
