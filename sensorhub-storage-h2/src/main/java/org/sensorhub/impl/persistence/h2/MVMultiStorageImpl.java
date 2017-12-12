/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.h2.mvstore.MVMap;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsStorage;


/**
 * <p>
 * H2 MVStore implementation of {@link IMultiSourceStorage} module.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 9, 2017
 */
public class MVMultiStorageImpl extends MVObsStorageImpl implements IMultiSourceStorage<IObsStorage>
{
    private static final String PRODUCERS_MAP_NAME = "@producers";

    MVMap<String, String> dataStoreInfoMap;
    Map<String, MVObsStorageImpl> obsStores;
    
    
    public MVMultiStorageImpl()
    {
        this.obsStores = new LinkedHashMap<>();
    }
    
    
    @Override
    public synchronized void start() throws SensorHubException
    {
        super.start();
        
        // load child data stores
        this.dataStoreInfoMap = mvStore.openMap(PRODUCERS_MAP_NAME, new MVMap.Builder<String, String>());
        for (String producerID: dataStoreInfoMap.keySet())
            loadDataStore(producerID);
    }
    
    
    private void loadDataStore(String producerID)
    {
        MVObsStorageImpl obsStore = new MVObsStorageImpl(mvStore, producerID);
        obsStores.put(producerID, obsStore);
    }
    
    
    @Override
    public Collection<String> getProducerIDs()
    {
        checkOpen();
        return Collections.unmodifiableSet(obsStores.keySet());
    }
    

    @Override
    public IObsStorage getDataStore(String producerID)
    {
        checkOpen();
        return obsStores.get(producerID);
    }
    

    @Override
    public synchronized IObsStorage addDataStore(String producerID)
    {
        checkOpen();
        dataStoreInfoMap.put(producerID, "");
        loadDataStore(producerID);
        return null;
    }
}
