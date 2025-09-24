package org.sensorhub.impl.datastore.postgis.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.sensorhub.api.datastore.DataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;

public class BatchConnectionManager extends AbstractConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(BatchConnectionManager.class);

    public ThreadLocal<PreparedStatement> batchPreparedStatement;
    protected static final int MAX_BATCH_SIZE = 10000;
    protected int currentBatchSize = 0;
    protected Connection batchConnection;

    public BatchConnectionManager(String url, String  dbName, String  login, String  password, String batchQueryString) {
        this.initBatchPrepareStatement(url,dbName,login,password,batchQueryString);
    }

    private void initBatchPrepareStatement(String url,String  dbName,String  login,String  password,String batchQueryString) {
        batchConnection = this.getConnection(url, dbName, login, password);
        try {
            batchConnection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        batchPreparedStatement = ThreadLocal.withInitial(() -> {
            try {
                PreparedStatement pstmt = batchConnection.prepareStatement(batchQueryString);
                org.postgresql.PGStatement pgstmt = pstmt.unwrap(org.postgresql.PGStatement.class);
                pgstmt.setPrepareThreshold(1);
                return pstmt;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Connection getConnection(String url, String dbName, String login, String password) {
        try {
            String urlPostgres = "jdbc:postgresql://" + url + "/" + dbName;
            Properties props = new Properties();
            props.setProperty("user", login);
            props.setProperty("password", password);
            props.setProperty("reWriteBatchedInserts", "true");
            props.setProperty("tcpKeepAlive","true");
            Connection connection =  DriverManager.getConnection(urlPostgres, props);
            connection.setAutoCommit(false);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Connection getConnection() {
        return batchConnection;
    }

    @Override
    public void commit() throws DataStoreException {
        try {
            synchronized (batchPreparedStatement) {
                if(currentBatchSize > 0) {
                    if (batchPreparedStatement != null) {
                        int[] rows = batchPreparedStatement.get().executeBatch();
//                        for(int i=0; i < rows.length; i++) {
//                            if(rows[i] == 0) {
//                                logger.error("Cannot commit");
//                                throw new DataStoreException("Cannot execute batch");
//                            }
//                        }
                    }
                    currentBatchSize = 0;
                }
            }
        } catch (Exception ex) {
            //
            try {
                batchConnection.rollback();
            } finally {
                logger.error("Cannot commit", ex);
                throw new DataStoreException("Cannot execute batch");
            }
        }
    }

    @Override
    public void close() {
        try {
            if (batchPreparedStatement != null) {
                batchPreparedStatement.get().close();
            }
            if(batchConnection != null && !batchConnection.isClosed()) {
                batchConnection.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected PreparedStatement getStatement(String query) {
        return null;
    }
}
