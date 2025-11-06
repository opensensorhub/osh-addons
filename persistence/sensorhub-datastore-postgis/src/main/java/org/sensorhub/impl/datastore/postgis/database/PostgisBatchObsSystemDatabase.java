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
import org.sensorhub.impl.datastore.postgis.store.feature.PostgisDeploymentStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.feature.PostgisFoiStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.feature.PostgisProcedureStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.feature.PostgisSystemDescStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.obs.PostgisBatchObsStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.obs.PostgisObsStoreImpl;
import org.sensorhub.impl.module.AbstractModule;

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
public class PostgisBatchObsSystemDatabase extends PostgisObsSystemDatabase {
    protected PostgisObsStoreImpl getObsStore(String url, String dbName,String login,String password, String tableName,int idScope, IdProviderType idProviderType) {
        if(obsStore == null) {
            obsStore = new PostgisBatchObsStoreImpl(url, dbName, login, password, tableName, idScope, idProviderType);
        }
        return obsStore;
    }
}
