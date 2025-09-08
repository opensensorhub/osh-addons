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
import org.sensorhub.impl.datastore.postgis.command.PostgisCommandStoreImpl;
import org.sensorhub.impl.datastore.postgis.feature.*;
import org.sensorhub.impl.datastore.postgis.obs.PostgisObsStoreImpl;
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
    final static String SYSTEM_TABLE_NAME = "sys";
    final static String FOI_TABLE_NAME = "foi";
    final static String OBS_TABLE_NAME = "obs";
    final static String CMD_TABLE_NAME = "cmd";
    final static String PROC_TABLE_NAME = "proc";
    final static String DEPLOY_TABLE_NAME = "deploy";

    PostgisCommandStoreImpl commandStore;
    PostgisObsStoreImpl obsStore;
    PostgisProcedureStoreImpl procedureStore;
    PostgisFoiStoreImpl foiStore;
    PostgisSystemDescStoreImpl systemDescStore;

    PostgisDeploymentStoreImpl deploymentStore;

    TimerTask timerTask;

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

            systemDescStore =  new PostgisSystemDescStoreImpl(url, dbName, login, password, SYSTEM_TABLE_NAME, idScope, idProviderType);
            deploymentStore = new PostgisDeploymentStoreImpl(url, dbName, login, password, DEPLOY_TABLE_NAME, idScope, idProviderType);
            foiStore = new PostgisFoiStoreImpl(url, dbName, login, password, FOI_TABLE_NAME, idScope, idProviderType);
            procedureStore = new PostgisProcedureStoreImpl(url, dbName, login, password, PROC_TABLE_NAME, idScope, idProviderType);
            obsStore = new PostgisObsStoreImpl(url, dbName, login, password, OBS_TABLE_NAME, idScope, idProviderType);
            commandStore = new PostgisCommandStoreImpl(url, dbName, login, password, CMD_TABLE_NAME, idScope, idProviderType);

            systemDescStore.linkTo(obsStore.getDataStreams());
            systemDescStore.linkTo(procedureStore);
            foiStore.linkTo(systemDescStore);
            foiStore.linkTo(obsStore);
            obsStore.linkTo(foiStore);
            obsStore.getDataStreams().linkTo(systemDescStore);
            commandStore.getCommandStreams().linkTo(systemDescStore);

            Timer t = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    obsStore.commit();
                }
            };

//            t.scheduleAtFixedRate(timerTask, 0,config.autoCommitPeriod * 1000L);

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
        obsStore.commit();
        timerTask.cancel();
        if (hasParentHub() && config.databaseNum != null)
            getParentHub().getDatabaseRegistry().unregister(this);
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
}
