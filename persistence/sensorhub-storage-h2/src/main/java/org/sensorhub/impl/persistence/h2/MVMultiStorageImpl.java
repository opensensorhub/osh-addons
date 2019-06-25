/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListMap;
import org.h2.mvstore.MVMap;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsStorage;
import org.vast.util.Asserts;
import org.vast.util.Bbox;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.swe.v20.DataBlock;


/**
 * <p>
 * H2 MVStore implementation of {@link IMultiSourceStorage} module.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 9, 2017
 */
public class MVMultiStorageImpl extends MVObsStorageImpl implements IMultiSourceStorage<IObsStorage>
{
    private static final String PRODUCERS_MAP_NAME = "@producers";

    MVMap<String, String> dataStoreInfoMap;
    Map<String, MVObsStorageImpl> obsStores = new ConcurrentSkipListMap<>(); // use sorted map so we can iterate in same order as persistent index (more sequential scan)
    
    
    /* to iterate through a list of producers records in parallel while sorting by time */
    abstract class MultiProducerTimeSortIterator<ObjectType> implements Iterator<ObjectType>
    {
        int numProducers;
        Iterator<IDataRecord>[] iterators;
        IDataRecord[] nextRecords;
        IDataRecord nextRecord;
        
        MultiProducerTimeSortIterator(Collection<String> producerIDs)
        {
            this.numProducers = producerIDs.size();
            this.iterators = new Iterator[numProducers];
            this.nextRecords = new IDataRecord[numProducers];
            
            // get first matching record for each producer
            int i = 0;
            for (String producerID: producerIDs)
            {
                Iterator<IDataRecord> it = getSubIterator(producerID);
                iterators[i] = it;
                if (it.hasNext())
                    nextRecords[i] = it.next();
                i++;
            }
            
            // call it once to init things properly
            nextRecord();
        }
        
        @Override
        public final boolean hasNext()
        {
            return nextRecord != null;
        }

        public final IDataRecord nextRecord()
        {
            IDataRecord rec = nextRecord;
            
            int minTimeIndex = -1;
            double minTime = Double.POSITIVE_INFINITY;
            
            // find record with earliest time stamp among producers
            for (int i = 0; i < numProducers; i++)
            {
                IDataRecord candidateRec = nextRecords[i];
                if (candidateRec == null)
                    continue;
                
                double nextTimeStamp = candidateRec.getKey().timeStamp;
                if (nextTimeStamp < minTime)
                {
                    minTime = nextTimeStamp;
                    minTimeIndex = i;
                }
            }
            
            // if a record was found, prepare for next iteration by fetching the next
            // record on the corresponding iterator. Keep all the other ones
            if (minTimeIndex >= 0)
            {
                nextRecord = nextRecords[minTimeIndex];
                Iterator<IDataRecord> recIt = iterators[minTimeIndex];
                if (recIt.hasNext())
                    nextRecords[minTimeIndex] = recIt.next();
                else
                    nextRecords[minTimeIndex] = null;
            }
            else
                nextRecord = null;
            
            return rec;
        }
        
        protected abstract Iterator<IDataRecord> getSubIterator(String producerID);
    
        @Override
        public final void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
    
    
    @Override
    public synchronized void start() throws SensorHubException
    {
        super.start();
        
        // load child data stores
        this.dataStoreInfoMap = mvStore.openMap(PRODUCERS_MAP_NAME, new MVMap.Builder<String, String>());
        for (String producerID: dataStoreInfoMap.keySet())
            loadDataStore(producerID);
    }
    
    
    @Override
    public synchronized void stop() throws SensorHubException
    {
        super.stop();
        
        dataStoreInfoMap = null;
        obsStores.clear();
    }


    private MVObsStorageImpl loadDataStore(String producerID)
    {
        MVObsStorageImpl obsStore;
        try
        {
            obsStore = new MVObsStorageImpl(this, producerID);
            obsStore.init(config);
            obsStores.put(producerID, obsStore);
            return obsStore;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Cannot load datastore for producer " + producerID, e);
        }
    }
    
    
    @Override
    public Collection<String> getProducerIDs()
    {
        checkOpen();
        return Collections.unmodifiableSet(dataStoreInfoMap.keySet());
    }
    

    @Override
    public IObsStorage getDataStore(String producerID)
    {
        checkOpen();
        Asserts.checkNotNull(producerID, "producerID");
                
        MVObsStorageImpl obsStore = obsStores.get(producerID);
        if (obsStore == null)
            throw new IllegalArgumentException("No data store for producer " + producerID);
        return obsStore;
    }
    

    @Override
    public synchronized IObsStorage addDataStore(String producerID)
    {
        checkOpen();
        Asserts.checkNotNull(producerID, "producerID");
        
        dataStoreInfoMap.putIfAbsent(producerID, "");
        return loadDataStore(producerID);
    }


    @Override
    public int getNumRecords(String recordType)
    {
        checkOpen();        
        return getRecordStore(recordType).getNumRecords();
    }


    @Override
    public double[] getRecordsTimeRange(String recordType)
    {
        checkOpen();
        Asserts.checkNotNull(recordType, "recordType");
        
        double[] timeRange = new double[] {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};        
        for (MVObsStorageImpl dataStore: obsStores.values())
        {
            if (dataStore.getRecordStores().containsKey(recordType))
            {
                double[] storeTimeRange = dataStore.getRecordsTimeRange(recordType);
                if (storeTimeRange[0] < timeRange[0])
                    timeRange[0] = storeTimeRange[0];
                if (storeTimeRange[1] > timeRange[1])
                    timeRange[1] = storeTimeRange[1];
            }
        }
        
        if (Double.isInfinite(timeRange[0]))
            timeRange[0] = timeRange[1] = Double.NaN;
        
        return timeRange;
    }
    
    
    @Override
    public int[] getEstimatedRecordCounts(String recordType, double[] timeStamps)
    {
        int [] counts = new int[timeStamps.length-1];
        
        for (MVObsStorageImpl dataStore: obsStores.values())
        {
            int[] producerCounts = dataStore.getEstimatedRecordCounts(recordType, timeStamps);
            for (int i = 0; i < counts.length; i++)
                counts[i] += producerCounts[i];
        }
        
        return counts;
    }


    @Override
    public DataBlock getDataBlock(DataKey key)
    {
        checkOpen();
        Asserts.checkNotNull(key, DataKey.class);
        
        if (key.producerID == null)
            return super.getDataBlock(key);
        else
            return getDataStore(key.producerID).getDataBlock(key);
    }


    @Override
    public Iterator<DataBlock> getDataBlockIterator(final IDataFilter filter)
    {
        checkOpen();
        Asserts.checkNotNull(filter, IDataFilter.class);
        
        // use producer list from filter or use all producers
        Collection<String> producerIDs = filter.getProducerIDs();
        if (producerIDs == null || producerIDs.isEmpty())
            producerIDs = this.getProducerIDs();
        
        return new MultiProducerTimeSortIterator<DataBlock>(producerIDs)
        {
            @Override
            public DataBlock next()
            {
                if (!hasNext())
                    throw new NoSuchElementException();
                return nextRecord().getData();
            }

            @Override
            protected Iterator<IDataRecord> getSubIterator(String producerID)
            {
                return (Iterator<IDataRecord>)getDataStore(producerID).getRecordIterator(filter);
            }
        };
    }


    @Override
    public Iterator<? extends IDataRecord> getRecordIterator(final IDataFilter filter)
    {
        checkOpen();
        Asserts.checkNotNull(filter, IDataFilter.class);
        
        // use producer list from filter or use all producers
        Collection<String> producerIDs = filter.getProducerIDs();
        if (producerIDs == null || producerIDs.isEmpty())
            producerIDs = this.getProducerIDs();
        
        return new MultiProducerTimeSortIterator<IDataRecord>(producerIDs)
        {
            @Override
            public IDataRecord next()
            {
                if (!hasNext())
                    throw new NoSuchElementException();
                return nextRecord();
            }

            @Override
            protected Iterator<IDataRecord> getSubIterator(String producerID)
            {
                return (Iterator<IDataRecord>)getDataStore(producerID).getRecordIterator(filter);
            }
        };
    }


    @Override
    public int getNumMatchingRecords(IDataFilter filter, long maxCount)
    {
        checkOpen();
        Asserts.checkNotNull(filter, IDataFilter.class);
        
        // use producer list from filter or use all producers
        Collection<String> producerIDs = filter.getProducerIDs();
        if (producerIDs == null || producerIDs.isEmpty())
            producerIDs = this.getProducerIDs();
        
        int numRecords = 0;
        for (String producerID: producerIDs)
        {
            numRecords += getDataStore(producerID).getNumMatchingRecords(filter, maxCount);
            if (numRecords > maxCount)
                return numRecords;
        }
        
        return numRecords;
    }


    @Override
    public void storeRecord(DataKey key, DataBlock data)
    {
        checkOpen();
        Asserts.checkNotNull(key, DataKey.class);
        Asserts.checkNotNull(data, DataBlock.class);
        
        if (key.producerID == null)
            super.storeRecord(key, data);
        else
            getDataStore(key.producerID).storeRecord(key, data);
    }


    @Override
    public void updateRecord(DataKey key, DataBlock data)
    {
        checkOpen();
        Asserts.checkNotNull(key, DataKey.class);
        Asserts.checkNotNull(data, DataBlock.class);
        
        if (key.producerID == null)
            super.updateRecord(key, data);
        else
            getDataStore(key.producerID).updateRecord(key, data);
    }


    @Override
    public void removeRecord(DataKey key)
    {
        checkOpen();
        Asserts.checkNotNull(key, DataKey.class);
        
        if (key.producerID == null)
            super.removeRecord(key);
        else
            getDataStore(key.producerID).removeRecord(key);
    }


    @Override
    public int removeRecords(IDataFilter filter)
    {
        checkOpen();
        Asserts.checkNotNull(filter, IDataFilter.class);
        Collection<String> producerIDs = filter.getProducerIDs();
        int numDeleted = 0;
        
        // may need to remove records with no producer ID
        if (producerIDs == null)
            super.removeRecords(filter);
        
        // use producer list from filter or use all producers
        if (producerIDs == null || producerIDs.isEmpty())
            producerIDs = this.getProducerIDs();
                
        for (String producerID: producerIDs) {
            int count = getDataStore(producerID).removeRecords(filter);
            numDeleted += count;
        }
        
        return numDeleted;
    }


    @Override
    public Bbox getFoisSpatialExtent()
    {
        return featureStore.getFeaturesSpatialExtent();
    }


    @Override
    public int getNumFois(IFoiFilter filter)
    {
        checkOpen();
        Asserts.checkNotNull(filter, IFoiFilter.class);
        Collection<String> producerIDs = filter.getProducerIDs();
        
        // if all producers are selected, delegate to main store
        if (producerIDs == null || producerIDs.isEmpty())
            return super.getNumFois(filter);
        
        int numFois = 0;
        for (String producerID: producerIDs)
            numFois += getDataStore(producerID).getNumFois(filter);
        
        return numFois;
    }


    @Override
    public Iterator<String> getFoiIDs(final IFoiFilter filter)
    {
        checkOpen();
        Asserts.checkNotNull(filter, IFoiFilter.class);
        Collection<String> producerIDs = filter.getProducerIDs();
        
        // if all producers are selected, delegate to main store
        if (producerIDs == null || producerIDs.isEmpty())
            return super.getFoiIDs(filter);
        
        // otherwise we're forced to temporarily hold the whole set in memory to remove duplicates
        LinkedHashSet<String> foiIDs = new LinkedHashSet<>();
        for (String producerID: producerIDs)
        {
            Iterator<String> it = getDataStore(producerID).getFoiIDs(filter);
            while (it.hasNext())
                foiIDs.add(it.next());
        }
        
        return foiIDs.iterator();
    }


    @Override
    public Iterator<AbstractFeature> getFois(final IFoiFilter filter)
    {
        checkOpen();
        Asserts.checkNotNull(filter, IFoiFilter.class);
        Collection<String> producerIDs = filter.getProducerIDs();
        
        // use producer list from filter or use all producers
        if (producerIDs == null || producerIDs.isEmpty())
            return super.getFois(filter);
        
        // we're forced to temporarily hold the whole set in memory to remove duplicates
        LinkedHashSet<AbstractFeature> fois = new LinkedHashSet<>();
        for (String producerID: producerIDs)
        {
            Iterator<AbstractFeature> it = getDataStore(producerID).getFois(filter);
            while (it.hasNext())
                fois.add(it.next());
        }
        
        return fois.iterator();
    }


    @Override
    public void storeFoi(String producerID, AbstractFeature foi)
    {
        checkOpen();
        Asserts.checkNotNull(foi, AbstractFeature.class);
        
        if (producerID == null)
            featureStore.store(foi);
        else
            getDataStore(producerID).storeFoi(producerID, foi);
    }
}
