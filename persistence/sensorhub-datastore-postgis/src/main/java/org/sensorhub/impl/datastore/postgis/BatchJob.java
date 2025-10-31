package org.sensorhub.impl.datastore.postgis;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class BatchJob {
    private Connection connection;
    private PreparedStatement preparedStatement;

    public BatchJob(Connection connection, PreparedStatement preparedStatement) {
        this.preparedStatement = preparedStatement;
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }
}
