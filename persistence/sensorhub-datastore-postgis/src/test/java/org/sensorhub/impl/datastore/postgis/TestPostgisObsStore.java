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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.datastore.AbstractTestObsStore;
import org.sensorhub.impl.datastore.postgis.store.obs.PostgisDataStreamStoreImpl;
import org.sensorhub.impl.datastore.postgis.store.obs.PostgisObsStoreImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Stream;

public class TestPostgisObsStore extends AbstractTestObsStore<PostgisObsStoreImpl> {
    protected static String OBS_DATASTORE_NAME = "test_obs";
    private static String DB_NAME = "gis";
    private final String url = "localhost:5432";
    private final String login = "postgres";
    private final String password = "postgres";
    private static final boolean USE_BATCH = true;

    protected PostgisObsStoreImpl postgisObsStore;

    @Test
    public void testGetDatastoreName() throws Exception
    {
        assertEquals(OBS_DATASTORE_NAME, obsStore.getDatastoreName());
        forceReadBackFromStorage();
    }

    protected PostgisObsStoreImpl initStore() throws Exception {
        this.postgisObsStore = new PostgisObsStoreImpl(url, DB_NAME, login, password, OBS_DATASTORE_NAME, DATABASE_NUM, IdProviderType.SEQUENTIAL, USE_BATCH);
        return this.postgisObsStore;
    }

    @Override
    protected void forceReadBackFromStorage() {
        try {
            this.postgisObsStore.getDataStreams().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSelectObsByMultipleIDs() throws Exception
    {
        Stream<Map.Entry<BigId, IObsData>> resultStream;
        ObsFilter filter;

        var dsKey = addSimpleDataStream(bigId(10), "out1");
        var startTime1 = Instant.parse("-3050-02-11T00:00:00Z");
        var obsBatch1 = addSimpleObsWithoutResultTime(dsKey, BigId.NONE, startTime1, 25, 1000);
        var startTime2 = Instant.parse("0001-05-31T10:40:00Z");
        var obsBatch2 = addSimpleObsWithoutResultTime(dsKey, BigId.NONE, startTime2, 63, 2500);

        forceReadBackFromStorage();

        // all from batch 1
        filter = new ObsFilter.Builder()
                .withInternalIDs(obsBatch1.keySet())
                .build();
        resultStream = obsStore.selectEntries(filter);
        checkSelectedEntries(resultStream, obsBatch1, filter);

        forceReadBackFromStorage();

        // all from batch 2
        filter = new ObsFilter.Builder()
                .withInternalIDs(obsBatch2.keySet())
                .build();
        resultStream = obsStore.selectEntries(filter);
        checkSelectedEntries(resultStream, obsBatch2, filter);

        // one from each batch
        filter = new ObsFilter.Builder()
                .withInternalIDs(
                        Iterators.get(obsBatch1.keySet().iterator(), 3),
                        Iterators.get(obsBatch2.keySet().iterator(), 10))
                .build();
        resultStream = obsStore.selectEntries(filter);
        var expectedResults = ImmutableMap.<BigId, IObsData>builder()
                .put(Iterators.get(obsBatch1.entrySet().iterator(), 3))
                .put(Iterators.get(obsBatch2.entrySet().iterator(), 10))
                .build();
        checkSelectedEntries(resultStream, expectedResults, filter);

        // invalid IDs
        var ids = BigId.fromLongs(DATABASE_NUM, 500, 120, 530, 760);
        filter = new ObsFilter.Builder()
                .withInternalIDs(ids)
                .build();
        resultStream = obsStore.selectEntries(filter);
        expectedResults = ImmutableMap.copyOf(Maps.filterKeys(allObs, k -> ids.contains(k)));
        assertTrue(expectedResults.isEmpty());
        checkSelectedEntries(resultStream, expectedResults, filter);
    }


    @After
    public void cleanup() throws Exception {
        postgisObsStore.drop();
        if(postgisObsStore != null && postgisObsStore.getDataStreams() != null) {
            ((PostgisDataStreamStoreImpl)postgisObsStore.getDataStreams()).drop();
            ((PostgisDataStreamStoreImpl)postgisObsStore.getDataStreams()).clearCache();
        }
    }


    @Override
    @Test
    public void testGetWrongKey() throws Exception
    {
        testGetNumRecordsOneDataStream();
        assertNull(obsStore.get(bigId(Integer.MAX_VALUE)));
    }

    @Test
    public void testAddAndCheckMapKeys() throws Exception
    {
        var dsID = addSimpleDataStream(bigId(10), "out1");
        addSimpleObsWithoutResultTime(dsID, BigId.NONE, Instant.parse("2000-01-01T00:00:00Z"), 100);
        forceReadBackFromStorage();
        checkMapKeySet(obsStore.keySet());

        dsID = addSimpleDataStream(bigId(10), "out2");
        addSimpleObsWithoutResultTime(dsID, BigId.NONE, Instant.parse("-4700-01-01T00:00:00Z").plusSeconds(1), 11);
        addSimpleObsWithoutResultTime(dsID, BigId.NONE, Instant.parse("-4700-01-01T00:00:00Z").minus(10, ChronoUnit.DAYS), 11);
        forceReadBackFromStorage();
        checkMapKeySet(obsStore.keySet());

        dsID = addSimpleDataStream(bigId(456), "output");
        addSimpleObsWithoutResultTime(dsID, bigId(569), Instant.parse("1950-01-01T00:00:00.5648712Z"), 56);
        forceReadBackFromStorage();
        checkMapKeySet(obsStore.keySet());
    }


}
