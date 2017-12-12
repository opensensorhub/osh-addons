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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.FeatureFilter;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IFeatureFilter;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.ObsFilter;
import org.sensorhub.api.persistence.ObsKey;
import org.sensorhub.impl.persistence.h2.MVFoiTimesStoreImpl.FoiTimePeriod;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockBoolean;
import org.vast.data.DataBlockByte;
import org.vast.data.DataBlockCompressed;
import org.vast.data.DataBlockDouble;
import org.vast.data.DataBlockFloat;
import org.vast.data.DataBlockInt;
import org.vast.data.DataBlockLong;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockParallel;
import org.vast.data.DataBlockShort;
import org.vast.data.DataBlockString;
import org.vast.data.DataBlockTuple;
import org.vast.data.DataBlockUByte;
import org.vast.data.DataBlockUInt;
import org.vast.data.DataBlockUShort;
import com.vividsolutions.jts.geom.Polygon;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


public class MVTimeSeriesImpl implements IRecordStoreInfo
{
    private static final String RECORDS_MAP_NAME = "@records:";
    static final double[] ALL_TIMES = new double[] {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
    
    MVMap<Double, DataBlock> recordIndex;
    MVObsStorageImpl parentStore;
    MVFoiTimesStoreImpl foiTimesStore;
    DataComponent recordDescription;
    DataEncoding recommendedEncoding;
    
    
    static class DataBlockDataType extends KryoDataType
    {
        DataBlockDataType()
        {
            // pre-register all data block types
            registeredClasses.put(10, DataBlockBoolean.class);
            registeredClasses.put(11, DataBlockByte.class);
            registeredClasses.put(12, DataBlockUByte.class);
            registeredClasses.put(13, DataBlockShort.class);
            registeredClasses.put(14, DataBlockUShort.class);
            registeredClasses.put(15, DataBlockInt.class);
            registeredClasses.put(16, DataBlockUInt.class);
            registeredClasses.put(17, DataBlockLong.class);
            registeredClasses.put(18, DataBlockFloat.class);
            registeredClasses.put(19, DataBlockDouble.class);
            registeredClasses.put(20, DataBlockString.class);
            registeredClasses.put(21, AbstractDataBlock[].class);
            registeredClasses.put(22, DataBlockTuple.class);
            registeredClasses.put(23, DataBlockParallel.class);
            registeredClasses.put(24, DataBlockMixed.class);
            registeredClasses.put(25, DataBlockCompressed.class);
        }
    }
    
    
    class IteratorWithFoi implements Iterator<IDataRecord>
    {
        Iterator<FoiTimePeriod> periodIt; 
        Iterator<IDataRecord> recordIt;
        IDataRecord nextRecord;
        String currentFoiID;
        boolean preloadValue;
        
        IteratorWithFoi(Set<FoiTimePeriod>foiTimePeriods, boolean preloadValue)
        {
            this.periodIt = foiTimePeriods.iterator();
            this.preloadValue = preloadValue;
            next();
        }

        @Override
        public final boolean hasNext()
        {
            return nextRecord != null;
        }

        @Override
        public final IDataRecord next()
        {
            IDataRecord rec = nextRecord;
            
            if ((recordIt == null || !recordIt.hasNext()) && periodIt.hasNext())
            {
                // process next time range
                FoiTimePeriod nextPeriod = periodIt.next();
                currentFoiID = nextPeriod.uid;
                recordIt = getEntryIterator(nextPeriod.start, nextPeriod.stop);
            }
            
            // continue processing time range
            if (recordIt != null && recordIt.hasNext())
            {
                nextRecord = recordIt.next();
                ((ObsKey)nextRecord.getKey()).foiID = currentFoiID;
            }
            else
                nextRecord = null;
            
            return rec;
        }
    
        @Override
        public final void remove()
        {
            recordIt.remove();
        }
    }
    
    
    public MVTimeSeriesImpl(MVObsStorageImpl parentStore, String name, DataComponent recordDescription, DataEncoding recommendedEncoding)
    {
        this.parentStore = parentStore;
        this.recordDescription = recordDescription;
        this.recommendedEncoding = recommendedEncoding;
        
        String mapName = RECORDS_MAP_NAME + name;
        this.recordIndex = parentStore.mvStore.openMap(mapName, new MVMap.Builder<Double, DataBlock>().valueType(new DataBlockDataType()));
        this.foiTimesStore = new MVFoiTimesStoreImpl(parentStore.mvStore, mapName);
    }
    
    
    @Override
    public String getName()
    {
        return recordIndex.getName();
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return recordDescription;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return recommendedEncoding;
    }
    
    
    int getNumRecords()
    {
        return recordIndex.size();
    }


    DataBlock getDataBlock(DataKey key)
    {
        return recordIndex.get(key.timeStamp);
    }


    int getNumMatchingRecords(IDataFilter filter, long maxCount)
    {
        if (!(filter instanceof ObsFilter))
        {
            // efficiently count records if only time filter is used
            double[] timeRange = getTimeRange(filter);
            
            Double t0 = recordIndex.ceilingKey(timeRange[0]);
            Double t1 = recordIndex.floorKey(timeRange[1]);
            if (t0 == null || t1 == null)
                return 0;
            
            long i0 = recordIndex.getKeyIndex(t0);
            long i1 = recordIndex.getKeyIndex(t1);        
            return (int)Math.min(i1-i0+1, maxCount);
        }
        else
        {
            // use entry iterator so datablocks are not loaded during scan
            int count = 0;
            Iterator<IDataRecord> it = getEntryIterator(filter, false);
            while (it.hasNext() && count <= maxCount)
            {
                it.next();
                count++;
            }
            
            return count;
        }
    }


    Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
         final Iterator<IDataRecord> it = getEntryIterator(filter, true);
        
        return new Iterator<DataBlock>()
        {
            public final boolean hasNext()
            {
                return it.hasNext();
            }

            public final DataBlock next()
            {
                IDataRecord rec = it.next();
                return rec.getData();
            }

            public final void remove()
            {
                it.remove();
            }
        };
    }


    Iterator<IDataRecord> getRecordIterator(IDataFilter filter)
    {
        // here, even when IObsFilter is not used we scan through FOIs time periods 
        // because we need to read the FOI ID anyway
        
        return getEntryIterator(filter, true);
    }


    void store(DataKey key, DataBlock data)
    {
        recordIndex.putIfAbsent(key.timeStamp, data);
        
        if (key instanceof ObsKey)
        {
            // update FOI times
            String foiID = ((ObsKey)key).foiID;
            if (foiID != null)
            {
                double timeStamp = key.timeStamp;
                foiTimesStore.updateFoiPeriod(foiID, timeStamp);
            }
        }
    }


    void update(DataKey key, DataBlock data)
    {
        recordIndex.replace(key.timeStamp, data);
    }


    void remove(DataKey key)
    {
        recordIndex.remove(key.timeStamp);
    }


    int remove(IDataFilter filter)
    {
        // remove records
        double[] timeRange = filter.getTimeStampRange();
        final RangeCursor<Double, DataBlock> cursor = new RangeCursor<>(recordIndex, timeRange[0], timeRange[1]);        
        int count = 0;
        while (cursor.hasNext())
        {
            // TODO would be nice if cursor remove() was implemented to avoid a get by key everytime
            recordIndex.remove(cursor.next());
            count++;
        }
        
        // remove FOI times
        if (filter instanceof IObsFilter)
        {
            for (String foidID: ((IObsFilter)filter).getFoiIDs())
            {
                // completely remove FOI times if no more records will be left
                if (filter.getTimeStampRange() == null) // || time range contains all foi time ranges
                    foiTimesStore.remove(foidID);
            }
        }
        
        return count;
    }


    double[] getDataTimeRange()
    {
        if (recordIndex.isEmpty())
            return new double[] { Double.NaN, Double.NaN };
        
        return new double[] {recordIndex.firstKey(), recordIndex.lastKey()};
    }
    
    
    public Iterator<double[]> getRecordsTimeClusters()
    {
        final Cursor<Double, DataBlock> cursor = recordIndex.cursor(Double.NEGATIVE_INFINITY);
        
        return new Iterator<double[]>()
        {
            double lastTime = Double.NaN;
            
            public boolean hasNext()
            {
                return cursor.hasNext();
            }

            public double[] next()
            {
                double[] clusterTimeRange = new double[2];
                clusterTimeRange[0] = lastTime;
                
                while (cursor.hasNext())
                {
                    double recTime = cursor.next();
                    
                    if (Double.isNaN(lastTime))
                    {
                        clusterTimeRange[0] = recTime;
                        lastTime = recTime;
                    }
                    else
                    {
                        double dt = recTime - lastTime;
                        lastTime = recTime;
                        if (dt > 60.0)
                            break;
                    }
                    
                    clusterTimeRange[1] = recTime;
                }
                
                return clusterTimeRange;
            }

            public void remove()
            {               
            }    
        };
    }
    
    
    private double[] getTimeRange(IDataFilter filter)
    {
        double[] timeRange = filter.getTimeStampRange();
        if (timeRange != null)
            return timeRange;
        else
            return ALL_TIMES;
    }
    
    
    private Iterator<IDataRecord> getEntryIterator(double begin, double end)
    {
        final RangeCursor<Double, DataBlock> cursor = new RangeCursor<>(recordIndex, begin, end);
        final String recordType = getName();
        
        return new Iterator<IDataRecord>()
        {
            public final boolean hasNext()
            {
                return cursor.hasNext();
            }

            public final IDataRecord next()
            {
                cursor.next();
                final ObsKey key = new ObsKey(recordType, null, cursor.getKey());
                final DataBlock val = cursor.getValue();
                
                return new IDataRecord()
                {
                    public DataKey getKey()
                    {
                        return key;
                    }

                    public DataBlock getData()
                    {
                        return val;
                    }
                };
            }

            public final void remove()
            {
                cursor.remove();
            }
        };
    }
    
    
    private IteratorWithFoi getEntryIterator(IDataFilter filter, boolean preloadValue)
    {
        // get time periods for matching FOIs
        Set<FoiTimePeriod> foiTimePeriods = getFoiTimePeriods(filter);
            
        // if no FOIs have been added just process whole time range
        if (foiTimePeriods == null)
        {
            double[] timeRange = filter.getTimeStampRange();
            double start = Double.NEGATIVE_INFINITY;
            double stop = Double.POSITIVE_INFINITY;
            if (timeRange != null)
            {
                start = filter.getTimeStampRange()[0];
                stop = filter.getTimeStampRange()[1];
            }
            
            foiTimePeriods = new HashSet<>();
            foiTimePeriods.add(new FoiTimePeriod(null, start, stop));
        }
        
        // scan through each time range sequentially
        // but wrap the process with a single iterator
        return new IteratorWithFoi(foiTimePeriods, preloadValue);
    }
    
    
    private Set<FoiTimePeriod> getFoiTimePeriods(final IDataFilter filter)
    {
        // extract FOI filters if any
        Collection<String> foiIDs = null;
        Polygon roi = null;
        if (filter instanceof IObsFilter)
        {
            foiIDs = ((IObsFilter)filter).getFoiIDs();
            if (foiIDs != null && foiIDs.isEmpty())
                foiIDs = null;
            roi = ((IObsFilter) filter).getRoi();
        }
        
        // if using spatial filter, first get matching FOI IDs
        // and then follow normal process
        if (roi != null)
        {
            IFeatureFilter foiFilter = new FeatureFilter()
            {
                public Collection<String> getFeatureIDs()
                {
                    return ((IObsFilter)filter).getFoiIDs();
                }

                public Polygon getRoi()
                {
                    return ((IObsFilter) filter).getRoi();
                }
            };
            
            Iterator<String> foiIt = parentStore.featureStore.getFeatureIDs(foiFilter);
            Collection<String> allFoiIDs = new ArrayList<>(100);
            
            // apply OR between FOI id list and ROI
            // this is not standard compliant but more useful than AND
            if (foiIDs != null)
                allFoiIDs.addAll(foiIDs);
            while (foiIt.hasNext())
                allFoiIDs.add(foiIt.next());
            
            foiIDs = allFoiIDs;
        }
        
        // if no FOIs selected don't compute periods
        if (foiIDs == null)
            return null;
        
        // get time periods for list of FOIs
        Set<FoiTimePeriod> foiTimes = foiTimesStore.getSortedFoiTimes(foiIDs);
        
        // trim periods to filter time range if specified
        double[] timeRange = filter.getTimeStampRange();
        if (timeRange != null)
        {
            Iterator<FoiTimePeriod> it = foiTimes.iterator();
            while (it.hasNext())
            {
                FoiTimePeriod foiPeriod = it.next();
                
                // trim foi period to filter time range
                if (foiPeriod.start < timeRange[0])
                    foiPeriod.start = timeRange[0];
                
                if (foiPeriod.stop > timeRange[1])
                    foiPeriod.stop = timeRange[1];
                                
                // case period is completely outside of time range
                if (foiPeriod.start > foiPeriod.stop)
                    it.remove();
            }
        }
        
        return foiTimes;
    }

}
