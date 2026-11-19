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

import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.obs.ObsStats;
import org.sensorhub.api.datastore.obs.ObsStatsQuery;
import org.sensorhub.api.datastore.obs.IObsStore.ObsField;
import org.sensorhub.impl.datastore.DataStoreUtils;
import org.sensorhub.impl.datastore.ReadOnlyDataStore;
import org.slf4j.Logger;
import org.vast.util.Asserts;


public class USGSObsStore extends ReadOnlyDataStore<BigId, IObsData, ObsField, ObsFilter> implements IObsStore
{
    final int idScope;
    final USGSDataFilter configFilter;
    final IParamDatabase paramDb;
    final Logger logger;
    final IDataStreamStore dataStreamStore;
    

    public USGSObsStore(int idScope, USGSDataFilter configFilter, IParamDatabase paramDb, Logger logger)
    {
        this.idScope = idScope;
        this.configFilter = Asserts.checkNotNull(configFilter, USGSDataFilter.class);
        this.paramDb = Asserts.checkNotNull(paramDb, IParamDatabase.class);
        this.logger = Asserts.checkNotNull(logger, Logger.class);
        this.dataStreamStore = new USGSDataStreamStore(idScope, configFilter, paramDb, logger);
    }


    @Override
    public Stream<Entry<BigId, IObsData>> selectEntries(ObsFilter filter, Set<ObsField> fields)
    {
        // convert obs filter to USGS filter
        var queryFilter = UsgsUtils.from(filter);
        Instant oldestData = Instant.MAX;
        
        // if specific datastreams are requested, lookup site num and param code
        Set<BigId> dataStreamIds = new TreeSet<>();
        if (filter.getDataStreamFilter() != null)
        {
            var dsStream = DataStoreUtils.selectDataStreamIDs(dataStreamStore, filter.getDataStreamFilter());
            var it = dsStream.iterator();
            if (!it.hasNext())
                return Stream.empty();
            
            while (it.hasNext())
            {
                var dsID = it.next();
                var dsInfo = (USGSTimeSeriesInfo)dataStreamStore.get(new DataStreamKey(dsID));
                queryFilter.siteIds.add(dsInfo.siteNum);
                queryFilter.otherParamCodes.add(dsInfo.paramCd);
                
                // record oldest available data
                var dsBegin = dsInfo.getPhenomenonTimeRange().begin();
                if (oldestData.isAfter(dsBegin))
                    oldestData = dsBegin;
                
                dataStreamIds.add(dsID);
            }
        }
        else
            oldestData = Instant.parse("1990-10-01T00:00:00Z");
        
        // adjust start time so it's not older than oldestData
        var oldestTs = oldestData.toEpochMilli();
        if (queryFilter.startTime != null && queryFilter.startTime.getTime() < oldestTs)
            queryFilter.startTime = new Date(oldestTs);
        
        // AND with config filter
        queryFilter = UsgsUtils.and(configFilter, queryFilter);
        
        // get observation stream
        var results = new ObsRecordLoader(idScope, paramDb, logger).getObservations(queryFilter, filter.getLimit());
        
        // post-filter on datastream IDs
        if (!dataStreamIds.isEmpty())
            results = results.filter(e -> dataStreamIds.contains(e.getValue().getDataStreamID()));
        
        // post-filter using original datastore filter
        return results
            .filter(e -> filter.test(e.getValue()))
            .limit(filter.getLimit());
    }
    
    
    @Override
    public long countMatchingEntries(ObsFilter filter)
    {
        return 10000L;
    }


    @Override
    public IObsData get(Object key)
    {
        return null;
    }


    @Override
    public Stream<ObsStats> getStatistics(ObsStatsQuery query)
    {
        return Stream.empty();
    }


    @Override
    public IDataStreamStore getDataStreams()
    {
        return dataStreamStore;
    }


    @Override
    public String getDatastoreName()
    {
        return "USGS Water Network Observation Store";
    }


    @Override
    public void linkTo(IFoiStore foiStore)
    {
    }


    @Override
    public BigId add(IObsData obs)
    {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MSG);
    }

}
