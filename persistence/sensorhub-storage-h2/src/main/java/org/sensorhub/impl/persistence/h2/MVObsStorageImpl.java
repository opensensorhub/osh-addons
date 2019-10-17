/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataFilter;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IObsStorageModule;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.utils.FileUtils;
import org.vast.util.Asserts;
import org.vast.util.Bbox;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractTimeGeometricPrimitive;
import net.opengis.gml.v32.TimeInstant;
import net.opengis.gml.v32.TimePeriod;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


/**
 * <p>
 * Implementation of Observation storage using H2 MVStore
 * </p>
 * <p>
 * The following maps are created in the MVStore:<br/>
 * - "@descHistory" contains process descriptions indexed by time<br/>
 * - "@recordStores" contains the definition and names of each record store<br/>
 * </p>
 * <p>
 * Features of Interest and records indexing is handled by other classes:<br/>
 * - {@link MVFeatureStoreImpl}<br/>
 * - {@link MVTimeSeriesImpl}<br/>
 * </p>
 * <p>
 * This class is used at a top-level store but also as a nested datastore
 * inside a multi-producer storage.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 10, 2017
 */
public class MVObsStorageImpl extends AbstractModule<MVStorageConfig> implements IObsStorageModule<MVStorageConfig>
{
    private static final String DESC_HISTORY_MAP_NAME = "@descHistory";
    private static final String RECORD_STORE_INFO_MAP_NAME = "@recordStores";
    
    MVStore mvStore;
    MVMap<String, IRecordStoreInfo> rsInfoMap;
    Map<String, MVTimeSeriesImpl> recordStores = new ConcurrentHashMap<>();
    MVMap<ProducerTimeKey, AbstractProcess> processDescMap;
    MVFeatureStoreImpl featureStore;
    private final String producerID;
    
    
    public MVObsStorageImpl()
    {
        this.producerID = null;
    }
    
    
    /*
     * Constructor used to create a nested MVObsStorage within a MVMultiStorage
     * In this case, we share maps between all producers
     */
    protected MVObsStorageImpl(MVObsStorageImpl parentStore, String producerID)
    {
        Asserts.checkNotNull(parentStore, "Parent");                
        this.mvStore = parentStore.mvStore;
        this.producerID = producerID;
        this.processDescMap = parentStore.processDescMap;
        this.featureStore = parentStore.featureStore;
        this.rsInfoMap = parentStore.rsInfoMap;
        this.recordStores = parentStore.recordStores;
    }


    @Override
    public synchronized void start() throws SensorHubException
    {
        Asserts.checkState(mvStore == null, "Cannot start a nested ObsStorage instance");
        
        try
        {
            // check file path is valid
            if (!FileUtils.isSafeFilePath(config.storagePath))
                throw new StorageException("Storage path contains illegal characters: " + config.storagePath);
            
            MVStore.Builder builder = new MVStore.Builder()
                                      .fileName(config.storagePath);
            
            if (config.memoryCacheSize > 0)
                builder = builder.cacheSize(config.memoryCacheSize/1024);
                                      
            if (config.autoCommitBufferSize > 0)
                builder = builder.autoCommitBufferSize(config.autoCommitBufferSize);
            
            if (config.useCompression)
                builder = builder.compress();
            
            mvStore = builder.open();
            mvStore.setVersionsToKeep(0);
                        
            // open description history map
            processDescMap = mvStore.openMap(DESC_HISTORY_MAP_NAME, new MVMap.Builder<ProducerTimeKey, AbstractProcess>()
                    .keyType(new ProducerKeyDataType())
                    .valueType(new KryoDataType()));
            
            // create feature store
            featureStore = new MVFeatureStoreImpl(mvStore);
            
            // load all record stores
            rsInfoMap = mvStore.openMap(RECORD_STORE_INFO_MAP_NAME, new MVMap.Builder<String, IRecordStoreInfo>().valueType(new KryoDataType()));
            for (IRecordStoreInfo rsInfo: rsInfoMap.values())
                loadRecordStore(rsInfo);
        }
        catch (Exception e)
        {
            throw new StorageException("Error while initializing storage", e);
        }
    }
    
    
    private void loadRecordStore(IRecordStoreInfo rsInfo)
    {
        MVTimeSeriesImpl recordStore;        
        if (config.indexObsLocation)
            recordStore = new MVTimeSeriesImpl(this, rsInfo);
        else
            recordStore = new MVTimeSeriesImpl(this, rsInfo.getName());
        
        recordStores.put(rsInfo.getName(), recordStore);
    }


    @Override
    public synchronized void stop() throws SensorHubException
    {
        if (mvStore != null) 
        {
            // make sure cached data is flushed
            for (MVTimeSeriesImpl recordStore: recordStores.values())
                recordStore.close();
            
            mvStore.close();
            mvStore = null;
            processDescMap = null;
            rsInfoMap = null;
            recordStores.clear();
            featureStore = null;
        }
    }


    @Override
    public void commit()
    {
        checkOpen();
        mvStore.commit();
    }


    @Override
    public void rollback()
    {
        checkOpen();
        mvStore.rollback();
    }


    @Override
    public synchronized void backup(OutputStream os) throws IOException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public synchronized void restore(InputStream is) throws IOException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public synchronized void sync(IStorageModule<?> storage) throws StorageException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public synchronized void cleanup() throws SensorHubException
    {
        if (mvStore != null)
            stop();
        
        // we just mark file as deleted by renaming it with .deleted suffix
        // storage will restart with an empty file but we won't loose any data
        if (config.storagePath != null)
        {
            File dbFile = new File(config.storagePath);
            File newFile = new File(config.storagePath + ".deleted");
            dbFile.renameTo(newFile);
        }
    }


    @Override
    public AbstractProcess getLatestDataSourceDescription()
    {
        return getDataSourceDescriptionAtTime(Double.POSITIVE_INFINITY);
    }


    @Override
    public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime)
    {
        checkOpen();
        
        ProducerTimeKey startKey = new ProducerTimeKey(producerID, startTime);
        ProducerTimeKey endKey = new ProducerTimeKey(producerID, endTime);
        RangeCursor<ProducerTimeKey, AbstractProcess> cursor = new RangeCursor<>(processDescMap, startKey, endKey);
        
        ArrayList<AbstractProcess> descList = new ArrayList<>();
        while (cursor.hasNext())
        {
            cursor.next();
            descList.add(cursor.getValue());
        }
        
        return descList;
    }


    @Override
    public AbstractProcess getDataSourceDescriptionAtTime(double time)
    {
        checkOpen();
        
        ProducerTimeKey key = new ProducerTimeKey(producerID, time);
        key = processDescMap.floorKey(key);
        if (key != null)
            return processDescMap.get(key);
        else
            return null;
    }
    
    
    protected synchronized boolean storeDataSourceDescription(AbstractProcess process, double time, boolean update)
    {
        checkOpen();
        
        ProducerTimeKey key = new ProducerTimeKey(producerID, time);
        if (update)
        {
            AbstractProcess oldProcess = processDescMap.replace(key, process);
            return (oldProcess != null);
        }
        else
        {
            AbstractProcess oldProcess = processDescMap.putIfAbsent(key, process);
            return (oldProcess == null);
        }
    }
    
    
    protected synchronized boolean storeDataSourceDescription(AbstractProcess process, boolean update)
    {
        checkOpen();
        Asserts.checkNotNull(process, AbstractProcess.class);
        
        boolean ok = false;            
        if (process.getNumValidTimes() > 0)
        {
            // we add the description in index for each validity period/instant
            for (AbstractTimeGeometricPrimitive validTime: process.getValidTimeList())
            {
                double time = Double.NaN;
                
                if (validTime instanceof TimeInstant)
                    time = ((TimeInstant) validTime).getTimePosition().getDecimalValue();
                else if (validTime instanceof TimePeriod)
                    time = ((TimePeriod) validTime).getBeginPosition().getDecimalValue();
                
                if (!Double.isNaN(time))
                    ok = storeDataSourceDescription(process, time, update);
            }
        }
        else
        {
            double time = System.currentTimeMillis() / 1000.;
            ok = storeDataSourceDescription(process, time, update);
        }
            
        return ok;
    }


    @Override
    public synchronized void storeDataSourceDescription(AbstractProcess process)
    {
        storeDataSourceDescription(process, false);
    }


    @Override
    public synchronized void updateDataSourceDescription(AbstractProcess process)
    {
        storeDataSourceDescription(process, true);
    }


    @Override
    public synchronized void removeDataSourceDescription(double time)
    {
        checkOpen();
        processDescMap.remove(new ProducerTimeKey(producerID, time));
    }


    @Override
    public synchronized void removeDataSourceDescriptionHistory(double startTime, double endTime)
    {
        checkOpen();
        
        ProducerTimeKey startKey = new ProducerTimeKey(producerID, startTime);
        ProducerTimeKey endKey = new ProducerTimeKey(producerID, endTime);
        RangeCursor<ProducerTimeKey, AbstractProcess> cursor = new RangeCursor<>(processDescMap, startKey, endKey);
        
        while (cursor.hasNext())
            processDescMap.remove(cursor.next());
    }


    @Override
    public Map<String, ? extends IRecordStoreInfo> getRecordStores()
    {
        checkOpen();
        return Collections.unmodifiableMap(rsInfoMap);
    }
    
    
    protected MVTimeSeriesImpl getRecordStore(String recordType)
    {
        Asserts.checkNotNull(recordType, "recordType");
        
        MVTimeSeriesImpl timeSeries = recordStores.get(recordType);
        if (timeSeries == null)
            throw new IllegalArgumentException("No time series with name " + recordType);
        return timeSeries;
    }


    @Override
    public synchronized void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding)
    {
        checkOpen();
        Asserts.checkNotNull(name, "name");
        Asserts.checkNotNull(recordStructure, DataComponent.class);
        Asserts.checkNotNull(recommendedEncoding, DataEncoding.class);
        
        DataStreamInfo rsInfo = new DataStreamInfo(name, recordStructure, recommendedEncoding);
        rsInfoMap.put(name, rsInfo);
        loadRecordStore(rsInfo);
    }
    
    
    public synchronized void upgradeRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding, boolean deleteRecords)
    {
        checkOpen();
        Asserts.checkNotNull(name, "name");
        Asserts.checkNotNull(recordStructure, DataComponent.class);
        Asserts.checkNotNull(recommendedEncoding, DataEncoding.class);
        
        DataStreamInfo rsInfo = new DataStreamInfo(name, recordStructure, recommendedEncoding);
        rsInfoMap.put(name, rsInfo);
        
        // remove old records if requested
        // usually needed if old records are not compatible with new data structure
        if (deleteRecords)
            recordStores.get(name).remove(new DataFilter(name));
        
        mvStore.commit();
    }
    
    
    public synchronized void removeRecordStore(String name)
    {
        rsInfoMap.remove(name);
        MVTimeSeriesImpl rsStore = recordStores.remove(name);
        rsStore.delete();
        mvStore.commit();
    }


    @Override
    public int getNumRecords(String recordType)
    {
        checkOpen();
        return getRecordStore(recordType).getNumRecords(producerID);
    }


    @Override
    public double[] getRecordsTimeRange(String recordType)
    {
        checkOpen();
        return getRecordStore(recordType).getDataTimeRange(producerID);
    }
    
    
    @Override
    public int[] getEstimatedRecordCounts(String recordType, double[] timeStamps)
    {
        Asserts.checkNotNull(timeStamps);
        Asserts.checkArgument(timeStamps.length >= 2, "At least 2 time stamps must be provided");
        return getRecordStore(recordType).getEstimatedRecordCounts(producerID, timeStamps);
    }
    
    
    protected void checkProducerID(String reqProducerID)
    {
        Asserts.checkArgument(reqProducerID.equals(producerID),
                "Invalid producer ID {}", reqProducerID);
    }
    
    
    protected void checkProducerID(Set<String> reqProducerIDs)
    {
        Asserts.checkArgument(reqProducerIDs.size() == 1,
                "No more than one producer ID can be requested");
        checkProducerID(reqProducerIDs.iterator().next());
    }
    
    
    protected void ensureProducerID(DataKey key)
    {
        Asserts.checkNotNull(key, DataKey.class);
        
        if (producerID == null)
            key.producerID = null;
        else if (key.producerID == null)
            key.producerID = producerID;
        else
            checkProducerID(key.producerID);
    }
    
    
    protected IDataFilter ensureProducerID(IDataFilter filter)
    {
        Asserts.checkNotNull(filter, IDataFilter.class);
        
        if (producerID == null)
            filter = new ProducerObsFilter(null, filter);
        else if (filter.getProducerIDs() == null || filter.getProducerIDs().isEmpty())
            filter = new ProducerObsFilter(producerID, filter);
        else
            checkProducerID(filter.getProducerIDs());
        
        return filter;
    }


    @Override
    public DataBlock getDataBlock(DataKey key)
    {
        checkOpen();
        ensureProducerID(key);
        
        return getRecordStore(key.recordType).getDataBlock(key);
    }


    @Override
    public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        checkOpen();
        filter = ensureProducerID(filter);
        
        return getRecordStore(filter.getRecordType()).getDataBlockIterator(filter);
    }


    @Override
    public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter)
    {
        checkOpen();
        filter = ensureProducerID(filter);
        
        return getRecordStore(filter.getRecordType()).getRecordIterator(filter);
    }


    @Override
    public int getNumMatchingRecords(IDataFilter filter, long maxCount)
    {
        checkOpen();
        filter = ensureProducerID(filter);
        
        return getRecordStore(filter.getRecordType()).getNumMatchingRecords(filter, maxCount);
    }


    @Override
    public synchronized void storeRecord(DataKey key, DataBlock data)
    {
        checkOpen();
        ensureProducerID(key);
        Asserts.checkNotNull(data, DataBlock.class);
        
        getRecordStore(key.recordType).store(key, data);
    }


    @Override
    public synchronized void updateRecord(DataKey key, DataBlock data)
    {
        checkOpen();
        ensureProducerID(key);
        Asserts.checkNotNull(data, DataBlock.class);
        
        getRecordStore(key.recordType).update(key, data);
    }


    @Override
    public synchronized void removeRecord(DataKey key)
    {
        checkOpen();
        ensureProducerID(key);
        
        getRecordStore(key.recordType).remove(key);
    }


    @Override
    public synchronized int removeRecords(IDataFilter filter)
    {
        checkOpen();
        filter = ensureProducerID(filter);
        
        return getRecordStore(filter.getRecordType()).remove(filter);
    }


    @Override
    public int getNumFois(IFoiFilter filter)
    {
        checkOpen();
        Asserts.checkNotNull(filter, IFoiFilter.class);
        
        return featureStore.getNumFeatures();
    }


    @Override
    public Bbox getFoisSpatialExtent()
    {
        return featureStore.getFeaturesSpatialExtent();
    }
    
    
    protected IFoiFilter getProducerFoiFilter(IFoiFilter filter)
    {
        // check producer ID if there is one specified
        if (filter.getProducerIDs() != null && !filter.getProducerIDs().isEmpty())
            checkProducerID(filter.getProducerIDs());
        
        // retrieve all FOIs associated with this producer
        LinkedHashSet<String> foiIDs = new LinkedHashSet<>();
        for (MVTimeSeriesImpl recordStore: recordStores.values())
        {
            Iterator<String> it = recordStore.getFoiIDs(producerID);
            while (it.hasNext())
            {
                String key = it.next();
                String foiID = key.replaceFirst(producerID, "");
                foiIDs.add(foiID);
            }
        }
        
        // also keep those of the original filter
        if (filter.getFeatureIDs() != null)
            foiIDs.addAll(filter.getFeatureIDs());
        
        return new ProducerFoiFilter(foiIDs, filter);
    }


    @Override
    public Iterator<String> getFoiIDs(IFoiFilter filter)
    {
        checkOpen();
        Asserts.checkNotNull(filter, IFoiFilter.class);
        
        // if producer specific, add producer FOI IDs to filter
        if (producerID != null)
            filter = getProducerFoiFilter(filter);
        
        return featureStore.getFeatureIDs(filter);
    }


    @Override
    public Iterator<AbstractFeature> getFois(IFoiFilter filter)
    {
        checkOpen();
        Asserts.checkNotNull(filter, IFoiFilter.class);
        
        // if producer specific, add producer FOI IDs to filter
        if (producerID != null)
            filter = getProducerFoiFilter(filter);
        
        return featureStore.getFeatures(filter);
    }


    @Override
    public synchronized void storeFoi(String producerID, AbstractFeature foi)
    {
        checkOpen();
        Asserts.checkNotNull(foi, AbstractFeature.class);
        
        featureStore.store(foi);
    }


    @Override
    public boolean isReadSupported()
    {
        return true;
    }


    @Override
    public boolean isWriteSupported()
    {
        return true;
    }
    
    
    protected void checkOpen()
    {
        Asserts.checkState(mvStore != null, "Storage is not open");
    }
}
