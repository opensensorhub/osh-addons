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

import java.util.AbstractMap.SimpleEntry;
import java.util.Set;
import java.util.stream.Stream;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.feature.IFeatureStore;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.feature.IFoiStore.FoiField;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.procedure.IProcedureStore;
import org.sensorhub.impl.datastore.DataStoreUtils;
import org.sensorhub.impl.datastore.ReadOnlyDataStore;
import org.slf4j.Logger;
import org.vast.ogc.gml.IGeoFeature;
import org.vast.util.Asserts;
import org.vast.util.Bbox;


public class USGSFoiStore extends ReadOnlyDataStore<FeatureKey, IGeoFeature, FoiField, FoiFilter> implements IFoiStore
{
    final Logger logger;
    final USGSDataFilter configFilter;
    final IParamDatabase paramDb;
    IProcedureStore procStore;
    
    
    public USGSFoiStore(USGSDataFilter configFilter, IParamDatabase paramDb, Logger logger)
    {
        this.configFilter = Asserts.checkNotNull(configFilter, USGSDataFilter.class);
        this.paramDb = Asserts.checkNotNull(paramDb, IParamDatabase.class);
        this.logger = Asserts.checkNotNull(logger, Logger.class);
    }
    
    
    @Override
    public Bbox getFeaturesBbox()
    {
        return null;
    }


    @Override
    public IGeoFeature get(Object key)
    {
        var fk = DataStoreUtils.checkFeatureKey(key);
        
        var usgsFilter = new USGSDataFilter();
        usgsFilter.siteIds.add(FilterUtils.toSiteCode(fk.getInternalID()));
        var results = new ObsSiteLoader(paramDb, logger).getSites(usgsFilter);
        
        return results.findFirst().orElse(null);
    }


    @Override
    public Stream<Entry<FeatureKey, IGeoFeature>> selectEntries(FoiFilter filter, Set<FoiField> fields)
    {
        if (filter.getParentFilter() != null)
        {
            var parentStream = DataStoreUtils.selectProcedureIDs(procStore, filter.getParentFilter());
            if (parentStream.findAny().isEmpty())
                return Stream.empty();
        }
        
        // convert FOI filter to USGS filter
        var queryFilter = FilterUtils.from(filter);
        
        // AND with config filter
        queryFilter = FilterUtils.and(configFilter, queryFilter);
        
        // get list of sites
        var results = new ObsSiteLoader(paramDb, logger).getSites(queryFilter);
        
        // stream results
        // post-filter using original FOI filter
        return results
            .filter(filter)
            .limit(filter.getLimit())
            .map(f -> {
               var fk = new FeatureKey(FilterUtils.toLongId(f.getId()));
               return new SimpleEntry<>(fk, f);
            });
    }
    
    
    @Override
    public Long getParent(long internalID)
    {
        return null;
    }


    @Override
    public String getDatastoreName()
    {
        return "USGS Water Network FOI Store";
    }


    @Override
    public void linkTo(IProcedureStore procStore)
    {
        this.procStore = Asserts.checkNotNull(procStore, IProcedureStore.class);
    }


    @Override
    public void linkTo(IObsStore obsStore)
    {
    }


    @Override
    public void linkTo(IFeatureStore featureStore)
    {
    }


    @Override
    public FeatureKey add(long parentID, IGeoFeature value) throws DataStoreException
    {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MSG);
    }

}
