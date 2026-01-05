package org.sensorhub.impl.datastore.postgis.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    public enum BatchStatus {
        SUCCESS,        // code >= 0
        SUCCESS_UNKNOWN, // code == -2
        FAILED           // code == -3
    }

    private String url;
    private String dbName;
    private String login;
    private String password;
    private HikariDataSource hikariDataSourceInstance = null;
    private int batchSize = 100;
    protected final ConcurrentLinkedDeque<String> batchList = new ConcurrentLinkedDeque<>();
    protected final ReentrantLock transactionLock = new ReentrantLock();
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
        config.setMaximumPoolSize(50);
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
        if (batchList.size() >= getBatchSize()) {
            this.commit();
        }
    }
    protected void commitBatch() {
        if(batchList.isEmpty()) {
            return;
        }
        int rows[] = null;
        // block list access
        transactionLock.lock();
        try (Connection connection = this.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                int read = 0;
                for (String sqlQuery : batchList) {
                    statement.addBatch(sqlQuery);
                    read++;
                    if(read >= batchSize) {
                        break;
                    }
                }
                rows = statement.executeBatch();
                for(int i=0;i< read;i++) {
                    batchList.removeFirst();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            transactionLock.unlock();
        }
        this.displayRowStats(rows);
    }
    protected void displayRowStats(int [] rows) {
        if (rows != null) {
            Map<BatchStatus, Long> summary =
                    Arrays.stream(rows)
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
        if (code >= 0) return BatchStatus.SUCCESS;
        if (code == Statement.SUCCESS_NO_INFO) return BatchStatus.SUCCESS_UNKNOWN;
        return BatchStatus.FAILED; // Statement.EXECUTE_FAILED (-3)
    }
}
