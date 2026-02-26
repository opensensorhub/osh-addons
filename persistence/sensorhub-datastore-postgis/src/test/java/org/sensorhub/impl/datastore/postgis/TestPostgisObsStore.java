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

import org.sensorhub.api.data.ObsData;
import org.vast.data.DataBlockMixed;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Stream;

public class TestPostgisObsStore extends AbstractTestObsStore<PostgisObsStoreImpl> {
    protected static String OBS_DATASTORE_NAME = "test_obs";
    protected static String DB_NAME = "gis";
    protected final String url = "localhost:5432";
    protected final String login = "postgres";
    protected final String password = "postgres";

    protected PostgisObsStoreImpl postgisObsStore;

    @Test
    public void testGetDatastoreName() throws Exception
    {
        assertEquals(OBS_DATASTORE_NAME, obsStore.getDatastoreName());
        forceReadBackFromStorage();
    }

    protected PostgisObsStoreImpl initStore() throws Exception {
        this.postgisObsStore = new PostgisObsStoreImpl(url, DB_NAME, login, password, OBS_DATASTORE_NAME, DATABASE_NUM, IdProviderType.UID_HASH);
        return this.postgisObsStore;
    }

    @Override
    protected void forceReadBackFromStorage() {
        try {
            this.postgisObsStore.commit();
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
        var obsBatch1 = addSimpleObsWithoutResultTime(dsKey, BigId.NONE, startTime1, 1, 1000);
//        var startTime2 = Instant.parse("0001-05-31T10:40:00Z");
//        var obsBatch2 = addSimpleObsWithoutResultTime(dsKey, BigId.NONE, startTime2, 63, 2500);

        forceReadBackFromStorage();

        // all from batch 1
        filter = new ObsFilter.Builder()
                .withInternalIDs(obsBatch1.keySet())
                .build();
        resultStream = obsStore.selectEntries(filter);
        checkSelectedEntries(resultStream, obsBatch1, filter);

        forceReadBackFromStorage();

        /*
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

         */
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
    public void testInsertObsWithSpecialTextCharacters() throws Exception
    {
        // Create a datastream with a Text component
        SWEHelper fac = new SWEHelper();
        var recordStruct = fac.createRecord()
            .name("textOutput")
            .addField("value", fac.createQuantity().build())
            .addField("label", fac.createText().build())
            .build();

        var dsId = addDataStream(bigId(10), recordStruct);

        // Test strings with various special/malicious characters
        String[] testStrings = {
            "normal text",
            "text with \"double quotes\"",
            "text with 'single quotes'",
            "text with / forward slash",
            "text with \\ backslash",
            "text with ; semicolon",
            "text with -- sql comment",
            "'); DROP TABLE obs; --",
            "' OR '1'='1",
            "text with\ttab and\nnewline",
            "<script>alert('xss')</script>",
            "{\"json\": \"value\"}",
            "unicode: \u00e9\u00e0\u00fc\u00f1 \u2603 \u2764",
            "",
            "   spaces   ",
            "a'b\"c\\d/e;f--g",
        };

        Instant startTime = Instant.parse("2024-01-01T00:00:00Z");

        for (int i = 0; i < testStrings.length; i++)
        {
            var dataBlock = recordStruct.createDataBlock();
            dataBlock.setDoubleValue(0, i);
            dataBlock.setStringValue(1, testStrings[i]);

            ObsData obs = new ObsData.Builder()
                .withDataStream(dsId)
                .withFoi(BigId.NONE)
                .withPhenomenonTime(startTime.plusSeconds(i * 60))
                .withResult(dataBlock)
                .build();

            try {
                BigId key = addObservation(obs);
                assertNotNull("Observation with text '" + testStrings[i] + "' should have been inserted", key);
            } catch (Exception e) {
                fail("Failed to insert observation with text '" + testStrings[i] + "': " + e.getMessage());
            }
        }

        forceReadBackFromStorage();

        // Verify all observations can be retrieved
        var filter = new ObsFilter.Builder()
            .withDataStreams(dsId)
            .build();
        var resultStream = obsStore.selectEntries(filter);
        var results = resultStream.collect(java.util.stream.Collectors.toList());
        assertEquals("All observations with special characters should be retrievable",
            testStrings.length, results.size());

        for (var entry : results)
        {
            IObsData obs = entry.getValue();
            int idx = (int) obs.getResult().getDoubleValue(0);
            String retrievedText = obs.getResult().getStringValue(1);
            assertEquals("Text value should be same for " + idx,
                testStrings[idx], retrievedText);
        }
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
