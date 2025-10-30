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
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.datastore.AbstractTestFeatureStore;
import org.sensorhub.impl.datastore.postgis.feature.PostgisFeatureStoreImpl;
import org.vast.ogc.gml.GenericTemporalFeatureImpl;

import javax.xml.namespace.QName;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestPostgisFeatureStore extends AbstractTestFeatureStore<PostgisFeatureStoreImpl> {
    protected static String FEATURE_DATASTORE_NAME = "test_features";
    private static String DB_NAME = "gis";
    private final String url = "localhost:5432";
    private final String login = "postgres";
    private final String password = "postgres";
    private static final boolean USE_BATCH = true;
    protected PostgisFeatureStoreImpl initStore() throws Exception {
        return new PostgisFeatureStoreImpl(url, DB_NAME, login, password, FEATURE_DATASTORE_NAME, DATABASE_NUM, IdProviderType.SEQUENTIAL, USE_BATCH);
    }


    @Test
    @Override
    public void testGetDatastoreName() throws Exception
    {
        assertEquals(FEATURE_DATASTORE_NAME, featureStore.getDatastoreName());
        forceReadBackFromStorage();
        assertEquals(FEATURE_DATASTORE_NAME, featureStore.getDatastoreName());
    }

    protected void forceReadBackFromStorage() {
        try {
            featureStore.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        featureStore.clearCache();
    }

    @After
    public void cleanup() {
        try {
            featureStore.drop();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            featureStore.clearCache();
            featureStore.close();
        }
    }

    @Test
    @Override
    public void testScanThroughput() throws Exception {}

    @Test
    @Override
    public void testPutThroughput() throws Exception {}

    @Test
    @Override
    public void testGetThroughput() throws Exception {}

    @Test
    @Override
    public void testTemporalFilterThroughput() throws Exception {}

    @Test
    @Override
    public void testSpatialFilterThroughput() throws Exception {}

    protected void addTemporalFeatures(BigId parentID, int startIndex, int numFeatures, OffsetDateTime startTime, boolean endNow) throws Exception
    {
        QName fType = new QName("http://mydomain/features", "MyTimeFeature");
        var now = Instant.now().atOffset(ZoneOffset.UTC);

        long t0 = System.currentTimeMillis();
        for (int i = startIndex; i < startIndex+numFeatures; i++)
        {
            // add feature with 5 different time periods
            for (int j = 0; j < NUM_TIME_ENTRIES_PER_FEATURE; j++)
            {
                GenericTemporalFeatureImpl f = new GenericTemporalFeatureImpl(fType);
                OffsetDateTime beginTime = startTime.plus(j*30, ChronoUnit.DAYS).plus(i, ChronoUnit.HOURS);
                OffsetDateTime endTime = endNow && now.isAfter(beginTime) ? now : beginTime.plus(30, ChronoUnit.DAYS);
                f.setValidTimePeriod(beginTime.truncatedTo(ChronoUnit.SECONDS), endTime.truncatedTo(ChronoUnit.SECONDS));
                setCommonFeatureProperties(f, i);
                addOrPutFeature(parentID, f);
            }
        }
        long t1 = System.currentTimeMillis();

        System.out.println("Inserted " + numFeatures + " temporal features in " + (t1-t0) + "ms" +
                " starting at #" + startIndex);
    }
}
