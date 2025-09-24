package org.sensorhub.impl.datastore.postgis.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.sensorhub.api.datastore.DataStoreException;

import java.sql.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HikariConnectionManager extends AbstractConnectionManager {
    static volatile HikariDataSource hikariDataSourceInstance = null;
    static final Lock reentrantLock = new ReentrantLock();

    public HikariConnectionManager(String url, String  dbName, String  login, String  password) {
        this.initHikariConnection(url, dbName,login,password);
    }

    private void initHikariConnection(String url,String  dbName,String  login,String  password) {
        this.hikariDataSourceInstance = this.getHikariDataSourceInstance(url, dbName, login, password);
    }

    // is thread-safe
    public HikariDataSource getHikariDataSourceInstance(String url, String dbName, String login, String password) {
        if(hikariDataSourceInstance == null) {
            reentrantLock.lock();
            try {
                if(hikariDataSourceInstance == null) {
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl("jdbc:postgresql://" + url + "/" + dbName);
                    config.setUsername(login);
                    config.setPassword(password);
                    config.setKeepaliveTime(30000*5);
                    config.setMaximumPoolSize(200);
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    config.setAutoCommit(true);
                    hikariDataSourceInstance = new HikariDataSource(config);
                }
            } finally {
                reentrantLock.unlock();
            }
        }
        return hikariDataSourceInstance;
    }

    @Override
    protected Connection getConnection() {
        try {
            return hikariDataSourceInstance.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void commit() throws DataStoreException {
        // auto commit
    }

    @Override
    protected void close() {

    }

    @Override
    protected PreparedStatement getStatement(String query) {
        return null;
    }
}
