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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.sensorhub.api.common.SensorHubException;
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
import org.vast.util.Bbox;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractTimeGeometricPrimitive;
import net.opengis.gml.v32.TimeInstant;
import net.opengis.gml.v32.TimePeriod;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


public class MVObsStorageImpl extends AbstractModule<MVStorageConfig> implements IObsStorageModule<MVStorageConfig>
{
    private final static String DESC_HISTORY_MAP_NAME = ":desc";
    private final static String RECORD_STORE_INFO_MAP_NAME = ":rsInfo";
    MVStore mvStore;
    MVMap<Double, AbstractProcess> processDescMap;
    MVMap<String, IRecordStoreInfo> rsInfoMap;
    Map<String, MVTimeSeriesImpl> recordStores;
    MVFeatureStoreImpl featureStore;
    
    
    public MVObsStorageImpl()
    {
        this.recordStores = new LinkedHashMap<String, MVTimeSeriesImpl>();
    }


    @Override
    public synchronized void start() throws SensorHubException
    {
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
            
            this.mvStore = builder.open();
            //this.mvStore.setAutoCommitDelay(100000);
            
            // create description map
            this.processDescMap = mvStore.openMap(DESC_HISTORY_MAP_NAME, new MVMap.Builder<Double, AbstractProcess>().valueType(new KryoDataType()));
            
            // create feature store
            this.featureStore = new MVFeatureStoreImpl(mvStore);
            
            // load all record stores
            this.rsInfoMap = mvStore.openMap(RECORD_STORE_INFO_MAP_NAME, new MVMap.Builder<String, IRecordStoreInfo>().valueType(new KryoDataType()));
            for (IRecordStoreInfo rsInfo: rsInfoMap.values())
                loadRecordStore(rsInfo);
        }
        catch (Exception e)
        {
            throw new StorageException("Error while opening storage " + config.name, e);
        }
    }
    
    
    private void loadRecordStore(IRecordStoreInfo rsInfo)
    {
        MVTimeSeriesImpl recordStore = new MVTimeSeriesImpl(mvStore,
                rsInfo.getName(),
                rsInfo.getRecordDescription(),
                rsInfo.getRecommendedEncoding());
        
        recordStores.put(rsInfo.getName(), recordStore);
    }


    @Override
    public synchronized void stop() throws SensorHubException
    {
        if (mvStore != null) 
        {
            mvStore.close();
            mvStore = null;
        }
    }


    @Override
    public void backup(OutputStream os) throws IOException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void restore(InputStream is) throws IOException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void commit()
    {
        mvStore.commit();
    }


    @Override
    public void rollback()
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void sync(IStorageModule<?> storage) throws StorageException
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
        if (processDescMap.isEmpty())
            return null;
        
        return processDescMap.get(processDescMap.lastKey());
    }


    @Override
    public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime)
    {
        RangeCursor<Double, AbstractProcess> cursor = new RangeCursor<Double, AbstractProcess>(processDescMap, startTime, endTime);
        
        ArrayList<AbstractProcess> descList = new ArrayList<AbstractProcess>();
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
        Double key = processDescMap.floorKey(time);
        if (key != null)
            return processDescMap.get(key);
        else
            return null;
    }
    
    
    protected boolean storeDataSourceDescription(AbstractProcess process, double time, boolean update)
    {
        if (update)
        {
            AbstractProcess oldProcess = processDescMap.replace(time, process);
            return (oldProcess != null);
        }
        else
        {
            AbstractProcess oldProcess = processDescMap.putIfAbsent(time, process);
            return (oldProcess == null);
        }
    }
    
    
    protected boolean storeDataSourceDescription(AbstractProcess process, boolean update)
    {
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
    public void updateDataSourceDescription(AbstractProcess process)
    {
        storeDataSourceDescription(process, true);
    }


    @Override
    public void removeDataSourceDescription(double time)
    {
        processDescMap.remove(time);
    }


    @Override
    public void removeDataSourceDescriptionHistory(double startTime, double endTime)
    {
        RangeCursor<Double, AbstractProcess> cursor = new RangeCursor<Double, AbstractProcess>(processDescMap, startTime, endTime);
        
        while (cursor.hasNext())
            processDescMap.remove(cursor.next());
    }


    @Override
    public Map<String, ? extends IRecordStoreInfo> getRecordStores()
    {
        return Collections.unmodifiableMap(rsInfoMap);
    }


    @Override
    public void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding)
    {
        DataStreamInfo rsInfo = new DataStreamInfo(name, recordStructure, recommendedEncoding);
        rsInfoMap.put(name, rsInfo);
        loadRecordStore(rsInfo);
    }


    @Override
    public int getNumRecords(String recordType)
    {
        return recordStores.get(recordType).getNumRecords();
    }


    @Override
    public double[] getRecordsTimeRange(String recordType)
    {
        return recordStores.get(recordType).getDataTimeRange();
    }


    @Override
    public Iterator<double[]> getRecordsTimeClusters(String recordType)
    {
        return recordStores.get(recordType).getRecordsTimeClusters();
    }


    @Override
    public DataBlock getDataBlock(DataKey key)
    {
        return recordStores.get(key.recordType).getDataBlock(key);
    }


    @Override
    public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        return recordStores.get(filter.getRecordType()).getDataBlockIterator(filter);
    }


    @Override
    public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter)
    {
        return recordStores.get(filter.getRecordType()).getRecordIterator(filter);
    }


    @Override
    public int getNumMatchingRecords(IDataFilter filter, long maxCount)
    {
        return recordStores.get(filter.getRecordType()).getNumMatchingRecords(filter, maxCount);
    }


    @Override
    public void storeRecord(DataKey key, DataBlock data)
    {
        recordStores.get(key.recordType).store(key, data);
    }


    @Override
    public void updateRecord(DataKey key, DataBlock data)
    {
        recordStores.get(key.recordType).update(key, data);
    }


    @Override
    public void removeRecord(DataKey key)
    {
        recordStores.get(key.recordType).remove(key);
    }


    @Override
    public int removeRecords(IDataFilter filter)
    {
        return recordStores.get(filter.getRecordType()).remove(filter);
    }


    @Override
    public int getNumFois(IFoiFilter filter)
    {
        return featureStore.getNumFeatures();
    }


    @Override
    public Bbox getFoisSpatialExtent()
    {
        return featureStore.getFeaturesSpatialExtent();
    }


    @Override
    public Iterator<String> getFoiIDs(IFoiFilter filter)
    {
        return featureStore.getFeatureIDs(filter);
    }


    @Override
    public Iterator<AbstractFeature> getFois(IFoiFilter filter)
    {
        return featureStore.getFeatures(filter);
    }


    @Override
    public void storeFoi(String producerID, AbstractFeature foi)
    {
        featureStore.store(foi);
    }
}
