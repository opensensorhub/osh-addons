package org.sensorhub.impl.datastore.postgis;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionManager {

    private String url;
    private String dbName;
    private String login;
    private String password;
    private static HikariDataSource hikariDataSourceInstance = null;
    private ThreadSafeBatchExecutor threadSafeBatchExecutor;
    private  Connection batchConnection;
    private int batchSize = 100;
    protected AtomicInteger currentBatchSize = new AtomicInteger(0);

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
    }

    public void enableBatch(int batchSize, final String query) {
        this.batchSize = batchSize;
        this.threadSafeBatchExecutor = new ThreadSafeBatchExecutor(() -> {
            try {
                Connection connection = getConnection(false);
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                return new BatchJob(connection,preparedStatement);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        batchConnection = this.getConnection(false);
    }

    private HikariDataSource createHikariDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + url + "/" + dbName);
        config.setUsername(login);
        config.setPassword(password);
        config.setMaximumPoolSize(5);

//                        config.setMaximumPoolSize(200_000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setAutoCommit(true);
        return new HikariDataSource(config);
    }

    private synchronized HikariDataSource getHikariDataSourceInstance() {
        if(hikariDataSourceInstance == null) {
            hikariDataSourceInstance = createHikariDataSource();
        }
        return hikariDataSourceInstance;
    }

    public Connection getConnection()  {
        try {
            return this.getHikariDataSourceInstance().getConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public Connection getConnection(boolean autoCommit) {
        if(!autoCommit) {
            return this.getNewNoAutoCommitConnection();
        } else {
            return this.getConnection();
        }
    }

    public BatchJob getBatchJob() {
        try {
            return this.threadSafeBatchExecutor.getConnection();
        } catch (SQLException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Connection getBatchConnection() {
        return batchConnection;
    }

    public void offerBatchJob(BatchJob batchJob) {
        if(this.threadSafeBatchExecutor != null) {
            this.threadSafeBatchExecutor.offer(batchJob);
        }
    }
    private Connection getNewNoAutoCommitConnection() {
        try {
            String urlPostgres = "jdbc:postgresql://" + url + "/" + dbName;
            Properties props = new Properties();
            props.setProperty("user", login);
            props.setProperty("password", password);
//            props.setProperty("reWriteBatchedInserts", "false");
//            props.setProperty("tcpKeepAlive","true");
            Connection connection =  DriverManager.getConnection(urlPostgres, props);
            connection.setAutoCommit(false);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        if(hikariDataSourceInstance != null) {
            hikariDataSourceInstance.close();
        }
        if(threadSafeBatchExecutor != null) {
            threadSafeBatchExecutor.close();
        }
        try {
            if (batchConnection != null && !batchConnection.isClosed()) {
                batchConnection.commit();
                batchConnection.close();
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
    public void commit() {
        try {
            if(threadSafeBatchExecutor != null) {
                threadSafeBatchExecutor.commit();
            }
            try {
                if (batchConnection != null && !batchConnection.isClosed()) {
                    batchConnection.commit();
                }
            }catch (Exception ex) {
                throw new IllegalStateException("Cannot commit Batch connection");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    public int getBatchSize() {
        return batchSize;
    }
}
