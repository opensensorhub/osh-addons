/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Georobotix. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.postgis.database;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.database.IObsSystemDatabaseModule;
import org.sensorhub.api.database.IProcedureDatabase;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.command.ICommandStore;
import org.sensorhub.api.datastore.deployment.IDeploymentStore;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.procedure.IProcedureStore;
import org.sensorhub.api.datastore.property.IPropertyStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.impl.datastore.postgis.IdProviderType;
import org.sensorhub.impl.datastore.postgis.store.command.PostgisCommandStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.obs.PostgisBatchObsStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.obs.PostgisObsStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.feature.PostgisDeploymentStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.feature.PostgisFoiStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.feature.PostgisProcedureStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.feature.PostgisSystemDescStoreImpl;
import org.sensorhub.impl.module.AbstractModule;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;


/**
 * <p>
 * Implementation of the {@link IObsSystemDatabase} interface backed by
 * a Postgis database that contains all maps necessary to store observations,
 * commands, features of interest and system description history.
 * </p>
 *
 * @author Mathieu Dhainaut
 * @date Jul 25, 2023
 */
public class PostgisObsSystemDatabase extends AbstractModule<PostgisObsSystemDatabaseConfig> implements IObsSystemDatabase, IProcedureDatabase, IObsSystemDatabaseModule<PostgisObsSystemDatabaseConfig> {

    protected final static String SYSTEM_TABLE_NAME = "sys";
    protected final static String FOI_TABLE_NAME = "foi";
    protected final static String OBS_TABLE_NAME = "obs";
    protected final static String CMD_TABLE_NAME = "cmd";
    protected final static String PROC_TABLE_NAME = "proc";
    protected final static String DEPLOY_TABLE_NAME = "deploy";

    protected PostgisCommandStoreImpl commandStore;
    protected PostgisObsStoreImpl obsStore;
    protected PostgisProcedureStoreImpl procedureStore;
    protected PostgisFoiStoreImpl foiStore;
    protected PostgisSystemDescStoreImpl systemDescStore;
    protected PostgisDeploymentStoreImpl deploymentStore;

    protected TimerTask timerTask;

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

            systemDescStore =  this.createSystemStore(url, dbName, login, password, SYSTEM_TABLE_NAME, idScope, idProviderType, false);
            deploymentStore = this.createDeploymentStore(url, dbName, login, password, DEPLOY_TABLE_NAME, idScope, idProviderType, false);
            foiStore = this.createFoiStore(url, dbName, login, password, FOI_TABLE_NAME, idScope, idProviderType, config.useBatch);
            procedureStore = this.createProcedureStore(url, dbName, login, password, PROC_TABLE_NAME, idScope, idProviderType, false);
            obsStore = this.createObsStore(url, dbName, login, password, OBS_TABLE_NAME, idScope, idProviderType, config.useBatch);
            commandStore = this.createCommandStore(url, dbName, login, password, CMD_TABLE_NAME, idScope, idProviderType, false);

            systemDescStore.linkTo(obsStore.getDataStreams());
            systemDescStore.linkTo(procedureStore);
            foiStore.linkTo(systemDescStore);
            foiStore.linkTo(obsStore);
//            foiStore.linkTo(obsStore.getDataStreams());
            obsStore.linkTo(foiStore);
            obsStore.getDataStreams().linkTo(systemDescStore);
            commandStore.getCommandStreams().linkTo(systemDescStore);

            if(config.useBatch) {
                Timer t = new Timer();
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            obsStore.commit();
                            foiStore.commit();
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
        // Try to close every store separately
        try {
            obsStore.close();
        } catch (Exception ex) {
            getLogger().warn("Cannot close obsStore");
        }

        try {
            foiStore.close();
        } catch (Exception ex) {
            getLogger().warn("Cannot close foi");
        }

        try {
            deploymentStore.close();
        } catch (Exception ex) {
            getLogger().warn("Cannot close deploymentStore");
        }

        try {
            procedureStore.close();
        } catch (Exception ex) {
            getLogger().warn("Cannot close procedureStore");
        }

        try {
            systemDescStore.close();
        } catch (Exception ex) {
            getLogger().warn("Cannot close systemDescStore");
        }

        if (hasParentHub() && config.databaseNum != null)
            getParentHub().getDatabaseRegistry().unregister(this);

        if(timerTask != null) {
            timerTask.cancel();
        }
    }

    protected void closeStore() {

    }

    @Override
    protected void doStop() throws SensorHubException {
    }


    @Override
    public Integer getDatabaseNum() {
        return config.databaseNum;
    }


    @Override
    public ISystemDescStore getSystemDescStore() {
        checkStarted();
        return systemDescStore;
    }


    @Override
    public IObsStore getObservationStore() {
        checkStarted();
        return obsStore;
    }


    @Override
    public IFoiStore getFoiStore() {
        checkStarted();
        return foiStore;
    }


    @Override
    public ICommandStore getCommandStore() {
        checkStarted();
        return commandStore;
    }

    @Override
    public IDeploymentStore getDeploymentStore() {
        return deploymentStore;
    }


    @Override
    public IProcedureStore getProcedureStore() {
        checkStarted();
        return procedureStore;
    }

    @Override
    public IPropertyStore getPropertyStore() {
        return null;
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

    protected PostgisObsStoreImpl createObsStore(String url, String dbName,String login,String password, String tableName,
                                                 int idScope, IdProviderType idProviderType, boolean useBatch) {
        if(useBatch) {
            return new PostgisBatchObsStoreImpl(url, dbName, login, password, tableName, idScope, idProviderType);
        } else {
            return new PostgisObsStoreImpl(url, dbName, login, password, tableName, idScope, idProviderType);
        }
    }

    protected PostgisSystemDescStoreImpl createSystemStore(String url, String dbName,String login,String password, String tableName,
                                                 int idScope, IdProviderType idProviderType, boolean useBatch) {
        return new PostgisSystemDescStoreImpl(url, dbName, login, password, tableName, idScope, idProviderType, useBatch);
    }

    protected PostgisDeploymentStoreImpl createDeploymentStore(String url, String dbName,String login,String password, String tableName,
                                                     int idScope, IdProviderType idProviderType, boolean useBatch) {
        return new PostgisDeploymentStoreImpl(url, dbName, login, password, tableName, idScope, idProviderType, useBatch);
    }

    protected PostgisProcedureStoreImpl createProcedureStore(String url, String dbName,String login,String password, String tableName,
                                                   int idScope, IdProviderType idProviderType, boolean useBatch) {
        return new PostgisProcedureStoreImpl(url, dbName, login, password, tableName, idScope, idProviderType, useBatch);
    }

    protected PostgisFoiStoreImpl createFoiStore(String url, String dbName,String login,String password, String tableName,
                                       int idScope, IdProviderType idProviderType, boolean useBatch) {
        return new PostgisFoiStoreImpl(url, dbName, login, password, tableName, idScope, idProviderType, useBatch);
    }

    protected PostgisCommandStoreImpl createCommandStore(String url, String dbName,String login,String password, String tableName,
                                       int idScope, IdProviderType idProviderType, boolean useBatch) {
        return new PostgisCommandStoreImpl(url, dbName, login, password, tableName, idScope, idProviderType, useBatch);
    }
}
