package org.sensorhub.impl.datastore.postgis;

import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ThreadSafeBatchExecutor {

    private final LinkedBlockingQueue<Connection> queue = new LinkedBlockingQueue<>(3);

    public ThreadSafeBatchExecutor(String url, String dbName, String login, String password, String sql) {
        initThreadResources(url,dbName,login,password,sql);
    }

    private void initThreadResources(String url, String dbName, String login, String password, String sql) {
        try {
            for(int i=0;i < queue.remainingCapacity();i++) {
                queue.offer(PostgisUtils.getConnection(url,dbName,login,password));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException, InterruptedException {
        return queue.poll(10, TimeUnit.MINUTES);
    }

    public void offer(Connection connection) {
        queue.offer(connection);
    }

    public void commit() {
        synchronized (queue) {
            queue.forEach(connection -> {
                try {
                    connection.commit();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
    public void close() {
        synchronized (queue) {
            queue.forEach(connection -> {
                try {
                    if(!connection.isClosed()) {
                        connection.commit();
                    }
                    connection.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        queue.clear();
    }
}
