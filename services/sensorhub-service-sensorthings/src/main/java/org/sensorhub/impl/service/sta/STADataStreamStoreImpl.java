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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.h2.mvstore.MVBTreeMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.RangeCursor;
import org.sensorhub.api.obs.DataStreamFilter;
import org.sensorhub.api.obs.DataStreamInfo;
import org.sensorhub.api.obs.IDataStreamInfo;
import org.sensorhub.api.obs.IDataStreamStore;
import org.sensorhub.impl.datastore.h2.MVVoidDataType;
import org.sensorhub.impl.service.sta.STADataStreamStoreTypes.*;
import org.vast.util.Asserts;


/**
 * <p>
 * Wrapper for {@link IDataStreamStore} to handle associations with Thing
 * entities.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 14, 2019
 */
class STADataStreamStoreImpl implements ISTADataStreamStore
{
    private static final String THING_DATASTREAMS_MAP_NAME = "@thing_dstreams";
    private static final String DATASTREAM_THING_MAP_NAME = "@dstream_thing";
    
    MVStore mvStore;
    IDataStreamStore delegateStore;
    ISTAThingStore thingStore;
    MVBTreeMap<MVDataStreamThingKey, Boolean> thingDataStreamsIndex;
    MVBTreeMap<Long, Long> dataStreamThingIndex;
    
    
    STADataStreamStoreImpl(STADatabase database, IDataStreamStore delegateStore)
    {
        this.mvStore = database.getMVStore();
        this.thingStore = database.getThingStore();
        this.delegateStore = delegateStore;
        
        // Thing-Datastreams association map
        String mapName = THING_DATASTREAMS_MAP_NAME + ":" + delegateStore.getDatastoreName();
        this.thingDataStreamsIndex = mvStore.openMap(mapName, new MVBTreeMap.Builder<MVDataStreamThingKey, Boolean>()
            .keyType(new MVThingDataStreamKeyDataType())
            .valueType(new MVVoidDataType()));
        
        // Datastream-Thing association map
        mapName = DATASTREAM_THING_MAP_NAME + ":" + delegateStore.getDatastoreName();
        this.dataStreamThingIndex = mvStore.openMap(mapName, new MVBTreeMap.Builder<Long, Long>());
    }


    @Override
    public Long add(IDataStreamInfo dsInfo)
    {
        Asserts.checkArgument(dsInfo instanceof STADataStream);
        
        // synchronize on MVStore to avoid autocommit in the middle of things
        synchronized (mvStore)
        {
            long currentVersion = mvStore.getCurrentVersion();
            
            try
            {
                // we need to create a pure DataStreamInfo before adding to DB
                var pureDsInfo = DataStreamInfo.Builder.from(dsInfo).build();
                
                Long newKey = delegateStore.add(pureDsInfo);
                putThingAssoc(newKey, (STADataStream)dsInfo);
                return newKey;
            }
            catch (Exception e)
            {
                mvStore.rollbackTo(currentVersion);
                throw e;
            }
        }
    }
    

    @Override
    public IDataStreamInfo put(Long key, IDataStreamInfo value)
    {
        Asserts.checkArgument(value instanceof STADataStream);
        
        // synchronize on MVStore to avoid autocommit in the middle of things
        synchronized (mvStore)
        {
            long currentVersion = mvStore.getCurrentVersion();
            
            try
            {
                // we need to create a pure DataStreamInfo before adding to DB
                IDataStreamInfo pureDsInfo = DataStreamInfo.Builder.from(value).build();
                
                IDataStreamInfo oldValue = delegateStore.put(key, pureDsInfo);
                putThingAssoc(key, (STADataStream)value);
                return oldValue;
            }
            catch (Exception e)
            {
                mvStore.rollbackTo(currentVersion);
                throw e;
            }
        }
    }
    
    
    void putThingAssoc(Long dsID, STADataStream dsInfo)
    {
        // remove previous assoc if any
        Long oldThingID = dataStreamThingIndex.remove(dsID);
        if (oldThingID != null)
            thingDataStreamsIndex.remove(new MVDataStreamThingKey(oldThingID, dsID));
        
        // create new assoc
        var assocKey = new MVDataStreamThingKey(dsInfo.getThingID(), dsID);
        thingDataStreamsIndex.put(assocKey, Boolean.TRUE);
        dataStreamThingIndex.put(assocKey.dataStreamID, assocKey.thingID);
    }


    public IDataStreamInfo remove(Object key)
    {
        // synchronize on MVStore to avoid autocommit in the middle of things
        synchronized (mvStore)
        {
            long currentVersion = mvStore.getCurrentVersion();
            
            try
            {
                IDataStreamInfo oldValue = delegateStore.remove(key);
                removeThingAssoc((Long)key);
                return oldValue;
            }
            catch (Exception e)
            {
                mvStore.rollbackTo(currentVersion);
                throw e;
            }
        }
    }
    
    
    void removeThingAssoc(Long key)
    {
        long thingID = dataStreamThingIndex.remove(key);
        var assocKey = new MVDataStreamThingKey(thingID, key);
        thingDataStreamsIndex.remove(assocKey);        
    }


    @Override
    public Long getAssociatedThing(long dataStreamID)
    {
        return dataStreamThingIndex.get(dataStreamID);
    }
    
    
    Stream<Long> getDataStreamIdsByThing(long thingID)
    {
        MVDataStreamThingKey first = new MVDataStreamThingKey(thingID, 0);
        MVDataStreamThingKey last = new MVDataStreamThingKey(thingID, Long.MAX_VALUE);
        var cursor = new RangeCursor<>(thingDataStreamsIndex, first, last);        
        return cursor.keyStream()
            .map(k -> k.dataStreamID);
    }


    public Stream<Entry<Long, IDataStreamInfo>> selectEntries(DataStreamFilter filter, Set<DataStreamInfoField> fields)
    {
        if (filter instanceof STADataStreamFilter)
        {
            var thingFilter = ((STADataStreamFilter)filter).getThings();
            if (thingFilter != null)
            {
                TreeSet<Long> datastreamIDs = thingStore.selectKeys(thingFilter)
                    .flatMap(id -> getDataStreamIdsByThing(id.getInternalID()))
                    .collect(Collectors.toCollection(TreeSet::new));
                
                return thingStore.selectKeys(thingFilter)
                    .flatMap(id -> delegateStore.selectEntries(new DataStreamFilter.Builder()
                        .withInternalIDs(datastreamIDs)
                        .build(), fields));
            }
        }        
        
        return delegateStore.selectEntries(filter);
    }


    public Long getLatestVersionKey(String procUID, String outputName)
    {
        return delegateStore.getLatestVersionKey(procUID, outputName);
    }


    public String getDatastoreName()
    {
        return delegateStore.getDatastoreName();
    }


    public ZoneOffset getTimeZone()
    {
        return delegateStore.getTimeZone();
    }


    public long getNumRecords()
    {
        return delegateStore.getNumRecords();
    }


    public IDataStreamInfo getLatestVersion(String procUID, String outputName)
    {
        return delegateStore.getLatestVersion(procUID, outputName);
    }


    public Stream<IDataStreamInfo> select(DataStreamFilter query)
    {
        return delegateStore.select(query);
    }


    public Stream<Long> selectKeys(DataStreamFilter query)
    {
        return delegateStore.selectKeys(query);
    }


    public Entry<Long, IDataStreamInfo> getLatestVersionEntry(String procUID, String outputName)
    {
        return delegateStore.getLatestVersionEntry(procUID, outputName);
    }


    public Stream<Long> removeEntries(DataStreamFilter query)
    {
        return delegateStore.removeEntries(query);
    }


    public long countMatchingEntries(DataStreamFilter query)
    {
        return delegateStore.countMatchingEntries(query);
    }


    public void commit()
    {
        delegateStore.commit();
    }


    public void backup(OutputStream is) throws IOException
    {
        delegateStore.backup(is);
    }


    public void restore(InputStream os) throws IOException
    {
        delegateStore.restore(os);
    }


    public boolean isReadSupported()
    {
        return delegateStore.isReadSupported();
    }


    public boolean isWriteSupported()
    {
        return delegateStore.isWriteSupported();
    }


    public void putAll(Map<? extends Long, ? extends IDataStreamInfo> map)
    {
        delegateStore.putAll(map);
    }


    public int size()
    {
        return delegateStore.size();
    }


    public boolean isEmpty()
    {
        return delegateStore.isEmpty();
    }


    public boolean containsKey(Object key)
    {
        return delegateStore.containsKey(key);
    }


    public boolean containsValue(Object value)
    {
        return delegateStore.containsValue(value);
    }


    public IDataStreamInfo get(Object key)
    {
        return delegateStore.get(key);
    }


    public void clear()
    {
        delegateStore.clear();
    }


    public Set<Long> keySet()
    {
        return delegateStore.keySet();
    }


    public Collection<IDataStreamInfo> values()
    {
        return delegateStore.values();
    }


    public Set<Entry<Long, IDataStreamInfo>> entrySet()
    {
        return delegateStore.entrySet();
    }


    public boolean equals(Object o)
    {
        return delegateStore.equals(o);
    }


    public int hashCode()
    {
        return delegateStore.hashCode();
    }


    public IDataStreamInfo getOrDefault(Object key, IDataStreamInfo defaultValue)
    {
        return delegateStore.getOrDefault(key, defaultValue);
    }


    public void forEach(BiConsumer<? super Long, ? super IDataStreamInfo> action)
    {
        delegateStore.forEach(action);
    }


    public void replaceAll(BiFunction<? super Long, ? super IDataStreamInfo, ? extends IDataStreamInfo> function)
    {
        delegateStore.replaceAll(function);
    }


    public IDataStreamInfo putIfAbsent(Long key, IDataStreamInfo value)
    {
        return delegateStore.putIfAbsent(key, value);
    }


    public boolean remove(Object key, Object value)
    {
        return delegateStore.remove(key, value);
    }


    public boolean replace(Long key, IDataStreamInfo oldValue, IDataStreamInfo newValue)
    {
        return delegateStore.replace(key, oldValue, newValue);
    }


    public IDataStreamInfo replace(Long key, IDataStreamInfo value)
    {
        return delegateStore.replace(key, value);
    }


    public IDataStreamInfo computeIfAbsent(Long key, Function<? super Long, ? extends DataStreamInfo> mappingFunction)
    {
        return delegateStore.computeIfAbsent(key, mappingFunction);
    }


    public IDataStreamInfo computeIfPresent(Long key, BiFunction<? super Long, ? super IDataStreamInfo, ? extends IDataStreamInfo> remappingFunction)
    {
        return delegateStore.computeIfPresent(key, remappingFunction);
    }


    public IDataStreamInfo compute(Long key, BiFunction<? super Long, ? super IDataStreamInfo, ? extends IDataStreamInfo> remappingFunction)
    {
        return delegateStore.compute(key, remappingFunction);
    }


    public IDataStreamInfo merge(Long key, IDataStreamInfo value, BiFunction<? super IDataStreamInfo, ? super IDataStreamInfo, ? extends IDataStreamInfo> remappingFunction)
    {
        return delegateStore.merge(key, value, remappingFunction);
    }

}
