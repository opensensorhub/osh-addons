/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.obs;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.postgresql.util.PGobject;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.DataStoreUtils;
import org.sensorhub.impl.datastore.obs.DataStreamInfoWrapper;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.PostgisStore;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderDataStreamStore;
import org.sensorhub.impl.datastore.postgis.utils.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Asserts;
import org.vast.util.TimeExtent;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostgisDataStreamStoreImpl extends PostgisStore<QueryBuilderDataStreamStore> implements IDataStreamStore {
    private volatile Cache<Long, IDataStreamInfo> cache = CacheBuilder.newBuilder()
            .maximumSize(150)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    private static final Logger logger = LoggerFactory.getLogger(PostgisDataStreamStoreImpl.class);
    private final Lock lock = new ReentrantLock();
    protected PostgisObsStoreImpl obsStore;
    protected ISystemDescStore systemStore;

    public PostgisDataStreamStoreImpl(PostgisObsStoreImpl obsStore, String url, String dbName, String login, String password,
                                      int idScope, IdProviderType dsIdProviderType, boolean useBatch) {
        super(idScope, dsIdProviderType, new QueryBuilderDataStreamStore(obsStore.getDatastoreName() + "_datastreams"), false);
        this.init(obsStore, url,dbName,login,password);
    }

    protected void init(PostgisObsStoreImpl obsStore, String url, String dbName, String login, String password) {
        this.obsStore = Asserts.checkNotNull(obsStore, PostgisObsStoreImpl.class);
        super.init(url, dbName, login, password, new String[]{
                queryBuilder.createTableQuery(),
                queryBuilder.createIndexQuery(),
                queryBuilder.createUniqueIndexQuery(),
                queryBuilder.createValidTimeBeginIndexQuery(),
                queryBuilder.createValidTimeEndIndexQuery(),
                queryBuilder.createTrigramExtensionQuery(),
                queryBuilder.createTrigramDescriptionFullTextIndexQuery()
        });
    }

    protected class DataStreamInfoWithTimeRanges extends DataStreamInfoWrapper
    {
        Long dsID;
        TimeExtent validTime;
        TimeExtent phenomenonTimeRange;
        TimeExtent resultTimeRange;

        DataStreamInfoWithTimeRanges(Long internalID, IDataStreamInfo dsInfo)
        {
            super(dsInfo);
            this.dsID = internalID;
        }

        @Override
        public TimeExtent getValidTime()
        {
            if (validTime == null)
            {
                validTime = super.getValidTime();

                // if valid time ends at now and there is a more recent version, compute the actual end time
                /*if (validTime.endsNow())
                {
                    var sysDsKey = new MVTimeSeriesSystemKey(
                            getSystemID().getInternalID().getIdAsLong(),
                            getOutputName(),
                            getValidTime().begin());

                    var nextKey = dataStreamBySystemIndex.lowerKey(sysDsKey); // use lower cause time sorting is reversed
                    if (nextKey != null &&
                            nextKey.systemID == sysDsKey.systemID &&
                            nextKey.signalName.equals(sysDsKey.signalName))
                        validTime = TimeExtent.period(validTime.begin(), Instant.ofEpochSecond(nextKey.validStartTime));
                }*/
            }

            return validTime;
        }

        @Override
        public TimeExtent getPhenomenonTimeRange()
        {
            if (phenomenonTimeRange == null)
                phenomenonTimeRange = obsStore.getDataStreamPhenomenonTimeRange(dsID);

            return phenomenonTimeRange;
        }

        @Override
        public TimeExtent getResultTimeRange()
        {
            if (resultTimeRange == null)
                resultTimeRange = obsStore.getDataStreamResultTimeRange(dsID);

            return resultTimeRange;
        }
    }

    @Override
    public Stream<Entry<DataStreamKey, IDataStreamInfo>> selectEntries(DataStreamFilter filter, Set<DataStreamInfoField> fields) {
        // build request
        String queryStr = queryBuilder.createSelectEntriesQuery(filter, fields);
        List<Entry<DataStreamKey, IDataStreamInfo>> results = new ArrayList<>();
        try (Connection connection = this.connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(queryStr)) {
                    while (resultSet.next()) {
                        long id = resultSet.getLong("id");
                        String data = resultSet.getString("data");
                        IDataStreamInfo dataStreamInfo = SerializerUtils.readIDataStreamInfoFromJson(data);
                        IDataStreamInfo wrapper = new DataStreamInfoWithTimeRanges(id, dataStreamInfo);
                        results.add(Map.entry(new DataStreamKey(obsStore.idScope, id), wrapper));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        logger.debug("{}, {}",queryStr, results.size());
        return results.stream();
    }

    @Override
    public void commit() {
        try {
            obsStore.commit();
        } catch (DataStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public void intersectsAndUpdate(IDataStreamInfo dsInfo) throws DataStoreException {
        // if DS already exists, compute the time intersection
        DataStreamFilter.Builder dataStreamFilterBuilder = new DataStreamFilter.Builder();
        if (dsInfo.getSystemID() != null) {
            dataStreamFilterBuilder = dataStreamFilterBuilder.withSystems(dsInfo.getSystemID().getInternalID());
        }
        if (dsInfo.getOutputName() != null) {
            dataStreamFilterBuilder = dataStreamFilterBuilder.withOutputNames(dsInfo.getOutputName());
        }

        if (dsInfo.getValidTime() != null) {
            dataStreamFilterBuilder = dataStreamFilterBuilder.withValidTime(new TemporalFilter.Builder()
                    .withLatestTime()
                    .build());
        } else {
            return;
        }

        DataStreamFilter dataStreamFilter = dataStreamFilterBuilder.build();
        Stream<Entry<DataStreamKey, IDataStreamInfo>> existingDsInfo = this.selectEntries(dataStreamFilter);
        Iterator<Entry<DataStreamKey, IDataStreamInfo>> ite = existingDsInfo.iterator();

        TimeExtent newValidTime = dsInfo.getValidTime();
        while (ite.hasNext()) {
            Entry<DataStreamKey, IDataStreamInfo> entry = ite.next();
            IDataStreamInfo currentDataStreamInfo = entry.getValue();

            TimeExtent dbValidTime = currentDataStreamInfo.getValidTime();

            TimeExtent resultIntersection;
            if (dbValidTime.endsNow() && !newValidTime.endsNow()) {
                if (newValidTime.begin().isBefore(dbValidTime.begin())) {
                    resultIntersection = TimeExtent.period(newValidTime.begin(), dbValidTime.begin());
                } else {
                    resultIntersection = TimeExtent.period(dbValidTime.begin(), newValidTime.begin());
                }
                this.updateTime(entry.getKey(), resultIntersection);
            } else if (newValidTime.endsNow() || dbValidTime.endsNow()) {
                // close dbValidTime at the start of newValidTime
                if (newValidTime.begin().isBefore(dbValidTime.begin())) {
                    throw new DataStoreException("Cannot overlap completely the validTime");
                }
                // otherwise, close the dbValidTime at the start of the new time
                resultIntersection = TimeExtent.period(dbValidTime.begin(), newValidTime.begin());
                this.updateTime(entry.getKey(), resultIntersection);
            }
        }
    }

    protected void updateTime(DataStreamKey dataStreamKey, TimeExtent timeExtent) {
        try (Connection connection = this.connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.updateValidTimeByIdQuery())) {
                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                jsonObject.setValue(SerializerUtils.writeTimeExtent(timeExtent));

                preparedStatement.setObject(1, jsonObject);
                preparedStatement.setLong(2, dataStreamKey.getInternalID().getIdAsLong());
                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    // update cache by invalidate old entry
                    cache.invalidate(dataStreamKey.getInternalID().getIdAsLong());
                } else {
                    throw new RuntimeException("Cannot update datastream ");
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DataStreamKey add(IDataStreamInfo dsInfo) throws DataStoreException {
        // RULE --
        // if DS already exists, compute the time intersection
        this.intersectsAndUpdate(dsInfo);

        // 2 cases, if exists, update time with intersection and insert the new one
        // otherwise only create it

        try (Connection connection = connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    queryBuilder.insertInfoQuery(), Statement.RETURN_GENERATED_KEYS)) {
                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                jsonObject.setValue(SerializerUtils.writeIDataStreamInfoToJson(dsInfo));

                preparedStatement.setObject(1, jsonObject);
                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
                        long generatedKey = 0;
                        if (rs.next()) {
                            generatedKey = rs.getLong(1);
                        }
                        return new DataStreamKey(BigId.fromLong(obsStore.idScope, generatedKey));
                    }
                } else {
                    throw new DataStoreException("Cannot insert dataStreamInfo " + dsInfo.getOutputName());
                }
            }
        } catch (SQLException | IOException e) {
            throw new DataStoreException("Cannot insert dataStreamInfo " + dsInfo.getOutputName(), e);
        }
    }

    @Override
    public IDataStreamInfo get(Object o) {
        if (!(o instanceof DataStreamKey)) {
            throw new UnsupportedOperationException("Get operation is not supported with argument != DataStream key, got=" + o.getClass());
        }
        DataStreamKey key = (DataStreamKey) o;
        IDataStreamInfo dataStreamInfo = cache.getIfPresent(key.getInternalID().getIdAsLong());
        if (dataStreamInfo == null) {
            lock.lock();
            try {
                // double lock checking + volatile
                if (dataStreamInfo == null) {
                    try (Connection connection = connectionManager.getConnection()) {
                        try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.selectByIdQuery())) {
                            preparedStatement.setLong(1, key.getInternalID().getIdAsLong());
                            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                                if (resultSet.next()) {
                                    String data = resultSet.getString("data");
                                    dataStreamInfo = SerializerUtils.readIDataStreamInfoFromJson(data);
                                    IDataStreamInfo wrapper = new DataStreamInfoWithTimeRanges(key.getInternalID().getIdAsLong(), dataStreamInfo);
                                    cache.put(key.getInternalID().getIdAsLong(), wrapper);
                                }
                            }
                        }
                    } catch (SQLException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return dataStreamInfo;
    }

    @Override
    public IDataStreamInfo put(DataStreamKey dataStreamKey, IDataStreamInfo iDataStreamInfo) {
        try (Connection connection = this.connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.updateByIdQuery())) {
                String json = SerializerUtils.writeIDataStreamInfoToJson(iDataStreamInfo);
                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                jsonObject.setValue(json);

                preparedStatement.setObject(1, jsonObject);
                preparedStatement.setLong(2, dataStreamKey.getInternalID().getIdAsLong());
                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    // update cache by invalidate old entry
                    cache.invalidate(dataStreamKey.getInternalID().getIdAsLong());
                    return iDataStreamInfo;
                } else {
                    throw new RuntimeException("Cannot update datastream ");
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IDataStreamInfo remove(Object o) {
        if (!(o instanceof DataStreamKey)) {
            throw new UnsupportedOperationException("Remove operation is not supported with argument != DataStreamKey key, got=" + o.getClass());
        }
        DataStreamKey key = (DataStreamKey) o;
        IDataStreamInfo data = this.get(key);

        logger.debug("Remove Feature with key={}", key.toString());
        // remove corresponding Obs
        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(key.getInternalID())
                .build();
        this.obsStore.removeEntries(filter);

        try (Connection connection = this.connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.removeByIdQuery())) {
                preparedStatement.setLong(1, key.getInternalID().getIdAsLong());
                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    cache.invalidate(key.getInternalID().getIdAsLong());
                    return data;
                } else {
                    throw new RuntimeException("Cannot remove IDataStreamInfo " + data.getOutputName());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clear() {
        try {
            super.clear();
            this.clearCache();
        } catch (Exception e) {
            throw e;
        } finally {
            cache.invalidateAll();
        }
    }

    public void clearCache() {
        cache.invalidateAll();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.get(key) != null;
    }


    @Override
    public Set<Map.Entry<DataStreamKey, IDataStreamInfo>> entrySet() {
        DataStreamFilter dataStreamFilter = new DataStreamFilter.Builder().build();
        return this.selectEntries(dataStreamFilter, new HashSet<>()).collect(Collectors.toSet());
    }

    public Collection<IDataStreamInfo> values() {
        return this.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toSet());
    }

    public Set<DataStreamKey> keySet() {
        return this.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    @Override
    public void linkTo(ISystemDescStore systemStore) {
        super.linkTo(systemStore);
    }

    public Set<Long> getDataStreamsIdsByTimeRange(Instant min, Instant max) {
        Set<Long> results = new LinkedHashSet<>();
        try (Connection connection = this.connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String queryStr = queryBuilder.getAllDataStreams(min, max);
                logger.debug(queryStr);
                try (ResultSet resultSet = statement.executeQuery(queryStr)) {
                    while (resultSet.next())
                        results.add(resultSet.getLong("id"));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return results;
    }

    @Override
    protected void initUidHashIdProvider() {
        idProvider = DataStoreUtils.getDataStreamHashIdProvider(741532149);
    }

}
