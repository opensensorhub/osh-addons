package org.sensorhub.impl.datastore.postgis;

import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class ThreadSafeBatchExecutor<T> {

    private final LinkedBlockingQueue<BatchJob> queue = new LinkedBlockingQueue<>(3);

    public ThreadSafeBatchExecutor(Supplier<BatchJob> connectionSupplier) {
        initThreadResources(connectionSupplier);
    }

    private void initThreadResources(Supplier<BatchJob> connectionSupplier) {
        try {
            for(int i=0;i < queue.remainingCapacity();i++) {
                queue.offer(connectionSupplier.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BatchJob getConnection() throws SQLException, InterruptedException {
        return queue.poll(1, TimeUnit.SECONDS);
    }

    public void offer(BatchJob batchJob) {
        queue.offer(batchJob);
    }

    public void commit() {
        synchronized (queue) {
            queue.forEach(batchJob -> {
                try {
                    batchJob.getPreparedStatement().executeBatch();
                    batchJob.getPreparedStatement().clearBatch();
                    batchJob.getConnection().commit();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
    public void close() {
        synchronized (queue) {
            queue.forEach(batchJob -> {
                try {
                    if(!batchJob.getPreparedStatement().isClosed()) {
                        batchJob.getPreparedStatement().executeBatch();
                        batchJob.getPreparedStatement().clearBatch();
                        batchJob.getPreparedStatement().close();
                    }

                    if(!batchJob.getConnection().isClosed()) {
                        batchJob.getConnection().commit();
                        batchJob.getConnection().close();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        queue.clear();
    }
}
