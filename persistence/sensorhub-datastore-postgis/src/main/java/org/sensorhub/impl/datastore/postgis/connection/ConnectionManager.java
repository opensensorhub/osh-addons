package org.sensorhub.impl.datastore.postgis.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionManager {

    private String url;
    private String dbName;
    private String login;
    private String password;
    private static HikariDataSource hikariDataSourceInstance = null;
    private int batchSize = 100;
    protected AtomicInteger currentBatchSize = new AtomicInteger(0);
    protected final CopyOnWriteArrayList<String> batchList = new CopyOnWriteArrayList<>();

    /**
     * Use separate ThreadSafeBatchExecutor to execute batch queries
     * @param url
     * @param dbName
     * @param login
     * @param password
     */
    public ConnectionManager(String url, String dbName, String login, String password) {
        this.url = url;
        this.dbName = dbName;
        this.login = login;
        this.password = password;
        DriverManager.setLoginTimeout(1000 * 60 * 5); // 5 minutes;
        hikariDataSourceInstance = createHikariDataSource();
    }

    public void enableBatch(int batchSize) {
        this.batchSize = batchSize;
    }

    private HikariDataSource createHikariDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + url + "/" + dbName);
        config.setUsername(login);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(1000 * 60 * 5); // 5 minutes

//                        config.setMaximumPoolSize(200_000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("tcpKeepAlive","true");
        config.addDataSourceProperty("socketTimeout","60");
        config.addDataSourceProperty("networkTimeout","60");
        config.setAutoCommit(true);
        return new HikariDataSource(config);
    }

    public Connection getConnection()  {
        try {
            return hikariDataSourceInstance.getConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public void addBatch(String sqlQuery) {
        batchList.add(sqlQuery);
    }

    public void close() {
        try {
            if(hikariDataSourceInstance != null) {
                hikariDataSourceInstance.close();
            }
        }catch (Exception ex) {
            throw new IllegalStateException("Cannot close Batch connection");
        }
    }

    public void tryCommit() {
        if (currentBatchSize.get() >= getBatchSize()) {
            this.commit();
            currentBatchSize.getAndSet(0);
        }
    }
    protected void commitBatch() {
        // block list access
        synchronized (batchList) {
            try(Connection connection = this.getConnection()) {
                try(Statement statement = connection.createStatement()) {
                    for(String sqlQuery: batchList) {
                        statement.addBatch(sqlQuery);
                    }
                    statement.executeBatch();
                    batchList.clear();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void commit() {
        try {
            this.commitBatch();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Cannot commit Batch connection");
        }
    }

    public int getBatchSize() {
        return batchSize;
    }
}
