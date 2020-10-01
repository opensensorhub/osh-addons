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
import org.h2.mvstore.MVStore;
import org.h2.mvstore.RangeCursor;
import org.sensorhub.api.feature.FeatureKey;
import org.sensorhub.api.feature.IFeatureFilter;
import org.sensorhub.api.feature.IFeatureStore.FeatureField;
import org.sensorhub.impl.datastore.h2.H2Utils;
import org.sensorhub.impl.datastore.h2.IdProvider;
import org.sensorhub.impl.datastore.h2.MVBaseFeatureStoreImpl;
import org.sensorhub.impl.datastore.h2.MVDataStoreInfo;
import org.sensorhub.impl.service.sta.STALocationStoreTypes.MVThingLocationKey;
import org.vast.ogc.gml.GenericFeature;


/**
 * <p>
 * Extension of {@link MVBaseFeatureStoreImpl} to handle JOIN queries of
 * Things by Location.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 29, 2019
 */
class STAThingStoreImpl extends MVBaseFeatureStoreImpl<GenericFeature, FeatureField> implements ISTAThingStore
{
    STALocationStoreImpl locationStore;
    
    
    STAThingStoreImpl()
    {
    }
    
    
    public static STAThingStoreImpl open(STADatabase db, String dataStoreName)
    {
        MVDataStoreInfo dataStoreInfo = H2Utils.loadDataStoreInfo(db.getMVStore(), dataStoreName);
        var store = new STAThingStoreImpl().init(db.getMVStore(), dataStoreInfo, null);
        return store;
    }
    
    
    public static STAThingStoreImpl create(STADatabase db, MVDataStoreInfo dataStoreInfo)
    {
        H2Utils.addDataStoreInfo(db.getMVStore(), dataStoreInfo);
        var store = new STAThingStoreImpl().init(db.getMVStore(), dataStoreInfo, null);
        return store;
    }
    
    
    @Override
    protected STAThingStoreImpl init(MVStore mvStore, MVDataStoreInfo dataStoreInfo, IdProvider idProvider)
    {
        super.init(mvStore, dataStoreInfo, idProvider);
        return this;
    }
    
    
    @Override
    protected FeatureKey generateKey(GenericFeature feature)
    {
        // generate key
        long internalID = idProvider.newInternalID();
        return new FeatureKey(internalID, FeatureKey.TIMELESS);
    }
        
    
    Stream<FeatureKey> getThingKeysByCurrentLocation(long locationID)
    {
        var first = new MVThingLocationKey(0, locationID, Instant.MAX);
        var last = new MVThingLocationKey(Long.MAX_VALUE, locationID, Instant.MIN);
        
        class Holder { Long value = null; }
        var lastThing = new Holder();
        
        var cursor = new RangeCursor<>(locationStore.locationThingTimeIndex, first, last);
        return cursor.keyStream()
            .filter(k -> !Objects.equals(k.thingID, lastThing.value))
            .filter(k -> isLatestLocation(k))
            .peek(k -> lastThing.value = k.thingID)
            .map(k -> new FeatureKey(k.thingID));
    }
    
    
    boolean isLatestLocation(MVThingLocationKey key)
    {
        // we need to check that this location is actually the latest for this thing
        
        var beforeLatest = new MVThingLocationKey(key.thingID, 0, Instant.MAX);
        var latestKey = locationStore.thingTimeLocationIndex.ceilingKey(beforeLatest);
        
        return latestKey != null &&
            latestKey.thingID == key.thingID &&
            Objects.equals(latestKey.time, key.time);
    }
    
    
    @Override
    protected Stream<Entry<FeatureKey, GenericFeature>> getIndexedStream(IFeatureFilter filter)
    {
        if (filter instanceof STAThingFilter)
        {
            var locationFilter = ((STAThingFilter)filter).getLocations();
            if (locationFilter != null)
            {
                return locationStore.selectKeys(locationFilter)
                    .flatMap(k -> getThingKeysByCurrentLocation(k.getInternalID()))
                    .map(k -> featuresIndex.getEntry(k));
            }
        }
        
        return super.getIndexedStream(filter);
    }
    
}
