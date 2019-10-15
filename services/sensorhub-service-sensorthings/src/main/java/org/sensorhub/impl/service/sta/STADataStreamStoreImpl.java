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
import java.nio.ByteBuffer;
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
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.MVBTreeMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.RangeCursor;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;
import org.sensorhub.api.datastore.DataStreamFilter;
import org.sensorhub.api.datastore.DataStreamInfo;
import org.sensorhub.api.datastore.FeatureKey;
import org.sensorhub.api.datastore.IDataStreamStore;
import org.sensorhub.api.datastore.IFeatureStore;
import org.sensorhub.impl.datastore.h2.MVVoidDataType;
import org.vast.ogc.gml.GenericFeature;


/**
 * <p>
 * Wrapper for {@link IDataStreamStore} to handle associations with Thing
 * entities.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 14, 2019
 */
public class STADataStreamStoreImpl implements IDataStreamStore
{
    private static final String THING_DATASTREAM_MAP_NAME = "@thing_dstreams";
    
    MVStore mvStore;
    IDataStreamStore delegateStore;
    IFeatureStore<FeatureKey, GenericFeature> thingStore;
    MVBTreeMap<MVDataStreamThingKey, Boolean> thingDataStreamIndex;
    
    
    static class MVDataStreamThingKey
    {
        long thingID;
        long dataStreamID;
        
        MVDataStreamThingKey(long thingID, long dsID)
        {
            this.thingID = thingID;
            this.dataStreamID = dsID;
        }
    }
    
    
    static class MVDataStreamThingKeyDataType implements DataType
    {
        private static final int MEM_SIZE = 8+8;
        
        
        @Override
        public int compare(Object objA, Object objB)
        {
            MVDataStreamThingKey a = (MVDataStreamThingKey)objA;
            MVDataStreamThingKey b = (MVDataStreamThingKey)objB;
            
            // first compare thing internal ID
            int comp = Long.compare(a.thingID, b.thingID);
            if (comp != 0)
                return comp;
            
            // then compare datastream ID
            return Long.compare(a.dataStreamID, b.dataStreamID);
        }
        

        @Override
        public int getMemory(Object obj)
        {
            return MEM_SIZE;
        }
        

        @Override
        public void write(WriteBuffer wbuf, Object obj)
        {
            MVDataStreamThingKey key = (MVDataStreamThingKey)obj;
            wbuf.putVarLong(key.thingID);
            wbuf.putVarLong(key.dataStreamID);
        }
        

        @Override
        public void write(WriteBuffer wbuf, Object[] obj, int len, boolean key)
        {
            for (int i=0; i<len; i++)
                write(wbuf, obj[i]);
        }
        

        @Override
        public Object read(ByteBuffer buff)
        {
            long thingID = DataUtils.readVarLong(buff);
            long dsID = DataUtils.readVarLong(buff);
            return new MVDataStreamThingKey(thingID, dsID);
        }
        

        @Override
        public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
        {
            for (int i=0; i<len; i++)
                obj[i] = read(buff);        
        }
    }
    
    
    STADataStreamStoreImpl(STADatabase database, MVStore mvStore, IDataStreamStore delegateStore)
    {
        this.mvStore = mvStore;
        this.thingStore = database.getThingStore();
        this.delegateStore = delegateStore;
        
        // Thing-Datastream association map
        String mapName = THING_DATASTREAM_MAP_NAME + ":" + delegateStore.getDatastoreName();
        this.thingDataStreamIndex = mvStore.openMap(mapName, new MVBTreeMap.Builder<MVDataStreamThingKey, Boolean>()
            .keyType(new MVDataStreamThingKeyDataType())
            .valueType(new MVVoidDataType()));
    }


    public Long add(DataStreamInfo dsInfo)
    {
        // synchronize on MVStore to avoid autocommit in the middle of things
        synchronized (mvStore)
        {
            long currentVersion = mvStore.getCurrentVersion();
            
            try
            {
                Long newKey = delegateStore.add(dsInfo);
                putThingAssoc(newKey, dsInfo);
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
    public DataStreamInfo put(Long key, DataStreamInfo value)
    {
        // synchronize on MVStore to avoid autocommit in the middle of things
        synchronized (mvStore)
        {
            long currentVersion = mvStore.getCurrentVersion();
            
            try
            {
                DataStreamInfo oldValue = delegateStore.put(key, value);
                putThingAssoc(key, value);
                return oldValue;
            }
            catch (Exception e)
            {
                mvStore.rollbackTo(currentVersion);
                throw e;
            }
        }
    }
    
    
    protected void putThingAssoc(Long key, DataStreamInfo dsInfo)
    {
        if (dsInfo instanceof STADataStream)
        {
            var assocKey = new MVDataStreamThingKey(
               ((STADataStream) dsInfo).getThingID(),
               key);
           
            thingDataStreamIndex.put(assocKey, Boolean.TRUE);
        }
    }


    public DataStreamInfo remove(Object key)
    {
        // synchronize on MVStore to avoid autocommit in the middle of things
        synchronized (mvStore)
        {
            long currentVersion = mvStore.getCurrentVersion();
            
            try
            {
                DataStreamInfo oldValue = delegateStore.remove(key);
                removeThingAssoc((Long)key, oldValue);
                return oldValue;
            }
            catch (Exception e)
            {
                mvStore.rollbackTo(currentVersion);
                throw e;
            }
        }
    }
    
    
    protected void removeThingAssoc(Long key, DataStreamInfo dsInfo)
    {
        if (dsInfo instanceof STADataStream)
        {
            var assocKey = new MVDataStreamThingKey(
               ((STADataStream) dsInfo).getThingID(),
               key);
           
            thingDataStreamIndex.remove(assocKey, Boolean.TRUE);
        }
    }
    
    
    Stream<Long> getDataStreamIdsByThing(long thingID)
    {
        MVDataStreamThingKey first = new MVDataStreamThingKey(thingID, 0);
        MVDataStreamThingKey last = new MVDataStreamThingKey(thingID, Long.MAX_VALUE);
        var cursor = new RangeCursor<>(thingDataStreamIndex, first, last);        
        return cursor.keyStream()
            .map(k -> k.dataStreamID);
    }


    public Stream<Entry<Long, DataStreamInfo>> selectEntries(DataStreamFilter filter)
    {
        if (filter instanceof STADataStreamFilter)
        {
            var thingFilter = ((STADataStreamFilter)filter).getThingFilter();
            if (thingFilter != null)
            {
                TreeSet<Long> datastreamIDs = thingStore.selectKeys(thingFilter)
                    .flatMap(id -> getDataStreamIdsByThing(id.getInternalID()))
                    .collect(Collectors.toCollection(TreeSet::new));
                
                return thingStore.selectKeys(thingFilter)
                    .flatMap(id -> delegateStore.selectEntries(new DataStreamFilter.Builder()
                        .withInternalIDs(datastreamIDs)
                        .build()));
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


    public DataStreamInfo getLatestVersion(String procUID, String outputName)
    {
        return delegateStore.getLatestVersion(procUID, outputName);
    }


    public Stream<DataStreamInfo> select(DataStreamFilter query)
    {
        return delegateStore.select(query);
    }


    public Stream<Long> selectKeys(DataStreamFilter query)
    {
        return delegateStore.selectKeys(query);
    }


    public Entry<Long, DataStreamInfo> getLatestVersionEntry(String procUID, String outputName)
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


    public void putAll(Map<? extends Long, ? extends DataStreamInfo> map)
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


    public DataStreamInfo get(Object key)
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


    public Collection<DataStreamInfo> values()
    {
        return delegateStore.values();
    }


    public Set<Entry<Long, DataStreamInfo>> entrySet()
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


    public DataStreamInfo getOrDefault(Object key, DataStreamInfo defaultValue)
    {
        return delegateStore.getOrDefault(key, defaultValue);
    }


    public void forEach(BiConsumer<? super Long, ? super DataStreamInfo> action)
    {
        delegateStore.forEach(action);
    }


    public void replaceAll(BiFunction<? super Long, ? super DataStreamInfo, ? extends DataStreamInfo> function)
    {
        delegateStore.replaceAll(function);
    }


    public DataStreamInfo putIfAbsent(Long key, DataStreamInfo value)
    {
        return delegateStore.putIfAbsent(key, value);
    }


    public boolean remove(Object key, Object value)
    {
        return delegateStore.remove(key, value);
    }


    public boolean replace(Long key, DataStreamInfo oldValue, DataStreamInfo newValue)
    {
        return delegateStore.replace(key, oldValue, newValue);
    }


    public DataStreamInfo replace(Long key, DataStreamInfo value)
    {
        return delegateStore.replace(key, value);
    }


    public DataStreamInfo computeIfAbsent(Long key, Function<? super Long, ? extends DataStreamInfo> mappingFunction)
    {
        return delegateStore.computeIfAbsent(key, mappingFunction);
    }


    public DataStreamInfo computeIfPresent(Long key, BiFunction<? super Long, ? super DataStreamInfo, ? extends DataStreamInfo> remappingFunction)
    {
        return delegateStore.computeIfPresent(key, remappingFunction);
    }


    public DataStreamInfo compute(Long key, BiFunction<? super Long, ? super DataStreamInfo, ? extends DataStreamInfo> remappingFunction)
    {
        return delegateStore.compute(key, remappingFunction);
    }


    public DataStreamInfo merge(Long key, DataStreamInfo value, BiFunction<? super DataStreamInfo, ? super DataStreamInfo, ? extends DataStreamInfo> remappingFunction)
    {
        return delegateStore.merge(key, value, remappingFunction);
    }

}
