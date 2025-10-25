/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis;

import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.IdProvider;
import org.sensorhub.api.datastore.command.ICommandStatusStore;
import org.sensorhub.api.datastore.command.ICommandStore;
import org.sensorhub.api.datastore.command.ICommandStreamStore;
import org.sensorhub.api.datastore.deployment.IDeploymentStore;
import org.sensorhub.api.datastore.feature.IFeatureStore;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.procedure.IProcedureStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.builder.QueryBuilder;
import org.sensorhub.impl.datastore.postgis.utils.PostgisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class PostgisStore<T extends QueryBuilder> {
    private static final Logger logger = LoggerFactory.getLogger(PostgisStore.class);

    protected ConnectionManager connectionManager;
    protected T queryBuilder;
    public int idScope;
    public IdProviderType idProviderType;
    protected  IdProvider idProvider;
    protected AtomicLong lastId = new AtomicLong(0);
    private TimerTask timerTask;

    protected int maxBatchSize = 100;
    protected AtomicInteger currentBatchSize = new AtomicInteger(0);
    protected boolean useBatch;
    private long autoCommitPeriod = 3600*1000L;
    public static final int STREAM_FETCH_SIZE = 1000;

    protected PostgisStore(int idScope, IdProviderType dsIdProviderType, T queryBuilder, boolean useBatch) {
        this.idProviderType = dsIdProviderType;
        this.idScope = idScope;
        this.queryBuilder = queryBuilder;
        this.useBatch = useBatch;
    }

    protected  void setBatchSize(int size) {
        this.maxBatchSize = size;
    }

    protected int getBatchSize() {
        return maxBatchSize;
    }

    protected void init(String url, String dbName, String login, String password, String[] initScripts) {
        this.connectionManager = new ConnectionManager(url, dbName, login, password);
        this.initStore(initScripts);
    }

    private void initStore(String[] initScripts) {
        try (Connection connection = this.connectionManager.getConnection()) {
            if (!PostgisUtils.checkTable(connection, queryBuilder.getStoreTableName())) {
                // create table
                PostgisUtils.executeQueries(connection, initScripts);
            }
            logger.info("Initialized store '{}'", queryBuilder.getStoreTableName());
            try(Statement statement = connection.createStatement()) {
                try(ResultSet resultSet = statement.executeQuery(queryBuilder.selectLastIdQuery())) {
                    if (resultSet.next()) {
                        lastId = new AtomicLong(resultSet.getLong("id"));
                    }
                    idProvider = (obj) -> lastId.incrementAndGet();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if(this.useBatch) {
            this.initAutoCommitTask();
        }
    }
    protected void initAutoCommitTask() {
        if(timerTask != null) {
            timerTask.cancel();
        }

        Timer t = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    commit();
                } catch (DataStoreException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        t.scheduleAtFixedRate(timerTask, 0,autoCommitPeriod * 1000L);
    }
    public void backup(OutputStream is) throws IOException {
        try(Connection connection = this.connectionManager.getConnection()) {
            if (PostgisUtils.checkTable(connection, queryBuilder.getStoreTableName())) {
                // cloning table to _backup
                PostgisUtils.executeQueries(connection, new String[]{
                        queryBuilder.cloningTableQuery()
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void restore(InputStream os) throws IOException {
        try(Connection connection = this.connectionManager.getConnection()) {
            if (PostgisUtils.checkTable(connection, queryBuilder.getStoreTableName() + "_backup")) {
                // restoring table
                PostgisUtils.executeQueries(connection, new String[]{
                        queryBuilder.dropQuery(),
                        queryBuilder.restoringTableQuery(),
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void clear() {
        try(Connection connection = this.connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(queryBuilder.clearQuery());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void drop() {
        try {
            // be sure to commit everything before dropping the table to avoid LOCK
            commit();
        } catch (DataStoreException e) {
            throw new RuntimeException(e);
        }
        try(Connection connection = this.connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(queryBuilder.dropQuery());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    public String getDatastoreName() {
        return this.queryBuilder.getStoreTableName();
    }

    public boolean isEmpty() {
        return this.getNumRecords() == 0;
    }


    public long getNumRecords() {
        try(Connection connection = this.connectionManager.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(queryBuilder.countQuery())) {
                    if (resultSet.next()) {
                        return resultSet.getLong("recordsCount");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    public void close() {
        try {
            commit();

            if(timerTask != null) {
                timerTask.cancel();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void commit() throws DataStoreException {
        if(this.useBatch) {
            try {
                synchronized (currentBatchSize) {
                    if (currentBatchSize.get() >= getBatchSize()) {
                        this.connectionManager.commit();
                        currentBatchSize.getAndSet(0);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected void linkTo(ISystemDescStore systemStore) { queryBuilder.linkTo(systemStore); }
    protected void linkTo(IObsStore obsStore) { queryBuilder.linkTo(obsStore); }
    protected void linkTo(IFeatureStore featureStore) { queryBuilder.linkTo(featureStore); }
    protected void linkTo(IDataStreamStore dataStreamStore) {
        queryBuilder.linkTo(dataStreamStore);
    }
    protected void linkTo(IProcedureStore procedureStore) { queryBuilder.linkTo(procedureStore); }
    protected void linkTo(IDeploymentStore deploymentStore) {
        queryBuilder.linkTo(deploymentStore);
    }
    protected void linkTo(ICommandStreamStore commandStreamStore) { queryBuilder.linkTo(commandStreamStore); }
    protected void linkTo(ICommandStore commandStore) { queryBuilder.linkTo(commandStore); }
    protected void linkTo(IFoiStore foiStore) { queryBuilder.linkTo(foiStore); }
    protected void linkTo(ICommandStatusStore commandStatusStore) { queryBuilder.linkTo(commandStatusStore); }

    public void setAutoCommitPeriod(long autoCommitPeriod) {
        this.autoCommitPeriod = autoCommitPeriod;
        if(this.useBatch) {
            this.initAutoCommitTask();
        }
    }
}
