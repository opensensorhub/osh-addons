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

import org.postgresql.util.PGobject;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.feature.IFeatureStore;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.impl.datastore.DataStoreUtils;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.builder.FoiIteratorResultSet;
import org.sensorhub.impl.datastore.postgis.builder.IteratorResultSet;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderFoiStore;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.sensorhub.impl.datastore.postgis.utils.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ogc.gml.IFeature;
import org.vast.util.Asserts;

import java.io.IOException;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.sensorhub.api.datastore.feature.IFeatureStoreBase.FeatureField.VALID_TIME;


public class PostgisFoiStoreImpl extends
        PostgisBaseFeatureStoreImpl<IFeature, IFoiStore.FoiField, FoiFilter, QueryBuilderFoiStore> implements IFoiStore {
    private static final Logger logger = LoggerFactory.getLogger(PostgisFoiStoreImpl.class);

    ISystemDescStore systemStore;

    public PostgisFoiStoreImpl(String url, String dbName, String login, String password,
                               int idScope, IdProviderType dsIdProviderType, boolean useBatch) {
        super(url,dbName, login, password, idScope, dsIdProviderType, new QueryBuilderFoiStore(), useBatch);
    }

    public PostgisFoiStoreImpl(String url, String dbName, String login, String password, String dataStoreName,
                               int idScope, IdProviderType dsIdProviderType, boolean useBatch) {
        super(url,dbName, login, password, idScope, dsIdProviderType, new QueryBuilderFoiStore(dataStoreName), useBatch);
    }

    @Override
    protected IFeature readFeature(String data) throws IOException {
        return SerializerUtils.readIFeatureFromJson(data);
    }
    @Override
    protected  String writeFeature(IFeature feature) throws IOException {
        return SerializerUtils.writeIFeatureToJson(feature);
    }

    @Override
    public Set<Entry<FeatureKey, IFeature>> entrySet() {
        FoiFilter filter = new FoiFilter.Builder().build();
        return this.selectEntries(filter, new HashSet<>()).collect(Collectors.toSet());
    }

    //DEBUG
    @Override
    public PostgisFeatureKey getCurrentVersionKey(BigId internalID)
    {
        var e = getCurrentVersionEntry(internalID);
        return e != null ? (PostgisFeatureKey)e.getKey() : null;
    }

    @Override
    protected void checkParentFeatureExists(BigId parentID) throws DataStoreException {
        if (systemStore != null)
            DataStoreUtils.checkParentFeatureExists(parentID, systemStore, this);
        else
            DataStoreUtils.checkParentFeatureExists(parentID, this);
    }

    @Override
    public void linkTo(ISystemDescStore systemStore) {
        this.systemStore = Asserts.checkNotNull(systemStore, ISystemDescStore.class);
    }

    @Override
    public void linkTo(IObsStore obsStore) {
        super.linkTo(obsStore);
    }

    @Override
    public void linkTo(IFeatureStore featureStore) {
        super.linkTo(featureStore);
    }


    @Override
    public long countMatchingEntries(FoiFilter filter) {
        var foiFilter = FoiFilter.Builder.from(filter)
                .withLimit(1)
                .build();
        return selectEntries(foiFilter).count();
    }

    public Stream<Entry<FeatureKey, IFeature>> selectEntries(FoiFilter filter, Set<IFoiStore.FoiField> fields) {
        String queryStr = queryBuilder.createSelectEntriesQuery(filter, fields);
        FoiIteratorResultSet<Entry<FeatureKey, IFeature>> iteratorResultSet =
                new FoiIteratorResultSet<>(
                        queryStr,
                        queryBuilder.getStoreTableName(),
                        connectionManager,
                        STREAM_FETCH_SIZE,
                        filter.getLimit(),
                        (resultSet) -> resultSetToEntry(resultSet, fields),
                        (entry) -> (filter.getValuePredicate() == null || filter.getValuePredicate().test(entry.getValue())),
                        filter);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iteratorResultSet, Spliterator.ORDERED), false);
    }
}
