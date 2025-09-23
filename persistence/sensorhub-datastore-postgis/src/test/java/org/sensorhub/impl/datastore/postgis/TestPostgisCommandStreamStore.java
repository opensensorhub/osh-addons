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
import org.junit.Test;
import org.junit.runner.OrderWith;
import org.junit.runner.manipulation.Alphanumeric;
import org.sensorhub.impl.datastore.AbstractTestCommandStreamStore;
import org.sensorhub.impl.datastore.postgis.command.PostgisCommandStatusStore;
import org.sensorhub.impl.datastore.postgis.command.PostgisCommandStoreImpl;
import org.sensorhub.impl.datastore.postgis.command.PostgisCommandStreamStoreImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

@OrderWith(Alphanumeric.class)
public class TestPostgisCommandStreamStore extends AbstractTestCommandStreamStore<PostgisCommandStreamStoreImpl> {
    protected static String COMMAND_STREAM_DATASTORE_NAME = "test_command";
    private static String DB_NAME = "gis";
    private final String url = "localhost:5432";
    private final String login = "postgres";
    private final String password = "postgres";

    protected PostgisCommandStoreImpl postgisCommandStore;
    //    @Override
    protected PostgisCommandStreamStoreImpl initStore() throws Exception {
        postgisCommandStore = new PostgisCommandStoreImpl(url, DB_NAME, login, password, COMMAND_STREAM_DATASTORE_NAME, DATABASE_NUM, IdProviderType.SEQUENTIAL);
        return (PostgisCommandStreamStoreImpl) postgisCommandStore.getCommandStreams();
    }

    @Before
    public void setup() throws Exception {
        Thread.sleep(2000);
    }

    @Test
    public void test1AddAndSelectCurrentVersion() throws Exception {
        super.testAddAndSelectCurrentVersion();
    }

    @Override
    public void testAddAndSelectCurrentVersion() {
        // Call this first so expected result not deleted by other test
    }

    protected void forceReadBackFromStorage() {}


    @After
    public void cleanup() throws Exception {
        postgisCommandStore.drop();
        ((PostgisCommandStreamStoreImpl)postgisCommandStore.getCommandStreams()).drop();
        ((PostgisCommandStatusStore)postgisCommandStore.getStatusReports()).drop();
    }
}
