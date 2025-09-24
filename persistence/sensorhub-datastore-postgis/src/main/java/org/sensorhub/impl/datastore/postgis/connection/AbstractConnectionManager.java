package org.sensorhub.impl.datastore.postgis.connection;

import org.sensorhub.api.datastore.DataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public abstract class AbstractConnectionManager {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractConnectionManager.class);

    public AbstractConnectionManager() {
    }

    public long executeQueryAndGetAsLong(String query, String fieldName) {
        try (Connection connection = getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(query)) {
                    return resultSet.getLong(fieldName);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkTable(String tableName) throws SQLException {
        DatabaseMetaData databaseMetaData = getConnection().getMetaData();
        try (ResultSet resultSet = databaseMetaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    public void executeQueries(String[] queries) throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            for (String query : queries) {
                statement.execute(query);
            }
        }
    }

    protected abstract Connection getConnection();

    protected abstract void commit() throws DataStoreException;

    protected abstract void close();

    protected abstract PreparedStatement getStatement(String query);

}
