/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.store.obs;

import com.google.common.collect.Range;
import net.opengis.swe.v20.DataBlock;
import org.postgresql.util.PGobject;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.*;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.builder.IteratorResultSet;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderObsStore;
import org.sensorhub.impl.datastore.postgis.store.PostgisStore;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.sensorhub.impl.datastore.postgis.utils.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockByte;
import org.vast.util.TimeExtent;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.sensorhub.api.datastore.obs.IObsStore.ObsField.*;

public class PostgisObsStoreImpl extends PostgisStore<QueryBuilderObsStore> implements IObsStore {
    private static final Logger logger = LoggerFactory.getLogger(PostgisObsStoreImpl.class);
    public static final int STREAM_FETCH_SIZE = 20_000;
    public static final String DEFAULT_TABLE_NAME = QueryBuilderObsStore.OBS_STORE_TABLE_NAME;

    protected PostgisDataStreamStoreImpl dataStreamStore;
    protected ISystemDescStore systemDescStore;
    protected IFoiStore foiStore;

    public PostgisObsStoreImpl(String url, String dbName, String login, String password, int idScope, IdProviderType dsIdProviderType) {
        this(url,dbName,login,password,DEFAULT_TABLE_NAME,idScope,dsIdProviderType);
    }

    public PostgisObsStoreImpl(String url, String dbName, String login, String password, String dataStoreName,
                               int idScope, IdProviderType dsIdProviderType) {
        this(url, dbName,login,password,dataStoreName,idScope,dsIdProviderType,new QueryBuilderObsStore(dataStoreName));
    }

    public PostgisObsStoreImpl(String url, String dbName, String login, String password, String dataStoreName,
                               int idScope, IdProviderType dsIdProviderType,QueryBuilderObsStore queryBuilderObsStore) {
        super(idScope, dsIdProviderType,
                queryBuilderObsStore,
                false);
        this.init(url, dbName, login, password, new String[]{
                        queryBuilder.createTableQuery(),
//                        queryBuilder.createDataIndexQuery(),
                        queryBuilder.createDataStreamIndexQuery(),
                        queryBuilder.createPhenomenonTimeIndexQuery(),
                        queryBuilder.createResultTimeIndexQuery(),
                        queryBuilder.createFoiIndexQuery(),
                        queryBuilder.createUniqueConstraint()
                }
        );
    }

    @Override
    protected void init(String url, String dbName, String login, String password, String[] initScripts) {
        super.init(url, dbName, login, password, initScripts);
        this.dataStreamStore = new PostgisDataStreamStoreImpl(this, url, dbName, login, password, idScope, idProviderType, false);
        queryBuilder.linkTo(dataStreamStore);
    }

    @Override
    public Stream<Entry<BigId, IObsData>> selectEntries(ObsFilter filter, Set<ObsField> fields) {
        Set<ObsField> hashSet;

        if (fields != null) {
            hashSet = new HashSet<>(fields);
            hashSet.add(PHENOMENON_TIME);
        } else {
            hashSet = null;
        }

        String queryStr = queryBuilder.createSelectEntriesQuery(filter, hashSet);
        if(logger.isDebugEnabled()) {
            logger.debug(queryStr);
        }
        IteratorResultSet<Entry<BigId, IObsData>> iteratorResultSet =
                new IteratorResultSet<>(
                        queryStr,
                        connectionManager,
                        STREAM_FETCH_SIZE,
                        (resultSet) -> resultSetToEntry(resultSet, fields),
                        (entry) -> (filter.getValuePredicate() == null || filter.getValuePredicate().test(entry.getValue())));
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iteratorResultSet, Spliterator.ORDERED), false);
    }

    private Entry<BigId, IObsData> resultSetToEntry(ResultSet resultSet, Set<ObsField> fields) {
        try {
            BigId id = BigId.fromLong(idScope, resultSet.getLong("id"));
            ObsData.Builder obsDataBuilder = new ObsData.Builder();
            ObsData obsData = null;

            boolean noFields = fields != null && !fields.isEmpty();
            BigId dataStreamId = null;
            if (!noFields || fields.contains(DATASTREAM_ID)) {
                long dataStreamIdAsLong = resultSet.getLong(String.valueOf(DATASTREAM_ID));
                if (!resultSet.wasNull()) {
                    dataStreamId = BigId.fromLong(idScope, dataStreamIdAsLong);
                    obsDataBuilder = obsDataBuilder.withDataStream(dataStreamId);
                }
            }
            if (!noFields || fields.contains(FOI_ID)) {
                long foiId = resultSet.getLong(String.valueOf(FOI_ID));
                if (!resultSet.wasNull() && foiId > 0) {
                    obsDataBuilder = obsDataBuilder.withFoi(BigId.fromLong(idScope, foiId));
                }
            }
            // required
            Timestamp phenomenonTimestamp = resultSet.getTimestamp(String.valueOf(PHENOMENON_TIME), UTC_LOCAL);
            if (!resultSet.wasNull()) {
                obsDataBuilder = obsDataBuilder.withPhenomenonTime(phenomenonTimestamp.toInstant());
            }

            if (!noFields || fields.contains(RESULT_TIME)) {
                Timestamp resultTimestamp = resultSet.getTimestamp(String.valueOf(RESULT_TIME), UTC_LOCAL);
                if (!resultSet.wasNull()) {
                    obsDataBuilder = obsDataBuilder.withResultTime(resultTimestamp.toInstant());
                }
            }

            // get datastream schema from datastreamId
            if (dataStreamId != null) {
                IDataStreamInfo dataStreamInfo = dataStreamStore.get(new DataStreamKey(dataStreamId));
                DataBlock dataBlock = SerializerUtils.readDataBlockFromJson(dataStreamInfo.getRecordStructure(), resultSet.getString(String.valueOf(RESULT)));
                obsData = obsDataBuilder.withResult(dataBlock).build();
            } else {
                // fake result
                obsData = obsDataBuilder.withResult(new DataBlockByte()).build();
            }

            return Map.entry(id, obsData);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot parse resultSet to IObs", ex);
        }
    }

    @Override
    public IDataStreamStore getDataStreams() {
        return this.dataStreamStore;
    }

    protected void fillAddStatement(DataStreamKey dataStreamKey, IObsData obs, PreparedStatement preparedStatement) throws SQLException {
        // insert DataStreamId
        preparedStatement.setLong(1, dataStreamKey.getInternalID().getIdAsLong());
        // insert foiId if any
        if (obs.hasFoi()) {
            preparedStatement.setLong(2, obs.getFoiID().getIdAsLong());
        } else {
            preparedStatement.setNull(2, Types.BIGINT);
        }

        // insert timestamp
        if (obs.getPhenomenonTime() != null) {
            preparedStatement.setTimestamp(3, Timestamp.from(obs.getPhenomenonTime()),UTC_LOCAL);
        } else {
            preparedStatement.setNull(3, Types.TIMESTAMP_WITH_TIMEZONE);
        }

        if (obs.getResultTime() != null) {
            preparedStatement.setTimestamp(4, Timestamp.from(obs.getResultTime()),UTC_LOCAL);
        } else {
            preparedStatement.setNull(4, Types.TIMESTAMP_WITH_TIMEZONE);
        }
        // insert DataBlock
        PGobject jsonObject = new PGobject();
        jsonObject.setType("json");
        IDataStreamInfo dataStreamInfo = dataStreamStore.get(new DataStreamKey(obs.getDataStreamID()));
        jsonObject.setValue(SerializerUtils.writeDataBlockToJson(dataStreamInfo.getRecordStructure(),
                dataStreamInfo.getRecordEncoding(), obs.getResult()));
        preparedStatement.setObject(5, jsonObject);
    }

    @Override
    public BigId add(IObsData obs) {
        DataStreamKey dataStreamKey = new DataStreamKey(obs.getDataStreamID());
        if (!dataStreamStore.containsKey(dataStreamKey))
            throw new IllegalStateException("Unknown datastream with ID: " + obs.getDataStreamID().getIdAsLong());
        // check that FOI exists
//        if (obs.hasFoi() && foiStore != null && foiStore.contains(obs.getFoiID()))
//            throw new IllegalStateException("Unknown FOI: " + obs.getFoiID());
        Instant dataTime = obs.getPhenomenonTime();
        // check existing partition
        try (Connection connection1 = this.connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection1.prepareStatement(queryBuilder.insertObsQuery(), Statement.RETURN_GENERATED_KEYS)) {
                this.fillAddStatement(dataStreamKey, obs, preparedStatement);
                int rows = preparedStatement.executeUpdate();
                try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
                    long generatedKey = 0;
                    if (rs.next()) {
                        generatedKey = rs.getLong(1);
                    }
                    return BigId.fromLong(idScope, generatedKey);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Cannot insert obs", e);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot insert obs", e);
        }
    }


    static class TimeParams {
        Range<Instant> phenomenonTimeRange;
        Range<Instant> resultTimeRange;
        boolean currentTimeOnly;
        boolean latestResultOnly;


        TimeParams(ObsFilter filter) {
            // get phenomenon time range
            phenomenonTimeRange = filter.getPhenomenonTime() != null ?
                    filter.getPhenomenonTime().getRange() : PostgisUtils.ALL_TIMES_RANGE;

            // get result time range
            resultTimeRange = filter.getResultTime() != null ?
                    filter.getResultTime().getRange() : PostgisUtils.ALL_TIMES_RANGE;

            latestResultOnly = filter.getResultTime() != null && filter.getResultTime().isLatestTime();
            currentTimeOnly = filter.getPhenomenonTime() != null && filter.getPhenomenonTime().isCurrentTime();
        }
    }

    @Override
    public Stream<ObsStats> getStatistics(ObsStatsQuery query) {
        var filter = query.getObsFilter();
        var timeParams = new TimeParams(filter);

        Set<Long> dsIds = dataStreamStore.getDataStreamsIdsByTimeRange(timeParams.phenomenonTimeRange.lowerEndpoint(),
                timeParams.phenomenonTimeRange.upperEndpoint());
        return dsIds.stream().map((dsId) -> {
            try (Connection connection = this.connectionManager.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    String queryStr = queryBuilder.statsQueryByDataStream(query, dsId);
                    logger.debug(queryStr);
                    try (ResultSet resultSet = statement.executeQuery(queryStr)) {
                        long start = timeParams.phenomenonTimeRange.lowerEndpoint().getEpochSecond();
                        long end = timeParams.phenomenonTimeRange.upperEndpoint().getEpochSecond();
                        long t = start;
                        long dt = query.getHistogramBinSize().getSeconds();
                        int numBins = (int) Math.ceil((double) (end - start) / dt);
                        int[] counts = new int[numBins];

                        while (resultSet.next()) {
                            int count = resultSet.getInt("count");
                            long intervalAliasSeconds = resultSet.getLong("interval_alias");
                            int binIdx = (int) Math.ceil((intervalAliasSeconds - t) / query.getHistogramBinSize().getSeconds());
                            counts[binIdx] = count;
                        }
                        return new ObsStats.Builder()
                                .withDataStreamID(BigId.fromLong(idScope, dsId))
//                            .withFoiID(foiID)
                                .withObsCountByTime(counts)
                                .withPhenomenonTimeRange(
                                        TimeExtent.period(
                                                Range.closed(Instant.parse("1000-01-01T00:00:00Z"), Instant.parse("3000-01-01T00:00:00Z"))
                                        ))
                                .withResultTimeRange(TimeExtent.period(
                                        Range.closed(Instant.parse("1000-01-01T00:00:00Z"), Instant.parse("3000-01-01T00:00:00Z"))
                                ))
                                .withTotalObsCount(countMatchingEntries(filter))
                                .build();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void linkTo(IFoiStore foiStore) {
        this.foiStore = foiStore;
        super.linkTo(foiStore);
    }

    public void linkTo(ISystemDescStore systemDescStore) {
        this.systemDescStore = systemDescStore;
        super.linkTo(systemDescStore);
    }

    @Override
    public IObsData get(Object o) {
        if (!(o instanceof BigId)) {
            throw new UnsupportedOperationException("Get operation is not supported with argument != BigId key, got=" + o.getClass());
        }
        BigId key = (BigId) o;
        try (Connection connection = this.connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.selectByIdQuery())) {
                preparedStatement.setLong(1, key.getIdAsLong());
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    ObsData.Builder obsDataBuilder = new ObsData.Builder();

                    BigId dataStreamId = BigId.fromLong(idScope, resultSet.getLong(String.valueOf(DATASTREAM_ID)));
                    obsDataBuilder = obsDataBuilder.withDataStream(dataStreamId);

                    long foiId = resultSet.getLong(String.valueOf(FOI_ID));
                    if (!resultSet.wasNull()) {
                        obsDataBuilder = obsDataBuilder.withFoi(BigId.fromLong(idScope, foiId));
                    }

                    Timestamp phenomenonTimestamp = resultSet.getTimestamp(String.valueOf(PHENOMENON_TIME), UTC_LOCAL);
                    if (!resultSet.wasNull()) {
                        obsDataBuilder = obsDataBuilder.withPhenomenonTime(phenomenonTimestamp.toInstant());
                    }
                    Timestamp resultTimestamp = resultSet.getTimestamp(String.valueOf(RESULT_TIME), UTC_LOCAL);
                    if (!resultSet.wasNull()) {
                        obsDataBuilder = obsDataBuilder.withResultTime(resultTimestamp.toInstant());
                    }

                    // get datastream schema from datastreamId
                    IDataStreamInfo dataStreamInfo = dataStreamStore.get(new DataStreamKey(dataStreamId));
                    DataBlock dataBlock = SerializerUtils.readDataBlockFromJson(dataStreamInfo.getRecordStructure(), resultSet.getString(String.valueOf(RESULT)));
                    return obsDataBuilder.withResult(dataBlock).build();
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IObsData put(BigId key, IObsData iObsData) {
        IObsData oldObs = this.get(key);
        if (oldObs == null)
            throw new UnsupportedOperationException("put can only be used to update existing entries");

        try (Connection connection = this.connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.updateByIdQuery())) {
                preparedStatement.setLong(1, iObsData.getDataStreamID().getIdAsLong());
                preparedStatement.setLong(2, iObsData.getFoiID().getIdAsLong());

                if (iObsData.getPhenomenonTime() != null) {
                    preparedStatement.setTimestamp(3, Timestamp.from(iObsData.getPhenomenonTime()), UTC_LOCAL);
                } else {
                    preparedStatement.setNull(3, Types.TIMESTAMP_WITH_TIMEZONE);
                }

                if (iObsData.getResultTime() != null) {
                    preparedStatement.setTimestamp(4, Timestamp.from(iObsData.getResultTime()),UTC_LOCAL);
                } else {
                    preparedStatement.setNull(4, Types.TIMESTAMP_WITH_TIMEZONE);
                }

                // insert DataBlock
                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                IDataStreamInfo dataStreamInfo = dataStreamStore.get(new DataStreamKey(iObsData.getDataStreamID()));
                jsonObject.setValue(SerializerUtils.writeDataBlockToJson(dataStreamInfo.getRecordStructure(),
                        dataStreamInfo.getRecordEncoding(), iObsData.getResult()));

                preparedStatement.setObject(5, jsonObject);
                preparedStatement.setLong(6, key.getIdAsLong());

                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    return iObsData;
                } else {
                    throw new RuntimeException("Cannot insert obs ");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IObsData remove(Object o) {
        if (!(o instanceof BigId)) {
            throw new UnsupportedOperationException("Remove operation is not supported with argument != BigId key, got=" + o.getClass());
        }
        BigId key = (BigId) o;
        IObsData data = this.get(o);

        logger.debug("Remove Obs with key={}", key);
        try (Connection connection = this.connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.removeByIdQuery())) {
                preparedStatement.setLong(1, key.getIdAsLong());
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    TimeExtent getDataStreamPhenomenonTimeRange(long dataStreamID) {
        Instant[] timeRange = new Instant[]{Instant.MAX, Instant.MIN};
        try (Connection connection = this.connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(queryBuilder.getPhenomenonTimeRangeByDataStreamIdQuery(dataStreamID))) {
                    if (resultSet.next()) {
                        Timestamp min = resultSet.getTimestamp("min");
                        Timestamp max = resultSet.getTimestamp("max");
                        if (min != null) {
                            timeRange[0] = min.toInstant();
                        }
                        if (max != null) {
                            timeRange[1] = max.toInstant();
                        }

                        if (timeRange[0] == Instant.MAX || timeRange[1] == Instant.MIN)
                            return null;
                        else
                            return TimeExtent.period(timeRange[0], timeRange[1]);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    TimeExtent getDataStreamResultTimeRange(long dataStreamID) {
        Instant[] timeRange = new Instant[]{Instant.MAX, Instant.MIN};
        try (Connection connection = this.connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = queryBuilder.getResultTimeRangeByDataStreamIdQuery(dataStreamID);
                logger.debug(query);
                try (ResultSet resultSet = statement.executeQuery(query)) {
                    while (resultSet.next()) {
                        Timestamp min = resultSet.getTimestamp("min");
                        Timestamp max = resultSet.getTimestamp("max");
                        if (min != null) {
                            timeRange[0] = min.toInstant();
                        }
                        if (max != null) {
                            timeRange[1] = max.toInstant();
                        }

                        if (timeRange[0] == Instant.MAX || timeRange[1] == Instant.MIN)
                            return null;
                        else
                            return TimeExtent.period(timeRange[0], timeRange[1]);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public long countMatchingEntries(ObsFilter filter) {
        try (Connection connection = this.connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String query = queryBuilder.createSelectEntriesCountQuery(filter);
                logger.debug(query);
                try (ResultSet resultSet = statement.executeQuery(query)) {
                    if (resultSet.next()) {
                        return resultSet.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    @Override
    public long removeEntries(ObsFilter filter) {
        logger.debug("Remove Feature with filter={}", filter.toString());
        String queryStr = queryBuilder.createRemoveEntriesQuery(filter);
        System.out.println(queryStr);
        if(logger.isDebugEnabled()) {
            logger.debug(queryStr);
        }
        try (Connection connection = this.connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                return statement.executeUpdate(queryStr);
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
