package org.sensorhub.impl.datastore.postgis.database;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IFeatureDatabase;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.feature.IFeatureStore;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.store.feature.PostgisFeatureStoreImpl;
import org.sensorhub.impl.module.AbstractModule;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

public class PostgisFeatureDatabase extends AbstractModule<PostgisFeatureDatabaseConfig> implements IFeatureDatabase {
    final static String FEATURE_STORE_NAME = "feature_store";

    PostgisFeatureStoreImpl featureStore;
    private TimerTask timerTask;

    @Override
    protected void beforeInit() throws SensorHubException {
        super.beforeInit();
    }

    @Override
    protected void doStart() throws SensorHubException {
        try {
            String url = config.url;
            String login = config.login;
            String password = config.password;
            String dbName = config.dbName;
            IdProviderType idProviderType = config.idProviderType;

            var idScope = getDatabaseNum() != null ? getDatabaseNum() : 0;
            featureStore = new PostgisFeatureStoreImpl(url, dbName, login, password, FEATURE_STORE_NAME, idScope, idProviderType,config.useBatch);

            if(config.useBatch) {
                Timer t = new Timer();
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            featureStore.commit();
                        } catch (DataStoreException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

                t.scheduleAtFixedRate(timerTask, 0, config.autoCommitPeriod * 1000L);
            }
        } catch (Exception e) {
            throw new DataStoreException("Error while starting Postgis connector", e);
        }
    }


    @Override
    protected void afterStart() {
        if (hasParentHub() && config.databaseNum != null)
            getParentHub().getDatabaseRegistry().register(this);
    }


    @Override
    protected void beforeStop() {
        if (hasParentHub() && config.databaseNum != null)
            getParentHub().getDatabaseRegistry().unregister(this);
        if(timerTask != null) {
            timerTask.cancel();
        }
    }


    @Override
    protected void doStop() throws SensorHubException {
    }


    @Override
    public Integer getDatabaseNum() {
        return config.databaseNum;
    }


    @Override
    public void commit() {
        checkStarted();
    }


    @Override
    public <T> T executeTransaction(Callable<T> transaction) throws Exception {
        checkStarted();
        return null;
    }

    @Override
    public boolean isOpen() {
        return isStarted();
    }


    @Override
    public boolean isReadOnly() {
        checkStarted();
        return false;
    }

    @Override
    public IFeatureStore getFeatureStore() {
        checkStarted();
        return featureStore;
    }
}
