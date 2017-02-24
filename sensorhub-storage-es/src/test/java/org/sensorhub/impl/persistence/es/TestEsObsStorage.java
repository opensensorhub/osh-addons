/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.es;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.sensorhub.test.persistence.AbstractTestObsStorage;


public class TestEsObsStorage extends AbstractTestObsStorage<ESObsStorageImpl>
{
    
    @Before
    public void init() throws Exception
    {
        ESBasicStorageConfig config = new ESBasicStorageConfig();
        config.autoStart = true;
        config.storagePath = "elastic-cluster";
        List<String> nodes = new ArrayList<String>();
        nodes.add("localhost:9300");
        
        config.nodeUrls = nodes;
        config.scrollFetchSize = 2;
        config.scrollMaxDuration = 999999000;
        config.id = "junit_"+UUID.randomUUID().toString();
        storage = new ESObsStorageImpl();
        storage.init(config);
        storage.start();
    }
    

    @Override
    protected void forceReadBackFromStorage() throws Exception
    {
    	storage.commit();
    }
    
    
    @After
    public void cleanup()
    {
    }
    
}
