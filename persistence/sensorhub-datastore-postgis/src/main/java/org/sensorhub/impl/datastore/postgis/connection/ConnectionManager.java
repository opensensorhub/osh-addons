package org.sensorhub.impl.datastore.postgis.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    public enum BatchStatus {
        SUCCESS,        // code >= 0
        SUCCESS_UNKNOWN, // code == -2
        FAILED           // code == -3
    }

    private static final int DRIVER_LOGIN_TIMEOUT_SECONDS = 5 * 60;
    private static final int MAX_POOL_SIZE = 8;
    private static final int MIN_IDLE_CONNECTIONS = 0;
    private static final long CONNECTION_TIMEOUT_MS = 30_000L;
    private static final long MAX_LIFETIME_MS = 15 * 60 * 1000L;
    private static final long KEEPALIVE_TIME_MS = 5 * 60 * 1000L;
    private static final long IDLE_TIMEOUT_MS = 2 * 60 * 1000L;

    private final String url;
    private final String dbName;
    private final String login;
    private final String password;
    private final HikariDataSource hikariDataSourceInstance;
    private int batchSize = 100;
    protected final ConcurrentLinkedDeque<String> batchList = new ConcurrentLinkedDeque<>();
    protected final ReentrantLock transactionLock = new ReentrantLock();

    /**
     * Use separate ThreadSafeBatchExecutor to execute batch queries.
     *
     * @param url database host:port or equivalent connection target
     * @param dbName database name
     * @param login database username
     * @param password database password
     */
    public ConnectionManager(String url, String dbName, String login, String password) {
        this.url = url;
        this.dbName = dbName;
        this.login = login;
        this.password = password;
        DriverManager.setLoginTimeout(DRIVER_LOGIN_TIMEOUT_SECONDS);
        this.hikariDataSourceInstance = createHikariDataSource();
    }

    public void enableBatch(int batchSize) {
        this.batchSize = batchSize;
    }

    private HikariDataSource createHikariDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + url + "/" + dbName);
        config.setUsername(login);
        config.setPassword(password);

        // Reduce steady-state idle connection footprint across multiple pools.
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE_CONNECTIONS);
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        config.setMaxLifetime(MAX_LIFETIME_MS);
        config.setKeepaliveTime(KEEPALIVE_TIME_MS);
        config.setIdleTimeout(IDLE_TIMEOUT_MS);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("tcpKeepAlive", "true");
        config.addDataSourceProperty("socketTimeout", "300");
        config.addDataSourceProperty("networkTimeout", "60");
        config.setAutoCommit(true);

        return new HikariDataSource(config);
    }

    public Connection getConnection() {
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
            if (hikariDataSourceInstance != null) {
                hikariDataSourceInstance.close();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot close Batch connection");
        }
    }

    public void tryCommit() {
        if (batchList.size() >= getBatchSize()) {
            this.commit();
        }
    }

    protected void commitBatch() {
        if (batchList.isEmpty()) {
            return;
        }

        List<String> queries;
        synchronized (batchList) {
            queries = new ArrayList<>(batchList);
            batchList.clear();
        }

        int[] rows = null;
        transactionLock.lock();
        try (Connection connection = this.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String sqlQuery : queries) {
                    statement.addBatch(sqlQuery);
                }
                rows = statement.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            transactionLock.unlock();
        }

        if (rows != null) {
            Map<BatchStatus, Long> summary = Arrays.stream(rows)
                .mapToObj(ConnectionManager::classify)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
            long success = summary.getOrDefault(BatchStatus.SUCCESS, 0L);
            long successUnknown = summary.getOrDefault(BatchStatus.SUCCESS_UNKNOWN, 0L);
            long failed = summary.getOrDefault(BatchStatus.FAILED, 0L);
            log.info("Batch execution: SUCCESS={}, SUCCESS_UNKNOWN={}, FAILED={}", success, successUnknown, failed);
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

    protected static BatchStatus classify(int code) {
        if (code >= 0) {
            return BatchStatus.SUCCESS;
        }
        if (code == Statement.SUCCESS_NO_INFO) {
            return BatchStatus.SUCCESS_UNKNOWN;
        }
        return BatchStatus.FAILED; // Statement.EXECUTE_FAILED (-3)
    }
}
