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

import org.junit.After;
import org.junit.Before;
import org.sensorhub.impl.datastore.AbstractTestObsDatabase;
import org.sensorhub.impl.datastore.postgis.command.PostgisCommandStatusStore;
import org.sensorhub.impl.datastore.postgis.command.PostgisCommandStoreImpl;
import org.sensorhub.impl.datastore.postgis.command.PostgisCommandStreamStoreImpl;
import org.sensorhub.impl.datastore.postgis.feature.PostgisDeploymentStoreImpl;
import org.sensorhub.impl.datastore.postgis.feature.PostgisFoiStoreImpl;
import org.sensorhub.impl.datastore.postgis.feature.PostgisProcedureStoreImpl;
import org.sensorhub.impl.datastore.postgis.feature.PostgisSystemDescStoreImpl;
import org.sensorhub.impl.datastore.postgis.obs.PostgisDataStreamStoreImpl;
import org.sensorhub.impl.datastore.postgis.obs.PostgisObsStoreImpl;

public class TestPostgisObsSystemDatabase extends AbstractTestObsDatabase<PostgisObsSystemDatabase>  {
    private static String DB_NAME = "gis";

    private final String url = "localhost:5432";
    private final String login = "postgres";

    private final String password = "postgres";

    protected  PostgisObsSystemDatabase postgisObsSystemDatabase;

    @Override
    protected PostgisObsSystemDatabase initDatabase() throws Exception {
        postgisObsSystemDatabase = new PostgisObsSystemDatabase();
        var config = new PostgisObsSystemDatabaseConfig();
        config.databaseNum = 1;
        config.url = url;
        config.login = login;
        config.password = password;
        config.dbName = DB_NAME;

        postgisObsSystemDatabase.setConfiguration(config);
        postgisObsSystemDatabase.init();
        postgisObsSystemDatabase.start();
        return postgisObsSystemDatabase;
    }

    @Override
    protected void forceReadBackFromStorage() throws Exception {

    }

    @After
    public void cleanup() throws Exception {
        ((PostgisCommandStoreImpl)postgisObsSystemDatabase.getCommandStore()).drop();
        PostgisCommandStatusStore cmdStatusStore = (PostgisCommandStatusStore) postgisObsSystemDatabase.getCommandStatusStore();
        PostgisCommandStreamStoreImpl commandStreamStore = (PostgisCommandStreamStoreImpl) postgisObsSystemDatabase.getCommandStreamStore();
        cmdStatusStore.drop();
        commandStreamStore.drop();

        ((PostgisDeploymentStoreImpl)postgisObsSystemDatabase.getDeploymentStore()).drop();
        ((PostgisObsStoreImpl)postgisObsSystemDatabase.getObservationStore()).drop();

        PostgisDataStreamStoreImpl dataStreamStore = (PostgisDataStreamStoreImpl) postgisObsSystemDatabase.getObservationStore().getDataStreams();
        dataStreamStore.drop();


        ((PostgisObsStoreImpl)postgisObsSystemDatabase.getObservationStore()).drop();

        ((PostgisFoiStoreImpl)postgisObsSystemDatabase.getFoiStore()).drop();
        ((PostgisProcedureStoreImpl)postgisObsSystemDatabase.getProcedureStore()).drop();
        ((PostgisSystemDescStoreImpl)postgisObsSystemDatabase.getSystemDescStore()).drop();
    }
}
