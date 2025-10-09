/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.util.PGobject;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.command.CommandFilter;
import org.sensorhub.api.datastore.command.CommandStreamFilter;
import org.sensorhub.api.datastore.command.CommandStreamKey;
import org.sensorhub.api.datastore.command.ICommandStreamStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.PostgisStore;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderCommandStreamStore;
import org.sensorhub.impl.datastore.postgis.utils.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Asserts;
import org.vast.util.TimeExtent;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostgisCommandStreamStoreImpl extends PostgisStore<QueryBuilderCommandStreamStore> implements ICommandStreamStore {
    private volatile Cache<Long, ICommandStreamInfo> cache = CacheBuilder.newBuilder()
            .maximumSize(150)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();
    private static final Logger logger = LoggerFactory.getLogger(PostgisCommandStreamStoreImpl.class);
    private final Lock lock = new ReentrantLock();

    private PostgisCommandStoreImpl cmdStore;

    protected ISystemDescStore systemStore;

    public PostgisCommandStreamStoreImpl(PostgisCommandStoreImpl cmdStore, HikariDataSource connection, boolean useBatch) {
        super(cmdStore.idScope, cmdStore.idProviderType, new QueryBuilderCommandStreamStore(cmdStore.getDatastoreName() + "_commandstreams"), useBatch);
        this.init(cmdStore, connection);
    }

    protected void init(PostgisCommandStoreImpl cmdStore, HikariDataSource hikariDataSource) {
        this.cmdStore = Asserts.checkNotNull(cmdStore, PostgisCommandStoreImpl.class);
        this.hikariDataSource = Asserts.checkNotNull(hikariDataSource, HikariDataSource.class);
        try(Connection connection = this.hikariDataSource.getConnection()) {
            if (!PostgisUtils.checkTable(connection, queryBuilder.getStoreTableName())) {
                // create table
                PostgisUtils.executeQueries(connection, new String[]{
                        queryBuilder.createTableQuery(),
                        queryBuilder.createIndexQuery(),
                        queryBuilder.createUniqueIndexQuery(),
                        queryBuilder.createValidTimeBeginIndexQuery(),
                        queryBuilder.createValidTimeEndIndexQuery(),
                        queryBuilder.createTrigramExtensionQuery(),
                        queryBuilder.createTrigramDescriptionFullTextIndexQuery(),
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized CommandStreamKey add(ICommandStreamInfo csInfo) throws DataStoreException {
        // RULE --
        // if CMD already exists, compute the time intersection
        this.intersectsAndUpdate(csInfo);

        try (Connection connection = hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    queryBuilder.insertCommandQuery(), Statement.RETURN_GENERATED_KEYS)) {
                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                jsonObject.setValue(SerializerUtils.writeICommandStreamInfoToJson(csInfo));

                preparedStatement.setObject(1, jsonObject);
                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
                        long generatedKey = 0;
                        if (rs.next()) {
                            generatedKey = rs.getLong(1);
                        }
                        return new CommandStreamKey(idScope, generatedKey);
                    }
                } else {
                    throw new DataStoreException("Cannot insert command " + csInfo.getName());
                }
            }
        } catch (SQLException | IOException e) {
            throw new DataStoreException("Cannot insert command " + csInfo.getName());
        }
    }

    public void intersectsAndUpdate(ICommandStreamInfo cmdInfo) throws DataStoreException {
        // if DS already exists, compute the time intersection
        CommandStreamFilter.Builder commandStreamFilterBuilder = new CommandStreamFilter.Builder();
        if (cmdInfo.getSystemID() != null) {
            commandStreamFilterBuilder = commandStreamFilterBuilder.withSystems(cmdInfo.getSystemID().getInternalID());
        }
        if (cmdInfo.getControlInputName() != null) {
            commandStreamFilterBuilder = commandStreamFilterBuilder.withControlInputNames(cmdInfo.getControlInputName());
        }

        if (cmdInfo.getValidTime() != null) {
            commandStreamFilterBuilder = commandStreamFilterBuilder.withValidTime(new TemporalFilter.Builder()
                    .withLatestTime()
                    .build());
        } else {
            return;
        }

        CommandStreamFilter commandStreamFilter = commandStreamFilterBuilder.build();
        Stream<Entry<CommandStreamKey, ICommandStreamInfo>> existingCmdInfo = this.selectEntries(commandStreamFilter);
        Iterator<Entry<CommandStreamKey, ICommandStreamInfo>> ite = existingCmdInfo.iterator();

        TimeExtent newValidTime = cmdInfo.getValidTime();
        while (ite.hasNext()) {
            Entry<CommandStreamKey, ICommandStreamInfo> entry = ite.next();
            ICommandStreamInfo currentCmdStreamInfo = entry.getValue();

            TimeExtent dbValidTime = currentCmdStreamInfo.getValidTime();

            TimeExtent resultIntersection;
            if(dbValidTime.endsNow() && !newValidTime.endsNow()) {
//                throw new DataStoreException("Existing datastream already exists at this valid Time");
                if(newValidTime.begin().isBefore(dbValidTime.begin())) {
                    resultIntersection = TimeExtent.period(newValidTime.begin(), dbValidTime.begin());
                } else {
                    resultIntersection = TimeExtent.period(dbValidTime.begin(), newValidTime.begin());
                }
                this.updateTime(entry.getKey(), resultIntersection);
            } else if(newValidTime.endsNow() || dbValidTime.endsNow()) {
                // close dbValidTime at the start of newValidTime
                if(newValidTime.begin().isBefore(dbValidTime.begin())) {
                    throw new DataStoreException("Cannot overlap completely the validTime");
                }
                // otherwise, close the dbValidTime at the start of the new time
                resultIntersection = TimeExtent.period(dbValidTime.begin(), newValidTime.begin());
                this.updateTime(entry.getKey(), resultIntersection);
            }
        }
    }

    protected void updateTime(CommandStreamKey cmdStreamKey, TimeExtent timeExtent) {
        try(Connection connection = this.hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.updateValidTimeByIdQuery())) {
                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                jsonObject.setValue(SerializerUtils.writeTimeExtent(timeExtent));

                preparedStatement.setObject(1, jsonObject);
                preparedStatement.setLong(2, cmdStreamKey.getInternalID().getIdAsLong());
                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    // update cache by invalidate old entry
                    cache.invalidate(cmdStreamKey.getInternalID().getIdAsLong());
                } else {
                    throw new RuntimeException("Cannot update commandstream");
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ICommandStreamInfo get(Object o) {
        if (!(o instanceof CommandStreamKey)) {
            throw new UnsupportedOperationException("Get operation is not supported with argument != CommandStreamKey key, got=" + o.getClass());
        }
        CommandStreamKey key = (CommandStreamKey) o;
        ICommandStreamInfo commandStreamInfo = cache.getIfPresent(key.getInternalID().getIdAsLong());
        if (commandStreamInfo == null) {
            lock.lock();
            try {
                // double lock checking + volatile
                if (commandStreamInfo == null) {
                    try (Connection connection = hikariDataSource.getConnection()) {
                        try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.selectByIdQuery())) {
                            preparedStatement.setLong(1, key.getInternalID().getIdAsLong());
                            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                                if (resultSet.next()) {
                                    String data = resultSet.getString("data");
                                    commandStreamInfo = SerializerUtils.readICommandStreamInfoFromJson(data);
                                    cache.put(key.getInternalID().getIdAsLong(), commandStreamInfo);
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
        return commandStreamInfo;
    }

    @Override
    public Stream<Entry<CommandStreamKey, ICommandStreamInfo>> selectEntries(CommandStreamFilter filter, Set<CommandStreamInfoField> fields) {
        // build request
        String queryStr = queryBuilder.createSelectEntriesQuery(filter, fields);
        logger.debug(queryStr);
        List<Entry<CommandStreamKey, ICommandStreamInfo>> results = new ArrayList<>();
        try(Connection connection = this.hikariDataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(queryStr)) {
                    while (resultSet.next()) {
                        long id = resultSet.getLong("id");
                        String data = resultSet.getString("data");
                        ICommandStreamInfo commandStreamInfo = SerializerUtils.readICommandStreamInfoFromJson(data);
                        results.add(Map.entry(new CommandStreamKey(cmdStore.idScope, id), commandStreamInfo));
                    }
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        return results.stream();
    }

    @Override
    public ICommandStreamInfo put(CommandStreamKey key, ICommandStreamInfo csInfo) {
        try(Connection connection = this.hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.updateByIdQuery())) {
                String json = SerializerUtils.writeICommandStreamInfoToJson(csInfo);
                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                jsonObject.setValue(json);

                preparedStatement.setObject(1, jsonObject);
                preparedStatement.setLong(2, key.getInternalID().getIdAsLong());
                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    // update cache by invalidate old entry
                    cache.invalidate(key.getInternalID().getIdAsLong());
                    return csInfo;
                } else {
                    throw new RuntimeException("Cannot update datastream ");
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized ICommandStreamInfo remove(Object o) {
        if (!(o instanceof CommandStreamKey)) {
            throw new UnsupportedOperationException("Remove operation is not supported with argument != CommandStreamKey key, got=" + o.getClass());
        }
        CommandStreamKey key = (CommandStreamKey) o;
        ICommandStreamInfo data = this.get(key);

        // remove corresponding commands
        CommandFilter filter = new CommandFilter.Builder()
                .withCommandStreams(key.getInternalID())
                .build();
        this.cmdStore.removeEntries(filter);

        try(Connection connection = this.hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.removeByIdQuery())) {
                preparedStatement.setLong(1, key.getInternalID().getIdAsLong());
                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    return data;
                } else {
                    throw new RuntimeException("Cannot remove ICommandStreamInfo " + data.getName());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void clear() {
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
    public void commit() throws DataStoreException {
        cmdStore.commit();
    }

    @Override
    public void linkTo(ISystemDescStore systemStore) {
        super.linkTo(this.systemStore);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return this.get(key) != null;
    }


    @Override
    public Set<Map.Entry<CommandStreamKey, ICommandStreamInfo>> entrySet() {
        CommandStreamFilter commandStreamFilter = new CommandStreamFilter.Builder().build();
        return this.selectEntries(commandStreamFilter, new HashSet<>()).collect(Collectors.toSet());
    }

    public Collection<ICommandStreamInfo> values() {
        return this.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toSet());
    }

    public Set<CommandStreamKey> keySet() {
        return this.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
    }

}