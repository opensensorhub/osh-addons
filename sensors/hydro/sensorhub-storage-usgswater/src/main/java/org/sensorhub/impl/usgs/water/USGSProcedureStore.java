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
import org.sensorhub.api.datastore.procedure.IProcedureStore;
import org.sensorhub.api.datastore.procedure.ProcedureFilter;
import org.sensorhub.api.datastore.procedure.IProcedureStore.ProcedureField;
import org.sensorhub.api.procedure.IProcedureWithDesc;
import org.sensorhub.impl.datastore.DataStoreUtils;
import org.sensorhub.impl.datastore.ReadOnlyDataStore;
import org.sensorhub.impl.procedure.wrapper.ProcedureWrapper;
import org.slf4j.Logger;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Asserts;
import org.vast.util.Bbox;


public class USGSProcedureStore extends ReadOnlyDataStore<FeatureKey, IProcedureWithDesc, ProcedureField, ProcedureFilter> implements IProcedureStore
{
    final Logger logger;
    final USGSDataFilter configFilter;
    final IParamDatabase paramDb;
    Entry<FeatureKey, IProcedureWithDesc> systemEntry;
    
    
    public USGSProcedureStore(USGSDataFilter configFilter, IParamDatabase paramDb, Logger logger)
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
        systemEntry = new SimpleEntry<>(new FeatureKey(1), new ProcedureWrapper(systemDesc));
    }
    
    
    @Override
    public Bbox getFeaturesBbox()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public IProcedureWithDesc get(Object key)
    {
        var fk = DataStoreUtils.checkFeatureKey(key);
        
        if (fk.getInternalID() == 1)
            return systemEntry.getValue();
        else
            return null;
    }


    @Override
    public Stream<Entry<FeatureKey, IProcedureWithDesc>> selectEntries(ProcedureFilter filter, Set<ProcedureField> fields)
    {
        if (filter.test(systemEntry.getValue()))
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
        return "USGS Water Network Procedure Store";
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
    public FeatureKey add(long parentID, IProcedureWithDesc value) throws DataStoreException
    {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MSG);
    }

}
