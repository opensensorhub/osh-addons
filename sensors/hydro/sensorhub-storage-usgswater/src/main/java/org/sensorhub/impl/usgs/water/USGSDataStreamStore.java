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

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.procedure.IProcedureStore;
import org.sensorhub.api.datastore.obs.IDataStreamStore.DataStreamInfoField;
import org.sensorhub.api.obs.IDataStreamInfo;
import org.sensorhub.impl.datastore.DataStoreUtils;
import org.sensorhub.impl.datastore.ReadOnlyDataStore;
import org.slf4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


/*
 * Datastream keys are composed of:
 * - 32 MSB contain the timeseries ID
 * - 32 LSB contain the param ID
 */
public class USGSDataStreamStore extends ReadOnlyDataStore<DataStreamKey, IDataStreamInfo, DataStreamInfoField, DataStreamFilter> implements IDataStreamStore
{
    final Logger logger;
    final USGSDataFilter configFilter;
    final IParamDatabase paramDb;
    final Cache<DataStreamKey, IDataStreamInfo> dsCache;
    

    public USGSDataStreamStore(USGSDataFilter configFilter, IParamDatabase paramDb, Logger logger)
    {
        this.configFilter = configFilter;
        this.paramDb = paramDb;
        this.logger = logger;
        this.dsCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build();
    }


    @Override
    public IDataStreamInfo get(Object key)
    {
        var fk = DataStoreUtils.checkDataStreamKey(key);
        
        try
        {
            return dsCache.get(fk, () -> {
                // extract param ID
                long paramId = fk.getInternalID() & 0xFFFFFFFFL;
                
                // create query filter with param code
                // too bad we cannot filter by time series ID using the web service!
                var queryFilter = new USGSDataFilter();
                queryFilter.otherParamCodes.add(FilterUtils.toParamCode(paramId));
                
                // AND with config filter
                queryFilter = FilterUtils.and(configFilter, queryFilter);
                
                // stream results, making sure we return only the selected datastream
                // since server will return all datastreams with that param code
                var results = new ObsSeriesLoader(paramDb, logger).getSeries(queryFilter);
                return results
                    .filter(e -> e.getKey().getInternalID() == fk.getInternalID())
                    .findFirst()
                    .orElse(null)
                    .getValue();
            });
        }
        catch (ExecutionException e)
        {
            throw new IllegalStateException("Error fetching datastream " + fk, e);
        }
    }


    @Override
    public Stream<Entry<DataStreamKey, IDataStreamInfo>> selectEntries(DataStreamFilter filter, Set<DataStreamInfoField> fields)
    {
        // check if USGS procedures are selected
        if (filter.getProcedureFilter() != null)
        {
            var uniqueIDs = filter.getProcedureFilter().getUniqueIDs();
            if (uniqueIDs != null && !uniqueIDs.contains(USGSWaterDataArchive.UID_PREFIX + "network"))
                return Stream.empty();
            
            var internalIDs = filter.getProcedureFilter().getInternalIDs();
            if (internalIDs != null && !internalIDs.contains(1L))
                return Stream.empty();
        }        
        
        // convert datastream filter to USGS filter
        var queryFilter = FilterUtils.from(filter);
        
        // AND with config filter
        queryFilter = FilterUtils.and(configFilter, queryFilter);
        
        // get list of sites
        var results = new ObsSeriesLoader(paramDb, logger).getSeries(queryFilter);
        
        // stream results
        // post-filter using original datastore filter
        return results
            .filter(e -> filter.test(e.getValue()))
            .limit(filter.getLimit());
    }


    @Override
    public String getDatastoreName()
    {
        return "USGS Water Network Datastream Store";
    }


    @Override
    public void linkTo(IProcedureStore procedureStore)
    {
    }


    @Override
    public DataStreamKey add(IDataStreamInfo dsInfo) throws DataStoreException
    {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MSG);
    }
}
