package org.sensorhub.impl.datastore.postgis;

import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class ThreadSafeBatchExecutor {

    private final LinkedBlockingQueue<Connection> queue = new LinkedBlockingQueue<>(3);

    public ThreadSafeBatchExecutor(Supplier<Connection> connectionSupplier) {
        initThreadResources(connectionSupplier);
    }

    private void initThreadResources(Supplier<Connection> connectionSupplier) {
        try {
            for(int i=0;i < queue.remainingCapacity();i++) {
                queue.offer(connectionSupplier.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException, InterruptedException {
        return queue.poll(1, TimeUnit.SECONDS);
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
