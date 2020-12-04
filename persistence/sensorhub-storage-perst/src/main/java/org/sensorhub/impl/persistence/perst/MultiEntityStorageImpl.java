/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import java.util.Collection;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsStorage;


/**
 * <p>
 * PERST implementation of {@link IMultiSourceStorage} module.
 * </p>
 *
 * @author Alex Robin
 * @since May 25, 2015
 */
public class MultiEntityStorageImpl extends ObsStorageImpl implements IMultiSourceStorage<IObsStorage>
{
        
    @Override
    protected Persistent createRoot(Storage db)
    {
        MultiEntityStorageRoot dbRoot = new MultiEntityStorageRoot(db);
        return dbRoot;
    }
    
    
    @Override
    public Collection<String> getProducerIDs()
    {
        return ((MultiEntityStorageRoot)dbRoot).getProducerIDs();
    }


    @Override
    public IObsStorage getDataStore(String producerID)
    {
        return ((MultiEntityStorageRoot)dbRoot).getDataStore(producerID);
    }
    
    
    @Override
    public synchronized IObsStorage addDataStore(String producerID)
    {
        IObsStorage dataStore = ((MultiEntityStorageRoot)dbRoot).addDataStore(producerID);
        if (autoCommit)
            commit();
        
        return dataStore;
    }
}
