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
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.sensorhub.api.datastore.DataStreamFilter;
import org.sensorhub.api.datastore.DataStreamInfo;
import org.sensorhub.api.datastore.IDataStreamStore;


/**
 * <p>
 * Wrapper to implement filtering Datastreams by their parent Thing
 * </p>
 *
 * @author Alex Robin
 * @date Oct 14, 2019
 */
public class STAFederatedDataStreamStoreWrapper implements IDataStreamStore
{
    ISTADatabase database;
    IDataStreamStore federatedStore;
    IDataStreamStore staDataStreamStore;
    

    STAFederatedDataStreamStoreWrapper(ISTADatabase database, IDataStreamStore federatedStore)
    {
        this.database = database;
        this.staDataStreamStore = database != null ? database.getDataStreamStore() : null;
        this.federatedStore = federatedStore;        
    }


    public Stream<Entry<Long, DataStreamInfo>> selectEntries(DataStreamFilter filter)
    {
        if (filter instanceof STADataStreamFilter)
        {
            var thingFilter = ((STADataStreamFilter)filter).getThings();
            if (thingFilter != null && staDataStreamStore != null)
                return staDataStreamStore.selectEntries(filter)
                    .map(e -> new AbstractMap.SimpleEntry<>(database.toPublicID(e.getKey()), e.getValue()));
        }        
        
        return federatedStore.selectEntries(filter);
    }
    

    public Long add(DataStreamInfo dsInfo)
    {
        return federatedStore.add(dsInfo);
    }


    public Long getLatestVersionKey(String procUID, String outputName)
    {
        return federatedStore.getLatestVersionKey(procUID, outputName);
    }


    public String getDatastoreName()
    {
        return federatedStore.getDatastoreName();
    }


    public ZoneOffset getTimeZone()
    {
        return federatedStore.getTimeZone();
    }


    public long getNumRecords()
    {
        return federatedStore.getNumRecords();
    }


    public DataStreamInfo getLatestVersion(String procUID, String outputName)
    {
        return federatedStore.getLatestVersion(procUID, outputName);
    }


    public Stream<DataStreamInfo> select(DataStreamFilter query)
    {
        return federatedStore.select(query);
    }


    public Stream<Long> selectKeys(DataStreamFilter query)
    {
        return federatedStore.selectKeys(query);
    }


    public Entry<Long, DataStreamInfo> getLatestVersionEntry(String procUID, String outputName)
    {
        return federatedStore.getLatestVersionEntry(procUID, outputName);
    }


    public Stream<Long> removeEntries(DataStreamFilter query)
    {
        return federatedStore.removeEntries(query);
    }


    public long countMatchingEntries(DataStreamFilter query)
    {
        return federatedStore.countMatchingEntries(query);
    }


    public void commit()
    {
        federatedStore.commit();
    }


    public void backup(OutputStream is) throws IOException
    {
        federatedStore.backup(is);
    }


    public void restore(InputStream os) throws IOException
    {
        federatedStore.restore(os);
    }


    public boolean isReadSupported()
    {
        return federatedStore.isReadSupported();
    }


    public boolean isWriteSupported()
    {
        return federatedStore.isWriteSupported();
    }


    public void putAll(Map<? extends Long, ? extends DataStreamInfo> map)
    {
        federatedStore.putAll(map);
    }


    public int size()
    {
        return federatedStore.size();
    }


    public boolean isEmpty()
    {
        return federatedStore.isEmpty();
    }


    public boolean containsKey(Object key)
    {
        return federatedStore.containsKey(key);
    }


    public boolean containsValue(Object value)
    {
        return federatedStore.containsValue(value);
    }


    public DataStreamInfo get(Object key)
    {
        return federatedStore.get(key);
    }


    public DataStreamInfo put(Long key, DataStreamInfo value)
    {
        return federatedStore.put(key, value);
    }


    public DataStreamInfo remove(Object key)
    {
        return federatedStore.remove(key);
    }


    public void clear()
    {
        federatedStore.clear();
    }


    public Set<Long> keySet()
    {
        return federatedStore.keySet();
    }


    public Collection<DataStreamInfo> values()
    {
        return federatedStore.values();
    }


    public Set<Entry<Long, DataStreamInfo>> entrySet()
    {
        return federatedStore.entrySet();
    }


    public boolean equals(Object o)
    {
        return federatedStore.equals(o);
    }


    public int hashCode()
    {
        return federatedStore.hashCode();
    }


    public DataStreamInfo getOrDefault(Object key, DataStreamInfo defaultValue)
    {
        return federatedStore.getOrDefault(key, defaultValue);
    }


    public void forEach(BiConsumer<? super Long, ? super DataStreamInfo> action)
    {
        federatedStore.forEach(action);
    }


    public void replaceAll(BiFunction<? super Long, ? super DataStreamInfo, ? extends DataStreamInfo> function)
    {
        federatedStore.replaceAll(function);
    }


    public DataStreamInfo putIfAbsent(Long key, DataStreamInfo value)
    {
        return federatedStore.putIfAbsent(key, value);
    }


    public boolean remove(Object key, Object value)
    {
        return federatedStore.remove(key, value);
    }


    public boolean replace(Long key, DataStreamInfo oldValue, DataStreamInfo newValue)
    {
        return federatedStore.replace(key, oldValue, newValue);
    }


    public DataStreamInfo replace(Long key, DataStreamInfo value)
    {
        return federatedStore.replace(key, value);
    }


    public DataStreamInfo computeIfAbsent(Long key, Function<? super Long, ? extends DataStreamInfo> mappingFunction)
    {
        return federatedStore.computeIfAbsent(key, mappingFunction);
    }


    public DataStreamInfo computeIfPresent(Long key, BiFunction<? super Long, ? super DataStreamInfo, ? extends DataStreamInfo> remappingFunction)
    {
        return federatedStore.computeIfPresent(key, remappingFunction);
    }


    public DataStreamInfo compute(Long key, BiFunction<? super Long, ? super DataStreamInfo, ? extends DataStreamInfo> remappingFunction)
    {
        return federatedStore.compute(key, remappingFunction);
    }


    public DataStreamInfo merge(Long key, DataStreamInfo value, BiFunction<? super DataStreamInfo, ? super DataStreamInfo, ? extends DataStreamInfo> remappingFunction)
    {
        return federatedStore.merge(key, value, remappingFunction);
    }

}
