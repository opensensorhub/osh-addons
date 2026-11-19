/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.store.command;

import net.opengis.swe.v20.DataBlock;
import org.postgresql.util.PGobject;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.command.ICommandStreamInfo;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.command.*;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.store.PostgisStore;
import org.sensorhub.impl.datastore.postgis.builder.IteratorResultSet;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderCommandStore;
import org.sensorhub.impl.datastore.postgis.utils.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockByte;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.sensorhub.api.datastore.command.ICommandStore.CommandField.*;
import static org.sensorhub.api.datastore.obs.IObsStore.ObsField.FOI_ID;

public class PostgisCommandStoreImpl extends PostgisStore<QueryBuilderCommandStore> implements ICommandStore {
    private static final Logger logger = LoggerFactory.getLogger(PostgisCommandStoreImpl.class);

    protected PostgisCommandStreamStoreImpl commandStreamStore;
    protected PostgisCommandStatusStore commandStatusStore;

    public PostgisCommandStoreImpl(String url, String dbName, String login, String password,
                                   int idScope, IdProviderType dsIdProviderType, boolean useBatch) {
        super(idScope, dsIdProviderType, new QueryBuilderCommandStore(), useBatch);
        this.init(url, dbName, login, password);
    }

    public PostgisCommandStoreImpl(String url, String dbName, String login, String password, String dataStoreName,
                                   int idScope, IdProviderType dsIdProviderType, boolean useBatch) {
        super(idScope, dsIdProviderType, new QueryBuilderCommandStore(dataStoreName), useBatch);
        this.init(url, dbName, login, password);
    }

    protected void init(String url, String dbName, String login, String password) {
        super.init(url,dbName,login,password,new String[]{
                queryBuilder.createTableQuery(),
                queryBuilder.createDataIndexQuery(),
                queryBuilder.createCommandStreamIndexQuery(),
                queryBuilder.createIssueTimeIndexQuery(),
                queryBuilder.createSenderIdIndexQuery(),
                queryBuilder.createFoidIdIndexQuery()
        });
        this.commandStreamStore = new PostgisCommandStreamStoreImpl(this, url, dbName,login,password,idScope,idProviderType,useBatch);
        this.commandStatusStore = new PostgisCommandStatusStore(this, url, dbName,login,password,idScope,idProviderType, useBatch);
        this.commandStatusStore.linkTo(commandStreamStore);
        this.commandStatusStore.linkTo(this);

        linkTo(commandStreamStore);
        linkTo(commandStatusStore);
    }
    @Override
    public Stream<Entry<BigId, ICommandData>> selectEntries(CommandFilter filter, Set<CommandField> fields) {
        String queryStr = queryBuilder.createSelectEntriesQuery(filter, fields);
        if(logger.isDebugEnabled()) {
            logger.debug(queryStr);
        }
        IteratorResultSet<Entry<BigId, ICommandData>> iteratorResultSet =
                new IteratorResultSet<>(
                        queryStr,
                        connectionManager,
                        filter.getLimit(),
                        (resultSet) -> resultSetToEntry(resultSet, fields),
                        (entry) -> (filter.getValuePredicate() == null || filter.getValuePredicate().test(entry.getValue())));
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iteratorResultSet, Spliterator.ORDERED), false);
    }

    private Entry<BigId, ICommandData> resultSetToEntry(ResultSet resultSet, Set<CommandField> fields) {
        try {
            BigId id = BigId.fromLong(idScope, resultSet.getLong("id"));
            CommandData.Builder commandDataBuilder = new CommandData.Builder();
            CommandData cmdData = null;

            // add id
            boolean noFields = fields != null && !fields.isEmpty();
            BigId commandStreamId = null;
            if (!noFields || fields.contains(COMMANDSTREAM_ID)) {
                long commandStreamIdAsLong = resultSet.getLong(String.valueOf(COMMANDSTREAM_ID));
                if (!resultSet.wasNull()) {
                    commandStreamId = BigId.fromLong(idScope, commandStreamIdAsLong);

                    commandDataBuilder = commandDataBuilder.withCommandStream(commandStreamId);
                }
            }

            if (!noFields || fields.contains(FOI_ID)) {
                long foiId = resultSet.getLong(String.valueOf(FOI_ID));
                if (!resultSet.wasNull()) {
                    commandDataBuilder = commandDataBuilder.withFoi(BigId.fromLong(idScope, foiId));
                }
            }
            // required
            Timestamp issueTimestamp = resultSet.getTimestamp(String.valueOf(ISSUE_TIME), UTC_LOCAL);
            if (!resultSet.wasNull()) {
                commandDataBuilder = commandDataBuilder.withIssueTime(issueTimestamp.toInstant());
            }

            if (!noFields || fields.contains(SENDER_ID)) {
                String senderId = resultSet.getString(String.valueOf(SENDER_ID));
                if(!resultSet.wasNull()) {
                    commandDataBuilder = commandDataBuilder.withSender(senderId);
                }
            }

            // get commandStream schema from commandStreamId
            if (commandStreamId != null) {
                ICommandStreamInfo commandStreamInfo = commandStreamStore.get(new CommandStreamKey(commandStreamId));
                DataBlock dataBlock = SerializerUtils.readDataBlockFromJson(commandStreamInfo.getRecordStructure(), resultSet.getString(String.valueOf(CommandField.PARAMETERS)));
                cmdData = commandDataBuilder.withParams(dataBlock).build();
            } else {
                // fake result
                cmdData = commandDataBuilder.withParams(new DataBlockByte()).build();
            }

            return Map.entry(id, cmdData);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot parse resultSet to CommandData",ex);
        }
    }

    @Override
    public ICommandStreamStore getCommandStreams() {
        return this.commandStreamStore;
    }

    @Override
    public ICommandStatusStore getStatusReports() {
        return this.commandStatusStore;
    }

    @Override
    public BigId add(ICommandData cmd) {
        CommandStreamKey commandStreamKey = new CommandStreamKey(cmd.getCommandStreamID());
        if (!commandStreamStore.containsKey(commandStreamKey))
            throw new IllegalStateException("Unknown commandStream" + cmd.getCommandStreamID());

        try (Connection connection1 = this.connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection1.prepareStatement(queryBuilder.insertCommandQuery(), Statement.RETURN_GENERATED_KEYS)) {

                // insert DataStreamId
                preparedStatement.setLong(1,commandStreamKey.getInternalID().getIdAsLong());

                // insert sendId if any
                if (cmd.getSenderID() != null && !cmd.getSenderID().isEmpty()) {
                    preparedStatement.setString(2, cmd.getSenderID());
                } else {
                    preparedStatement.setNull(2, Types.VARCHAR);
                }

                // insert foiId if any
                if (cmd.hasFoi()) {
                    preparedStatement.setLong(3, cmd.getFoiID().getIdAsLong());
                } else {
                    preparedStatement.setNull(3, Types.BIGINT);
                }

                // insert issueTime as timestamp
                if (cmd.getIssueTime() != null) {
                    preparedStatement.setTimestamp(4, Timestamp.from(cmd.getIssueTime()),UTC_LOCAL);
                } else {
                    preparedStatement.setNull(4, Types.TIMESTAMP_WITH_TIMEZONE);
                }

                // insert DataBlock
                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                ICommandStreamInfo commandStreamInfo = commandStreamStore.get(new CommandStreamKey(cmd.getCommandStreamID()));
                jsonObject.setValue(SerializerUtils.writeDataBlockToJson(commandStreamInfo.getRecordStructure(),
                        commandStreamInfo.getRecordEncoding(), cmd.getParams()));

                preparedStatement.setObject(5, jsonObject);

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
    public Stream<CommandStats> getStatistics(CommandStatsQuery query) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void linkTo(IFoiStore foiStore) {
        super.linkTo(foiStore);
    }

    @Override
    public ICommandData get(Object o) {
        if (!(o instanceof BigId)) {
            throw new UnsupportedOperationException("Get operation is not supported with argument != BigId key, got=" + o.getClass());
        }
        BigId key = (BigId) o;
        try (Connection connection = this.connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.selectByIdQuery())) {
                preparedStatement.setLong(1, key.getIdAsLong());
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    CommandData.Builder cmdDataBuilder = new CommandData.Builder();

                    BigId commandStreamId = BigId.fromLong(idScope,resultSet.getLong(String.valueOf(COMMANDSTREAM_ID)));
                    cmdDataBuilder = cmdDataBuilder.withCommandStream(commandStreamId);

                    long foiId = resultSet.getLong(String.valueOf(FOI_ID));
                    if (!resultSet.wasNull()) {
                        cmdDataBuilder = cmdDataBuilder.withFoi(BigId.fromLong(idScope, foiId));
                    }

                    String senderId = resultSet.getString(String.valueOf(SENDER_ID));
                    if (!resultSet.wasNull()) {
                        cmdDataBuilder = cmdDataBuilder.withSender(senderId);
                    }

                    Timestamp issueTimestamp = resultSet.getTimestamp(String.valueOf(ISSUE_TIME), UTC_LOCAL);
                    if (!resultSet.wasNull()) {
                        cmdDataBuilder = cmdDataBuilder.withIssueTime(issueTimestamp.toInstant());
                    }

                    // get datastream schema from datastreamId
                    ICommandStreamInfo commandStreamInfo = commandStreamStore.get(new CommandStreamKey(commandStreamId));
                    DataBlock dataBlock = SerializerUtils.readDataBlockFromJson(commandStreamInfo.getRecordStructure(), resultSet.getString(String.valueOf(CommandField.PARAMETERS)));
                    return cmdDataBuilder.withParams(dataBlock).build();
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ICommandData put(BigId key, ICommandData value) {
        ICommandData oldCommand = this.get(key);
        if (oldCommand == null)
            throw new UnsupportedOperationException("put can only be used to update existing entries");

        try (Connection connection = this.connectionManager.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.updateByIdQuery())) {
                preparedStatement.setLong(1, value.getCommandStreamID().getIdAsLong());
                preparedStatement.setString(2, value.getSenderID());

                if(value.hasFoi()) {
                    preparedStatement.setLong(3, value.getFoiID().getIdAsLong());
                } else {
                    preparedStatement.setNull(3, Types.BIGINT);
                }

                if (value.getIssueTime() != null) {
                    preparedStatement.setTimestamp(4, Timestamp.from(value.getIssueTime()), UTC_LOCAL);
                } else {
                    preparedStatement.setNull(4, Types.TIMESTAMP_WITH_TIMEZONE);
                }

                // insert DataBlock
                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                ICommandStreamInfo commandStreamInfo = commandStreamStore.get(new CommandStreamKey(value.getCommandStreamID()));
                jsonObject.setValue(SerializerUtils.writeDataBlockToJson(commandStreamInfo.getRecordStructure(),
                        commandStreamInfo.getRecordEncoding(), value.getParams()));

                preparedStatement.setObject(5, jsonObject);
                preparedStatement.setLong(6, key.getIdAsLong());

                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    return value;
                } else {
                    throw new RuntimeException("Cannot insert obs ");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ICommandData remove(Object o) {
        if (!(o instanceof BigId)) {
            throw new UnsupportedOperationException("Remove operation is not supported with argument != BigId key, got=" + o.getClass());
        }
        BigId key = (BigId) o;
        ICommandData data = this.get(o);
        try (Connection connection = this.connectionManager.getConnection()) {
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
    public void commit() throws DataStoreException {

    }

    @Override
    public boolean containsKey(Object key) {
        return this.get(key) != null;
    }


    @Override
    public Set<Entry<BigId, ICommandData>> entrySet() {
        CommandFilter commandFilter = new CommandFilter.Builder().build();
        return this.selectEntries(commandFilter, new HashSet<>()).collect(Collectors.toSet());
    }

    public Collection<ICommandData> values() {
        return this.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toSet());
    }

    public Set<BigId> keySet() {
        Set<Entry<BigId, ICommandData>> res = this.entrySet();
        return res.stream().map(Map.Entry::getKey).collect(Collectors.toSet());
    }
}