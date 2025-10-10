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

import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.util.PGobject;
import org.sensorhub.api.command.ICommandStatus;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.command.*;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.PostgisStore;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.sensorhub.impl.datastore.postgis.builder.IteratorResultSet;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderCommandStatusStore;
import org.sensorhub.impl.datastore.postgis.utils.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Asserts;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.sensorhub.api.datastore.command.ICommandStatusStore.CommandStatusField.COMMAND_ID;


public class PostgisCommandStatusStore extends PostgisStore<QueryBuilderCommandStatusStore> implements ICommandStatusStore {
    private static final Logger logger = LoggerFactory.getLogger(PostgisCommandStoreImpl.class);

    final int idScope;
    PostgisCommandStoreImpl commandStore;

    public PostgisCommandStatusStore(PostgisCommandStoreImpl commandStore, HikariDataSource hikariDataSource, boolean useBatch) {
        super(commandStore.idScope, commandStore.idProviderType, new QueryBuilderCommandStatusStore(),useBatch);
        this.idScope = commandStore.idScope;
        this.commandStore = commandStore;
        this.init(commandStore, hikariDataSource);
    }

    protected void init(PostgisCommandStoreImpl commandStore, HikariDataSource hikariDataSource) {
        this.hikariDataSource = Asserts.checkNotNull(hikariDataSource, HikariDataSource.class);
        try(Connection connection = this.hikariDataSource.getConnection()) {
            if (!PostgisUtils.checkTable(connection, queryBuilder.getStoreTableName())) {
                // create table
                PostgisUtils.executeQueries(connection, new String[]{
                        queryBuilder.createTableQuery(),
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        queryBuilder.linkTo(this.commandStore);
        queryBuilder.linkTo(this.commandStore.getCommandStreams());
    }

    @Override
    public String getDatastoreName() {
        return queryBuilder.getStoreTableName();
    }

    @Override
    public Stream<Entry<BigId, ICommandStatus>> selectEntries(CommandStatusFilter filter, Set<CommandStatusField> fields) {
        System.out.println(filter);
        String queryStr = queryBuilder.createSelectEntriesQuery(filter, fields);
        logger.debug(queryStr);
        IteratorResultSet<Entry<BigId, ICommandStatus>> iteratorResultSet =
                new IteratorResultSet<>(
                        queryStr,
                        hikariDataSource,
                        filter.getLimit(),
                        (resultSet) -> resultSetToEntry(resultSet, fields),
                        (entry) -> (filter.getValuePredicate() == null || filter.getValuePredicate().test(entry.getValue())));
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iteratorResultSet, Spliterator.ORDERED), false);
    }

    private Entry<BigId, ICommandStatus> resultSetToEntry(ResultSet resultSet, Set<ICommandStatusStore.CommandStatusField> fields) {
        try {
            BigId id = BigId.fromLong(idScope, resultSet.getLong("id"));
            boolean noFields = fields != null && !fields.isEmpty();
            BigId commandStreamId = null;
            if (!noFields || fields.contains(COMMAND_ID)) {
                long commandIdAsLong = resultSet.getLong(String.valueOf(COMMAND_ID));
                if (!resultSet.wasNull()) {
                    var commandId = BigId.fromLong(idScope, commandIdAsLong);
                    commandStreamId = commandStore.get(commandId).getCommandStreamID();
                }
            }

            ICommandStatus cmdStatus = null;
            if (commandStreamId != null) {
                ICommandStreamInfo commandStreamInfo = commandStore.getCommandStreams().get(new CommandStreamKey(commandStreamId));
                cmdStatus = SerializerUtils.readICommandStatusFromJson(resultSet.getString("data"), commandStreamInfo);
            } else {
                cmdStatus = SerializerUtils.readICommandStatusFromJson(resultSet.getString("data"));
            }
            return Map.entry(id, cmdStatus);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot parse resultSet to CommandStatus",ex);
        }
    }

    @Override
    public void commit() throws DataStoreException {

    }

    protected ICommandStreamInfo getContext(ICommandStatus status) {
        var cmdId = status.getCommandID();
        var cmdStreamId = commandStore.get(cmdId).getCommandStreamID();
        var cmdStreamKey = new CommandStreamKey(cmdStreamId);
        return commandStore.getCommandStreams().get(cmdStreamKey);
    }

    @Override
    public BigId add(ICommandStatus rec) {
        try (Connection connection1 = hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection1.prepareStatement(queryBuilder.insertCommandQuery(), Statement.RETURN_GENERATED_KEYS)) {
                // insert Object
                String objectAsJson;
                // Use context to serialize inline records
                if (rec.getResult() != null &&
                        rec.getResult().getInlineRecords() != null &&
                        !rec.getResult().getInlineRecords().isEmpty()) {
                    var context = getContext(rec);
                    objectAsJson = SerializerUtils.writeICommandStatusToJson(rec, context);
                } else {
                    objectAsJson = SerializerUtils.writeICommandStatusToJson(rec);
                }

                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                jsonObject.setValue(objectAsJson);

                preparedStatement.setObject(1, jsonObject);

                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
                        long generatedKey = 0;
                        if (rs.next()) {
                            generatedKey = rs.getLong(1);
                        }
                        return BigId.fromLong(idScope, generatedKey);
                    }
                } else {
                    throw new RuntimeException("Cannot insert command ");
                }
            } catch (Exception e) {
                throw new RuntimeException("Cannot insert command", e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot insert command", e);
        }
    }

    @Override
    public ICommandStatus get(Object o) {
        if (!(o instanceof BigId)) {
            throw new UnsupportedOperationException("Get operation is not supported with argument != BigId key, got=" + o.getClass());
        }
        BigId key = (BigId) o;
        try (Connection connection = hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.selectByIdQuery())) {
                preparedStatement.setLong(1, key.getIdAsLong());
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    return SerializerUtils.readICommandStatusFromJson(resultSet.getString("data"));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public ICommandStatus put(BigId key, ICommandStatus value) {
        ICommandStatus oldCommand = this.get(key);
        if (oldCommand == null)
            throw new UnsupportedOperationException("put can only be used to update existing entries");

        try (Connection connection = hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.updateByIdQuery())) {
                preparedStatement.setLong(2, key.getIdAsLong());

                String objectAsJson = SerializerUtils.writeICommandStatusToJson(value);

                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                jsonObject.setValue(objectAsJson);

                preparedStatement.setObject(1, jsonObject);

                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    return value;
                } else {
                    throw new RuntimeException("Cannot update command ");
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ICommandStatus remove(Object o) {
        if (!(o instanceof BigId)) {
            throw new UnsupportedOperationException("Remove operation is not supported with argument != BigId key, got=" + o.getClass());
        }
        BigId key = (BigId) o;
        ICommandStatus data = this.get(o);
        try (Connection connection = hikariDataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.removeByIdQuery())) {
                preparedStatement.setLong(1, key.getIdAsLong());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.get(key) != null;
    }


    @Override
    public Set<Map.Entry<BigId, ICommandStatus>> entrySet() {
        CommandStatusFilter filter = new CommandStatusFilter.Builder().build();
        return this.selectEntries(filter, new HashSet<>()).collect(Collectors.toSet());
    }

    public Collection<ICommandStatus> values() {
        return this.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toSet());
    }

    public Set<BigId> keySet() {
        return this.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public void linkTo(ISystemDescStore systemDescStore) {
        super.linkTo(systemDescStore);
    }
    public void linkTo(ICommandStreamStore commandStreamStore) { super.linkTo(commandStreamStore); }
    public void linkTo(ICommandStore commandStore) {
        super.linkTo(commandStore);
    }
}
