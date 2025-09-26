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

import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.sensorhub.impl.datastore.AbstractTestDataStreamStore;
import org.sensorhub.impl.datastore.postgis.obs.PostgisDataStreamStoreImpl;
import org.sensorhub.impl.datastore.postgis.obs.PostgisObsStoreImpl;
import org.vast.swe.SWEUtils;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class TestPostgisDataStreamStore extends AbstractTestDataStreamStore<PostgisDataStreamStoreImpl> {
    protected static String OBS_DATASTORE_NAME = "test_obs";


    private static String DB_NAME = "gis";

    private final String url = "localhost:5432";
    private final String login = "postgres";
    private final String password = "postgres";

    protected PostgisObsStoreImpl postgisObsStore;
    //    @Override
    protected PostgisDataStreamStoreImpl initStore() throws Exception {
        postgisObsStore =  new PostgisObsStoreImpl(url, DB_NAME, login, password, OBS_DATASTORE_NAME, DATABASE_NUM, IdProviderType.SEQUENTIAL, false);
        return (PostgisDataStreamStoreImpl) postgisObsStore.getDataStreams();
    }


    protected void forceReadBackFromStorage() {

    }


    @After
    public void cleanup() throws Exception {
        postgisObsStore.drop();
        ((PostgisDataStreamStoreImpl)postgisObsStore.getDataStreams()).drop();
    }

    @Override
    protected void checkDataComponentEquals(DataComponent c1, DataComponent c2) {
        SWEUtils utils = new SWEUtils(SWEUtils.V2_0);
        ByteArrayOutputStream os1 = new ByteArrayOutputStream();
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();

        try
        {
            utils.writeComponent(os1, c1, false, false);
            utils.writeComponent(os2, c2, false, false);
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }

        assertArrayEquals(os1.toByteArray(), os2.toByteArray());
    }

}
