/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;
import org.h2.mvstore.MVBTreeMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.RangeCursor;
import org.sensorhub.api.datastore.FeatureKey;
import org.sensorhub.api.datastore.IFeatureFilter;
import org.sensorhub.api.datastore.IFeatureStore;
import org.sensorhub.api.datastore.RangeFilter;
import org.sensorhub.impl.datastore.h2.H2Utils;
import org.sensorhub.impl.datastore.h2.IdProvider;
import org.sensorhub.impl.datastore.h2.MVBaseFeatureStoreImpl;
import org.sensorhub.impl.datastore.h2.MVDataStoreInfo;
import org.sensorhub.impl.datastore.h2.MVVoidDataType;
import org.sensorhub.impl.service.sta.STALocationStoreTypes.MVLocationThingKeyDataType;
import org.sensorhub.impl.service.sta.STALocationStoreTypes.MVThingLocationKey;
import org.sensorhub.impl.service.sta.STALocationStoreTypes.MVThingLocationKeyDataType;
import org.vast.ogc.gml.GenericFeature;
import net.opengis.gml.v32.AbstractFeature;


/**
 * <p>
 * Extension of {@link MVBaseFeatureStoreImpl} for associating Location entities to Things
 * </p>
 *
 * @author Alex Robin
 * @date Oct 16, 2019
 */
class STALocationStoreImpl extends MVBaseFeatureStoreImpl<AbstractFeature> implements ILocationStore
{
    private static final String THING_LOCATIONS_MAP_NAME = "@thing_locations";
    private static final String LOCATION_THINGS_MAP_NAME = "@location_things";
    
    IFeatureStore<FeatureKey, GenericFeature> thingStore;
    MVBTreeMap<MVThingLocationKey, Boolean> thingTimeLocationIndex;
    MVBTreeMap<MVThingLocationKey, Boolean> locationTimeThingIndex;
    
    
    STALocationStoreImpl()
    {
    }
    
    
    public static STALocationStoreImpl open(STADatabase db, String dataStoreName)
    {
        MVDataStoreInfo dataStoreInfo = H2Utils.loadDataStoreInfo(db.getMVStore(), dataStoreName);
        var store = new STALocationStoreImpl().init(db.getMVStore(), dataStoreInfo, null);
        store.thingStore = db.getThingStore();
        return store;
    }
    
    
    public static STALocationStoreImpl create(STADatabase db, MVDataStoreInfo dataStoreInfo)
    {
        H2Utils.addDataStoreInfo(db.getMVStore(), dataStoreInfo);
        var store = new STALocationStoreImpl().init(db.getMVStore(), dataStoreInfo, null);
        store.thingStore = db.getThingStore();
        return store;
    }
    
    
    @Override
    protected STALocationStoreImpl init(MVStore mvStore, MVDataStoreInfo dataStoreInfo, IdProvider idProvider)
    {
        super.init(mvStore, dataStoreInfo, idProvider);
        
        // thing+time to location map
        // sorted by thing ID, then by time, then by location ID
        String mapName = THING_LOCATIONS_MAP_NAME + ":" + dataStoreInfo.getName();
        this.thingTimeLocationIndex = mvStore.openMap(mapName,
            new MVBTreeMap.Builder<MVThingLocationKey, Boolean>()
                .keyType(new MVThingLocationKeyDataType())
                .valueType(new MVVoidDataType()));
        
        // location+time to thing map
        // sorted by location ID, then by time, then by thing ID
        mapName = LOCATION_THINGS_MAP_NAME + ":" + dataStoreInfo.getName();
        this.locationTimeThingIndex = mvStore.openMap(mapName,
            new MVBTreeMap.Builder<MVThingLocationKey, Boolean>()
                .keyType(new MVLocationThingKeyDataType())
                .valueType(new MVVoidDataType()));
        
        return this;
    }
    
    
    public void addAssociation(long thingID, long locationID, Instant time)
    {
        var key = new MVThingLocationKey(thingID, locationID, time);
        thingTimeLocationIndex.put(key, Boolean.TRUE);
        locationTimeThingIndex.put(key, Boolean.TRUE);
    }
        
    
    Stream<FeatureKey> getCurrentLocationKeysByThing(long thingID)
    {
        var beforeLatest = new MVThingLocationKey(thingID, 0, Instant.MAX);
        var first = thingTimeLocationIndex.ceilingKey(beforeLatest);
        if (first == null || first.thingID != thingID)
            return Stream.empty();
        var last = new MVThingLocationKey(thingID, Integer.MAX_VALUE, first.time);
        var cursor = new RangeCursor<>(thingTimeLocationIndex, first, last);
        
        return cursor.keyStream()
            .map(k -> new FeatureKey(k.locationID));
    }


    @Override
    protected Stream<Entry<FeatureKey, AbstractFeature>> getIndexedStream(IFeatureFilter filter)
    {
        if (filter instanceof STALocationFilter)
        {
            var thingFilter = ((STALocationFilter)filter).getThings();
            if (thingFilter != null)
            {
                return thingStore.selectKeys(thingFilter)
                    .flatMap(k -> getCurrentLocationKeysByThing(k.getInternalID()))
                    .map(k -> featuresIndex.getEntry(k));
            }
        }
        
        return super.getIndexedStream(filter);
    }
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    Stream<IHistoricalLocation> getHistoricalLocationsByThing(long thingID, RangeFilter<Instant> timeRange)
    {
        MVThingLocationKey first, last;
        if (timeRange != null)
        {
            // time bounds are reversed since we are sorted by descending time stamp
            first = new MVThingLocationKey(thingID, 0, timeRange.getMax());
            last = new MVThingLocationKey(thingID, Integer.MAX_VALUE, timeRange.getMin());
        }
        else
        {
            // time bounds are reversed since we are sorted by descending time stamp
            first = new MVThingLocationKey(thingID, 0, Instant.MAX);
            last = new MVThingLocationKey(thingID, Integer.MAX_VALUE, Instant.MIN);
        }
        
        var cursor = new RangeCursor<>(thingTimeLocationIndex, first, last);        
        class Holder { Instant value = null; }
        var lastTime = new Holder();
        
        return (Stream)cursor.keyStream()
            .filter(k -> !Objects.equals(k.time, lastTime.value))
            .peek(k -> lastTime.value = k.time);  
    }
        
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Stream<IHistoricalLocation> selectHistoricalLocations(STALocationFilter filter)
    {
        if (filter.getThings() != null)
        {
            return thingStore.selectKeys(filter.getThings())
                .flatMap(k -> getHistoricalLocationsByThing(k.getInternalID(), filter.getValidTime()));
        }
        else
        {
            class Holder { Instant value = null; }
            var lastTime = new Holder();
            return (Stream)thingTimeLocationIndex.keyStream()
                .filter(k -> !Objects.equals(k.time, lastTime.value))
                .peek(k -> lastTime.value = k.time);
        }
    }
    
}
