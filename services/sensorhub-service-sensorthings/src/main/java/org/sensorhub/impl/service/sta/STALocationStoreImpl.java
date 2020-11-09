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
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.stream.Stream;
import org.h2.mvstore.MVBTreeMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.RangeCursor;
import org.sensorhub.api.datastore.RangeFilter;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.feature.IFeatureStoreBase.FeatureField;
import org.sensorhub.impl.datastore.h2.H2Utils;
import org.sensorhub.impl.datastore.h2.IdProvider;
import org.sensorhub.impl.datastore.h2.MVBaseFeatureStoreImpl;
import org.sensorhub.impl.datastore.h2.MVDataStoreInfo;
import org.sensorhub.impl.datastore.h2.MVFeatureParentKey;
import org.sensorhub.impl.datastore.h2.MVVoidDataType;
import org.sensorhub.impl.service.sta.STALocationStoreTypes.MVLocationThingKeyDataType;
import org.sensorhub.impl.service.sta.STALocationStoreTypes.MVThingLocationKey;
import org.sensorhub.impl.service.sta.STALocationStoreTypes.MVThingLocationKeyDataType;
import org.vast.ogc.gml.IFeature;
import org.vast.util.Asserts;
import net.opengis.gml.v32.AbstractFeature;


/**
 * <p>
 * Extension of {@link MVBaseFeatureStoreImpl} for associating Location
 * entities to Thing entities.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 16, 2019
 */
class STALocationStoreImpl extends MVBaseFeatureStoreImpl<AbstractFeature, FeatureField, STALocationFilter> implements ISTALocationStore
{
    private static final String THING_LOCATIONS_MAP_NAME = "@thing_locations";
    private static final String LOCATION_THINGS_MAP_NAME = "@location_things";
    
    ISTAThingStore thingStore;
    MVBTreeMap<MVThingLocationKey, Boolean> thingTimeLocationIndex;
    MVBTreeMap<MVThingLocationKey, Boolean> locationThingTimeIndex;
    
    
    STALocationStoreImpl()
    {
    }
    
    
    public static STALocationStoreImpl open(STADatabase db, MVDataStoreInfo dataStoreInfo)
    {
        return new STALocationStoreImpl().init(db.getMVStore(), dataStoreInfo, null);
    }
    
    
    @Override
    public synchronized FeatureKey add(long parentID, AbstractFeature feature)
    {
        Asserts.checkNotNull(feature, IFeature.class);
        
        long internalID = idProvider.newInternalID();
        var newKey = new MVFeatureParentKey(parentID, internalID);

        // add to store
        put(newKey, feature, false, false);
        return newKey;       
    }
    
    
    @Override
    public AbstractFeature put(FeatureKey key, AbstractFeature feature)
    {
        Asserts.checkNotNull(key, FeatureKey.class);
        Asserts.checkNotNull(feature, IFeature.class); 
        var fk = new MVFeatureParentKey(0L, key.getInternalID(), key.getValidStartTime());
        return put(fk, feature, true, true);
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
        // sorted by location ID, then by thing ID, then by time
        mapName = LOCATION_THINGS_MAP_NAME + ":" + dataStoreInfo.getName();
        this.locationThingTimeIndex = mvStore.openMap(mapName,
            new MVBTreeMap.Builder<MVThingLocationKey, Boolean>()
                .keyType(new MVLocationThingKeyDataType())
                .valueType(new MVVoidDataType()));
        
        return this;
    }
    
    
    public void addAssociation(long thingID, long locationID, Instant time)
    {
        var key = new MVThingLocationKey(thingID, locationID, time);
        thingTimeLocationIndex.put(key, Boolean.TRUE);
        locationThingTimeIndex.put(key, Boolean.TRUE);
    }
        
    
    Stream<FeatureKey> getCurrentLocationKeysByThing(long thingID)
    {
        var beforeLatest = new MVThingLocationKey(thingID, 0, Instant.MAX);
        var first = thingTimeLocationIndex.ceilingKey(beforeLatest);
        if (first == null || first.thingID != thingID)
            return Stream.empty();
        var last = new MVThingLocationKey(thingID, Long.MAX_VALUE, first.time);
        var cursor = new RangeCursor<>(thingTimeLocationIndex, first, last);
        
        return cursor.keyStream()
            .map(k -> new FeatureKey(k.locationID));
    }
    
    
    Stream<FeatureKey> getLocationKeysByThingAndTime(long thingID, Instant time)
    {
        /*var beforeFirst = new MVThingLocationKey(thingID, 0, time);
        var first = thingTimeLocationIndex.ceilingKey(beforeFirst);
        if (first == null || first.thingID != thingID || !first.time.equals(time))
            return Stream.empty();
        var last = new MVThingLocationKey(thingID, Long.MAX_VALUE, first.time);
        var cursor = new RangeCursor<>(thingTimeLocationIndex, first, last);*/
        
        var first = new MVThingLocationKey(thingID, 0, time.plusSeconds(1).minusMillis(1));
        var last = new MVThingLocationKey(thingID, 0, time);
        var cursor = new RangeCursor<>(thingTimeLocationIndex, first, last);
        
        return cursor.keyStream()
            .map(k -> new FeatureKey(k.locationID));
    }


    @Override
    protected Stream<Entry<MVFeatureParentKey, AbstractFeature>> getIndexedStream(STALocationFilter filter)
    {
        if (filter instanceof STALocationFilter)
        {
            var thingFilter = ((STALocationFilter)filter).getThings();
            if (thingFilter != null)
            {
                // get time filter
                var timeFilter = filter.getValidTime() != null ?
                    filter.getValidTime() : H2Utils.ALL_TIMES_FILTER;
                boolean currentVersionOnly = timeFilter.isCurrentTime();
                
                if (currentVersionOnly)
                {
                    return thingStore.selectKeys(thingFilter)
                        .flatMap(k -> getCurrentLocationKeysByThing(k.getInternalID()))
                        .map(k -> featuresIndex.getEntry(k));
                }
                else
                {
                    return thingStore.selectKeys(thingFilter)
                        .flatMap(k -> getLocationKeysByThingAndTime(k.getInternalID(), timeFilter.getMin()))
                        .map(k -> featuresIndex.getEntry(k));
                }
            }
        }
        
        return super.getIndexedStream(filter);
    }
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    Stream<IHistoricalLocation> getHistoricalLocationsByThing(long thingID, TemporalFilter timeRange)
    {
        MVThingLocationKey first, last;
        if (timeRange == null || timeRange.isAllTimes())
        {
            // time bounds are reversed since we are sorted by descending time stamp
            first = new MVThingLocationKey(thingID, 0, Instant.MAX);
            last = new MVThingLocationKey(thingID, Integer.MAX_VALUE, Instant.MIN);
        }
        else
        {
            // time bounds are reversed since we are sorted by descending time stamp
            //first = new MVThingLocationKey(thingID, 0, timeRange.getMax());
            //last = new MVThingLocationKey(thingID, Integer.MAX_VALUE, timeRange.getMin());
            first = new MVThingLocationKey(thingID, 0, timeRange.getMax().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1));
            last = new MVThingLocationKey(thingID, Integer.MAX_VALUE, timeRange.getMin().truncatedTo(ChronoUnit.SECONDS));
        }
        
        var cursor = new RangeCursor<>(thingTimeLocationIndex, first, last);
        class Holder { Instant value = null; }
        var lastTime = new Holder();
        
        return (Stream)cursor.keyStream()
            .filter(k -> !Objects.equals(k.time, lastTime.value))
            .peek(k -> lastTime.value = k.time);  
    }
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    Stream<IHistoricalLocation> getHistoricalLocationsByLocation(long locationID, RangeFilter<Instant> timeRange)
    {
        var beforeLatest = new MVThingLocationKey(0, locationID, timeRange.getMax());
        var first = locationThingTimeIndex.ceilingKey(beforeLatest);
        if (first == null || first.locationID != locationID)
            return Stream.empty();
        var last = new MVThingLocationKey(Long.MAX_VALUE, locationID, timeRange.getMin());
        var cursor = new RangeCursor<>(locationThingTimeIndex, first, last);
        return (Stream)cursor.keyStream();
    }
        
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Stream<IHistoricalLocation> selectHistoricalLocations(STALocationFilter filter)
    {
        if (filter.getThings() != null)
        {
            return thingStore.selectKeys(filter.getThings())
                .flatMap(k -> getHistoricalLocationsByThing(k.getInternalID(), filter.getValidTime()));
        }
        else if (filter.getInternalIDs() != null)
        {
            return filter.getInternalIDs().stream()
                .flatMap(id -> getHistoricalLocationsByLocation(id, filter.getValidTime()));
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
