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
import org.junit.Test;
import org.sensorhub.impl.datastore.AbstractTestCommandStore;
import org.sensorhub.impl.datastore.postgis.command.PostgisCommandStatusStore;
import org.sensorhub.impl.datastore.postgis.command.PostgisCommandStoreImpl;
import org.sensorhub.impl.datastore.postgis.command.PostgisCommandStreamStoreImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestPostgisCommandStore extends AbstractTestCommandStore<PostgisCommandStoreImpl> {
    protected static String COMMAND_DATASTORE_NAME = "test_command";

    private static String DB_NAME = "gis";

    private final String url = "localhost:5432";
    private final String login = "postgres";
    private final String password = "postgres";

    protected  PostgisCommandStoreImpl postgisCommandStore;
    //    @Override
    protected PostgisCommandStoreImpl initStore() throws Exception {
        postgisCommandStore = new PostgisCommandStoreImpl(url, DB_NAME, login, password, COMMAND_DATASTORE_NAME, DATABASE_NUM, IdProviderType.UID_HASH, false);
        return postgisCommandStore;
    }


    @Test
    @Override
    public void testGetDatastoreName() throws Exception
    {
        assertEquals(COMMAND_DATASTORE_NAME, cmdStore.getDatastoreName());
        forceReadBackFromStorage();
        assertEquals(COMMAND_DATASTORE_NAME, cmdStore.getDatastoreName());
    }

    protected void forceReadBackFromStorage() {

    }


    @After
    public void cleanup() throws Exception {
        postgisCommandStore.drop();
        ((PostgisCommandStreamStoreImpl)postgisCommandStore.getCommandStreams()).drop();
        ((PostgisCommandStatusStore)postgisCommandStore.getStatusReports()).drop();
    }

    @Test
    @Override
    public void testAddAndCheckMapKeys() throws Exception
    {
        var csKey = addSimpleCommandStream(bigId(10), "out1");
        addCommands(csKey.getInternalID(), Instant.parse("2000-01-01T00:00:00Z"), 10);
        checkMapKeySet(cmdStore.keySet());

        forceReadBackFromStorage();
        checkMapKeySet(cmdStore.keySet());

        csKey = addSimpleCommandStream(bigId(10), "out2");
        addCommands(csKey.getInternalID(), Instant.parse("-4700-01-01T00:00:00Z").plusSeconds(1), 11);
        addCommands(csKey.getInternalID(), Instant.parse("3000-01-01T00:00:00Z").minus(10, ChronoUnit.DAYS), 11);
        checkMapKeySet(cmdStore.keySet());

        csKey = addSimpleCommandStream(bigId(456), "output");
        addCommands(csKey.getInternalID(), Instant.parse("1950-01-01T00:00:00.5648712Z"), 56);
        checkMapKeySet(cmdStore.keySet());

        forceReadBackFromStorage();
        checkMapKeySet(cmdStore.keySet());
    }

    @Test
    public void testGetWrongKey() throws Exception
    {
        testGetNumRecordsOneDataStream();
        assertNull(cmdStore.get(bigId(1100L)));
    }

    @Test
    public void testSelectCommandsByDataStreamIDAndPredicates() throws Exception {
        super.testSelectCommandsByDataStreamIDAndPredicates();;
    }
}
