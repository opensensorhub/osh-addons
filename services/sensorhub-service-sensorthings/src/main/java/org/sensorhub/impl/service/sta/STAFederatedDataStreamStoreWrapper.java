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
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;


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


    public Stream<Entry<DataStreamKey, IDataStreamInfo>> selectEntries(DataStreamFilter filter, Set<DataStreamInfoField> fields)
    {
        if (filter instanceof STADataStreamFilter && ((STADataStreamFilter)filter).getThings() != null)
            return staDataStreamStore.selectEntries(filter, fields);
        else
            return federatedStore.selectEntries(filter);
    }


    public DataStreamKey getLatestVersionKey(String sysUID, String outputName)
    {
        return federatedStore.getLatestVersionKey(sysUID, outputName);
    }


    public String getDatastoreName()
    {
        return federatedStore.getDatastoreName();
    }


    public long getNumRecords()
    {
        return federatedStore.getNumRecords();
    }


    public IDataStreamInfo getLatestVersion(String sysUID, String outputName)
    {
        return federatedStore.getLatestVersion(sysUID, outputName);
    }


    public Stream<IDataStreamInfo> select(DataStreamFilter query)
    {
        return federatedStore.select(query);
    }


    public Stream<DataStreamKey> selectKeys(DataStreamFilter query)
    {
        return federatedStore.selectKeys(query);
    }


    public Entry<DataStreamKey, IDataStreamInfo> getLatestVersionEntry(String sysUID, String outputName)
    {
        return federatedStore.getLatestVersionEntry(sysUID, outputName);
    }


    public long removeEntries(DataStreamFilter query)
    {
        return federatedStore.removeEntries(query);
    }


    public long countMatchingEntries(DataStreamFilter query)
    {
        return federatedStore.countMatchingEntries(query);
    }


    public void commit() throws DataStoreException
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


    public boolean isReadOnly()
    {
        return federatedStore.isReadOnly();
    }


    public void putAll(Map<? extends DataStreamKey, ? extends IDataStreamInfo> map)
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


    public IDataStreamInfo get(Object key)
    {
        return federatedStore.get(key);
    }


    public IDataStreamInfo put(DataStreamKey key, IDataStreamInfo value)
    {
        return federatedStore.put(key, value);
    }


    public IDataStreamInfo remove(Object key)
    {
        return federatedStore.remove(key);
    }


    public void clear()
    {
        federatedStore.clear();
    }


    public Set<DataStreamKey> keySet()
    {
        return federatedStore.keySet();
    }


    public Collection<IDataStreamInfo> values()
    {
        return federatedStore.values();
    }


    public Set<Entry<DataStreamKey, IDataStreamInfo>> entrySet()
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


    public IDataStreamInfo getOrDefault(Object key, IDataStreamInfo defaultValue)
    {
        return federatedStore.getOrDefault(key, defaultValue);
    }


    public void forEach(BiConsumer<? super DataStreamKey, ? super IDataStreamInfo> action)
    {
        federatedStore.forEach(action);
    }


    public void replaceAll(BiFunction<? super DataStreamKey, ? super IDataStreamInfo, ? extends IDataStreamInfo> function)
    {
        federatedStore.replaceAll(function);
    }


    public IDataStreamInfo putIfAbsent(DataStreamKey key, IDataStreamInfo value)
    {
        return federatedStore.putIfAbsent(key, value);
    }


    public boolean remove(Object key, Object value)
    {
        return federatedStore.remove(key, value);
    }


    public boolean replace(DataStreamKey key, IDataStreamInfo oldValue, IDataStreamInfo newValue)
    {
        return federatedStore.replace(key, oldValue, newValue);
    }


    public IDataStreamInfo replace(DataStreamKey key, IDataStreamInfo value)
    {
        return federatedStore.replace(key, value);
    }


    public IDataStreamInfo computeIfAbsent(DataStreamKey key, Function<? super DataStreamKey, ? extends IDataStreamInfo> mappingFunction)
    {
        return federatedStore.computeIfAbsent(key, mappingFunction);
    }


    public IDataStreamInfo computeIfPresent(DataStreamKey key, BiFunction<? super DataStreamKey, ? super IDataStreamInfo, ? extends IDataStreamInfo> remappingFunction)
    {
        return federatedStore.computeIfPresent(key, remappingFunction);
    }


    public IDataStreamInfo compute(DataStreamKey key, BiFunction<? super DataStreamKey, ? super IDataStreamInfo, ? extends IDataStreamInfo> remappingFunction)
    {
        return federatedStore.compute(key, remappingFunction);
    }


    public IDataStreamInfo merge(DataStreamKey key, IDataStreamInfo value, BiFunction<? super IDataStreamInfo, ? super IDataStreamInfo, ? extends IDataStreamInfo> remappingFunction)
    {
        return federatedStore.merge(key, value, remappingFunction);
    }
    

    public DataStreamKey add(IDataStreamInfo dsInfo)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void linkTo(ISystemDescStore systemStore)
    {
        throw new UnsupportedOperationException();
    }

}
