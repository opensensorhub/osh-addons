package org.sensorhub.impl.datastore.postgis.store.obs;

import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.datastore.postgis.BatchJob;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderObsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class PostgisBatchObsStoreImpl extends PostgisObsStoreImpl {
    private static final Logger logger = LoggerFactory.getLogger(PostgisBatchObsStoreImpl.class);
    public static final int BATCH_SIZE = 5000;

    public PostgisBatchObsStoreImpl(String url, String dbName, String login, String password, int idScope, IdProviderType dsIdProviderType) {
        this(url,dbName,login,password,DEFAULT_TABLE_NAME,idScope,dsIdProviderType);
    }

    public PostgisBatchObsStoreImpl(String url, String dbName, String login, String password, String dataStoreName,
                                                                        int idScope, IdProviderType dsIdProviderType) {
        super(url, dbName, login, password, dataStoreName,idScope, dsIdProviderType);
    }


    public PostgisBatchObsStoreImpl(String url, String dbName, String login, String password, String dataStoreName,
                                    int idScope, IdProviderType dsIdProviderType, QueryBuilderObsStore queryBuilderObsStore) {
        super(url, dbName, login, password, dataStoreName,idScope, dsIdProviderType,queryBuilderObsStore);
    }


    @Override
    protected void init(String url, String dbName, String login, String password, String[] initScripts) {
        super.init(url, dbName, login, password, initScripts);
        this.connectionManager.enableBatch(BATCH_SIZE, queryBuilder.removeByIdQuery());
    }

    @Override
    public synchronized BigId add(IObsData obs) {
        DataStreamKey dataStreamKey = new DataStreamKey(obs.getDataStreamID());
        if (!dataStreamStore.containsKey(dataStreamKey))
            throw new IllegalStateException("Unknown datastream with ID: " + obs.getDataStreamID().getIdAsLong());
        // check that FOI exists
//        if (obs.hasFoi() && foiStore != null && foiStore.contains(obs.getFoiID()))
//            throw new IllegalStateException("Unknown FOI: " + obs.getFoiID());
        try {
            batchLock.lock();
            Connection connection = this.connectionManager.getBatchConnection();
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.insertObsQuery(), Statement.RETURN_GENERATED_KEYS)) {
                this.fillAddStatement(dataStreamKey, obs, preparedStatement);
                int rows = preparedStatement.executeUpdate();
                try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
                    long generatedKey = 0;
                    if (rs.next()) {
                        generatedKey = rs.getLong(1);
                    }
                    return BigId.fromLong(idScope, generatedKey);
                }
            } catch (Exception e) {
                throw new RuntimeException("Cannot insert obs", e);
            } finally {
                this.connectionManager.tryCommit();
            }
        } finally {
            batchLock.unlock();
        }
    }

    @Override
    public synchronized IObsData remove(Object o) {
        if (!(o instanceof BigId)) {
            throw new UnsupportedOperationException("Remove operation is not supported with argument != BigId key, got=" + o.getClass());
        }
        BigId key = (BigId) o;
        IObsData data = this.get(o);

        logger.debug("Remove Obs with key={}", key);
        batchLock.lock();
        BatchJob batchJob = this.connectionManager.getBatchJob();
        try {
            PreparedStatement preparedStatement = batchJob.getPreparedStatement();
            preparedStatement.setLong(1, key.getIdAsLong());
            preparedStatement.addBatch();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot remove obs " + data.toString());
        } finally {
            this.connectionManager.offerBatchJob(batchJob);
            this.connectionManager.tryCommit();
            batchLock.unlock();
        }
        return data;
    }

    @Override
    public long removeEntries(ObsFilter filter) {
        batchLock.lock();
        try {
            return super.removeEntries(filter);
        } finally {
            batchLock.unlock();
        }
    }
}
