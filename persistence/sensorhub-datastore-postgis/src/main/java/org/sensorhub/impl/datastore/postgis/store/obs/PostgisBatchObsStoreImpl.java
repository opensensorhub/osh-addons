package org.sensorhub.impl.datastore.postgis.store.obs;

import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilderObsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgisBatchObsStoreImpl extends PostgisObsStoreImpl {
    private static final Logger logger = LoggerFactory.getLogger(PostgisBatchObsStoreImpl.class);
    public static final int BATCH_SIZE = 10000;

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
        this.connectionManager.enableBatch(BATCH_SIZE);
    }

    @Override
    public BigId add(IObsData obs) {
        DataStreamKey dataStreamKey = new DataStreamKey(obs.getDataStreamID());
        if (!dataStreamStore.containsKey(dataStreamKey))
            throw new IllegalStateException("Unknown datastream with ID: " + obs.getDataStreamID().getIdAsLong());

        BigId id  = BigId.fromLong(idScope, idProvider.newInternalID(obs));
        try {
            String sqlQuery = fillAddStatement(id, dataStreamKey,obs);
            this.connectionManager.addBatch(sqlQuery);
            this.connectionManager.tryCommit();
        } catch (Exception e) {
            throw new RuntimeException("Cannot insert obs=" + obs);
        }
        return id;
    }

    @Override
    public IObsData remove(Object o) {
        if (!(o instanceof BigId)) {
            throw new UnsupportedOperationException("Remove operation is not supported with argument != BigId key, got=" + o.getClass());
        }
        BigId key = (BigId) o;
        IObsData data = this.get(o);

        try {
            logger.debug("Remove Obs with key={}", key);
            String sqlQuery = queryBuilder.removeByIdQuery().replace("?",key.getIdAsLong()+"");
            this.connectionManager.addBatch(sqlQuery);
            this.connectionManager.tryCommit();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot remove obs " + data.toString());
        }
        return data;
    }
}
