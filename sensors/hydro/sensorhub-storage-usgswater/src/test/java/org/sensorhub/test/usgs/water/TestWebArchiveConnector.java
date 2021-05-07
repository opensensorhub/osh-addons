/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.usgs.water;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.impl.usgs.water.CodeEnums.StateCode;
import org.sensorhub.impl.usgs.water.USGSWaterDataConfig;
import org.sensorhub.impl.usgs.water.USGSWaterDataArchive;
import org.vast.ogc.om.SamplingPoint;


public class TestWebArchiveConnector
{

    protected USGSWaterDataArchive initDatabase(USGSWaterDataConfig config) throws Exception
    {
        if (config == null)
            config = new USGSWaterDataConfig();
        
        USGSWaterDataArchive db = new USGSWaterDataArchive();
        config.id = "USGS_TEST_STORAGE";
        db.init(config);
        db.start();
        
        return db;
    }
    
    
    @Test
    public void testSelectFoisByKeywordsWithNoConfigFilter() throws Exception
    {
        var db = initDatabase(null);
        
        // query FOIs
        FoiFilter filter = new FoiFilter.Builder()
            .withKeywords("AL", "creek")
            .build();
        
        db.getFoiStore().select(filter).forEach(f ->{
            System.out.println(f.getUniqueIdentifier() + " -> " + f.getDescription() + " @ " + ((SamplingPoint)f).getShape());
            assertTrue(f.getDescription().contains("CREEK"));
        });
    }
    
    
    @Test
    public void testSelectFoisByKeywordsWithConfigFilterByState() throws Exception
    {
        USGSWaterDataConfig config = new USGSWaterDataConfig();
        config.exposeFilter.stateCodes.add(StateCode.AL);
        var db = initDatabase(config);
        
        // query FOIs
        FoiFilter filter = new FoiFilter.Builder()
            .withKeywords("huntsville")
            .build();
        
        db.getFoiStore().select(filter).forEach(f -> {
            System.out.println(f.getUniqueIdentifier() + " -> " + f.getDescription() + " @ " + ((SamplingPoint)f).getShape());
            assertTrue(f.getDescription().contains("HUNTSVILLE"));
        });
    }
    
    
    @Test
    public void testSelectFoisByIdWithNoConfigFilter() throws Exception
    {
        var db = initDatabase(null);
        
        // query FOIs
        var f = db.getFoiStore().get(new FeatureKey(2378170L));
        assertNotNull(f);
        System.out.println(f.getUniqueIdentifier() + " -> " + f.getDescription() + " @ " + ((SamplingPoint)f).getShape());
        assertEquals("02378170", f.getId());
    }
}
