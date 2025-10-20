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
import org.sensorhub.impl.datastore.AbstractTestFeatureStore;
import org.sensorhub.impl.datastore.postgis.feature.PostgisFeatureStoreImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestPostgisFeatureStore extends AbstractTestFeatureStore<PostgisFeatureStoreImpl> {
    private static String DB_NAME = "gis";

    private final String url = "localhost:5432";
    private final String login = "postgres";

    private final String password = "postgres";

    protected PostgisFeatureStoreImpl postgisFeatureStore;
    protected static String FEATURE_DATASTORE_NAME = "test_features";

    protected PostgisFeatureStoreImpl initStore() throws Exception {
        postgisFeatureStore =  new PostgisFeatureStoreImpl(url, DB_NAME, login, password, FEATURE_DATASTORE_NAME, DATABASE_NUM, IdProviderType.SEQUENTIAL);
        return postgisFeatureStore;
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
        postgisFeatureStore.clearCache();
    }

    @After
    public void cleanup() {
        try {
            postgisFeatureStore.close();
            postgisFeatureStore.drop();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            postgisFeatureStore.clearCache();
        }
    }
}
