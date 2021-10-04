/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.usgs.water;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.Set;
import java.util.stream.Stream;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.datastore.system.ISystemDescStore.SystemField;
import org.sensorhub.api.system.ISystemWithDesc;
import org.sensorhub.impl.datastore.DataStoreUtils;
import org.sensorhub.impl.datastore.ReadOnlyDataStore;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.slf4j.Logger;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Asserts;
import org.vast.util.Bbox;


public class USGSSystemDescStore extends ReadOnlyDataStore<FeatureKey, ISystemWithDesc, SystemField, SystemFilter> implements ISystemDescStore
{
    final Logger logger;
    final USGSDataFilter configFilter;
    final IParamDatabase paramDb;
    Entry<FeatureKey, ISystemWithDesc> systemEntry;
    
    
    public USGSSystemDescStore(USGSDataFilter configFilter, IParamDatabase paramDb, Logger logger)
    {
        this.configFilter = Asserts.checkNotNull(configFilter, USGSDataFilter.class);
        this.paramDb = Asserts.checkNotNull(paramDb, IParamDatabase.class);
        this.logger = Asserts.checkNotNull(logger, Logger.class);
        
        // create parent network SML description
        SMLHelper sml = new SMLHelper();
        var systemDesc = sml.createPhysicalSystem()
            .uniqueID(USGSWaterDataArchive.UID_PREFIX + "network")
            .name("USGS Water Data Network")
            .description("USGS automated sensor network collecting water-resources data at sites across the US")
            .build();        
        systemEntry = new SimpleEntry<>(new FeatureKey(1), new SystemWrapper(systemDesc));
    }
    
    
    @Override
    public Bbox getFeaturesBbox()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public ISystemWithDesc get(Object key)
    {
        var fk = DataStoreUtils.checkFeatureKey(key);
        
        if (fk.getInternalID() == 1)
            return systemEntry.getValue();
        else
            return null;
    }


    @Override
    public Stream<Entry<FeatureKey, ISystemWithDesc>> selectEntries(SystemFilter filter, Set<SystemField> fields)
    {
        if ((filter.getInternalIDs() != null &&filter.getInternalIDs().contains(1L)) ||
            filter.test(systemEntry.getValue()))
            return Stream.of(systemEntry);
        else
            return Stream.empty();
    }
    
    
    @Override
    public Long getParent(long internalID)
    {
        return null;
    }


    @Override
    public String getDatastoreName()
    {
        return "USGS Water Network System Description Store";
    }


    @Override
    public void linkTo(IDataStreamStore dataStreamStore)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void backup(OutputStream is) throws IOException
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void restore(InputStream os) throws IOException
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public FeatureKey add(long parentID, ISystemWithDesc value) throws DataStoreException
    {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MSG);
    }

}
