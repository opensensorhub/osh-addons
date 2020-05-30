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
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.sensorhub.api.persistence.DataFilter;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.FeatureFilter;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IFeatureFilter;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.ObsKey;
import org.sensorhub.impl.persistence.IteratorWrapper;
import org.sensorhub.utils.SWEDataUtils;
import org.sensorhub.utils.SWEDataUtils.VectorIndexer;
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
import org.vast.util.Asserts;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;
import net.opengis.swe.v20.DataBlock;


public class MVTimeSeriesImpl
{
    private static final String RECORDS_MAP_NAME = "@records";
    static final double[] ALL_TIMES = new double[] {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
    
    String name;
    MVMap<ProducerTimeKey, DataBlock> recordIndex;
    MVObsStorageImpl parentStore;
    MVFoiTimesStoreImpl foiTimesIndex;
    MVSamplingLocationIndexImpl samplingLocationIndex;
    
    
    static class DataBlockDataType extends KryoDataType
    {
        DataBlockDataType()
        {
            // pre-register known types with Kryo
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
    
    
    class IteratorWithFoi extends IteratorWrapper<IDataRecord, IDataRecord>
    {
        Iterator<ObsTimePeriod> periodIt;
        String currentFoiID;
        
        IteratorWithFoi(Set<ObsTimePeriod>foiTimePeriods)
        {
            super(Collections.<IDataRecord>emptyList().iterator());
            this.periodIt = foiTimePeriods.iterator();
        }

        @Override
        public void preloadNext()
        {
            next = null;
            
            while ((it == null || !it.hasNext()) && periodIt.hasNext())
            {
                // process next time range
                ObsTimePeriod nextPeriod = periodIt.next();
                currentFoiID = nextPeriod.foiID;
                it = getEntryIterator(nextPeriod.producerID, nextPeriod.start, nextPeriod.stop);
            }
            
            // continue processing time range
            if (it != null && it.hasNext())
            {
                next = it.next();
                ((ObsKey)next.getKey()).foiID = currentFoiID;
            }
        }
    }
    
    
    class IteratorWithSpatialFilter extends IteratorWrapper<IDataRecord, IDataRecord>
    {
        PreparedPolygon roi;
        Point point;
        Coordinate coords;
        
        public IteratorWithSpatialFilter(Iterator<IDataRecord> it, IObsFilter filter)
        {
            super(it);
            this.roi = new PreparedPolygon(filter.getRoi());
            this.coords = new Coordinate();
            this.point = new GeometryFactory().createPoint(coords);
        }

        @Override
        protected IDataRecord process(IDataRecord elt)
        {
            DataBlock data = elt.getData();
            coords.x = samplingLocationIndex.locationIndexer.getCoordinateAsDouble(0, data);
            coords.y = samplingLocationIndex.locationIndexer.getCoordinateAsDouble(1, data);
            point.geometryChanged();
            return roi.intersects(point) ? elt : null;
        }        
    }
    
    
    public MVTimeSeriesImpl(MVObsStorageImpl parentStore, String seriesName)
    {
        this.name = seriesName;
        this.parentStore = parentStore;
        
        String mapName = RECORDS_MAP_NAME + ":" + seriesName;        
        this.recordIndex = parentStore.mvStore.openMap(mapName, new MVMap.Builder<ProducerTimeKey, DataBlock>()
                .keyType(new ProducerKeyDataType())
                .valueType(new DataBlockDataType()));
        
        this.foiTimesIndex = new MVFoiTimesStoreImpl(parentStore.mvStore, seriesName);
    }
    
    
    public MVTimeSeriesImpl(MVObsStorageImpl parentStore, IRecordStoreInfo rsInfo)
    {
        this(parentStore, rsInfo.getName());
        
        // try to detect sampling location
        VectorIndexer locationIndexer = SWEDataUtils.getLocationIndexer(rsInfo.getRecordDescription());
        
        // if found, also use sampling lolcation index
        if (locationIndexer != null)
            this.samplingLocationIndex = new MVSamplingLocationIndexImpl(parentStore.mvStore, rsInfo.getName(), locationIndexer);
    }
    
    
    int getNumRecords()
    {
        return recordIndex.size();
    }
    
    
    int getNumRecords(final String producerID)
    {        
        if (producerID != null)
        {
            DataFilter filter = new DataFilter(name) {
                @Override
                public Set<String> getProducerIDs()
                {
                    return Sets.newHashSet(producerID);
                }
            };
            
            return getNumMatchingRecords(filter, Long.MAX_VALUE);
        }
        else
            return getNumRecords();
    }


    DataBlock getDataBlock(DataKey key)
    {
        return recordIndex.get(new ProducerTimeKey(key.producerID, key.timeStamp));
    }
    
    
    String getProducerID(IDataFilter filter)
    {
        String producerID = null;
        if (filter.getProducerIDs() != null)
        {
            Asserts.checkArgument(filter.getProducerIDs().size() <= 1, "Filter must contain exactly one producer ID");
            producerID = filter.getProducerIDs().iterator().next();
        }
        
        return producerID;
    }
    
    
    ProducerTimeKey[] getKeyRange(IDataFilter filter)
    {
        String producerID = getProducerID(filter);
        double[] timeRange = getTimeRange(producerID, filter);
        
        return new ProducerTimeKey[] {
            new ProducerTimeKey(producerID, timeRange[0]),
            new ProducerTimeKey(producerID, timeRange[1])
        };
    }


    int getNumMatchingRecords(IDataFilter filter, long maxCount)
    {
        if (!(filter instanceof IObsFilter))
        {
            // efficiently count records if only time filter is used
            ProducerTimeKey[] keys = getKeyRange(filter);
            ProducerTimeKey t0 = recordIndex.ceilingKey(keys[0]);
            ProducerTimeKey t1 = recordIndex.floorKey(keys[1]);
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
            @Override
            public final boolean hasNext()
            {
                return it.hasNext();
            }

            @Override
            public final DataBlock next()
            {
                IDataRecord rec = it.next();
                return rec.getData();
            }

            @Override
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
        DataBlock oldValue = recordIndex.putIfAbsent(new ProducerTimeKey(key.producerID, key.timeStamp), data);
        
        if (key instanceof ObsKey && oldValue == null)
        {
            // update FOI times
            String foiID = ((ObsKey)key).foiID;
            if (foiID != null)
            {
                double timeStamp = key.timeStamp;
                foiTimesIndex.updateFoiPeriod(key.producerID, foiID, timeStamp);
            }
            
            // update obs location index
            if (samplingLocationIndex != null)
                samplingLocationIndex.update(key, data);
        }
    }


    void update(DataKey key, DataBlock data)
    {
        recordIndex.replace(new ProducerTimeKey(key.producerID, key.timeStamp), data);
    }


    void remove(DataKey key)
    {
        recordIndex.remove(new ProducerTimeKey(key.producerID, key.timeStamp));
    }


    int remove(IDataFilter filter)
    {
        // remove records
        int count = 0;
        Iterator<IDataRecord> it = getEntryIterator(filter, false);
        while (it.hasNext())
        {
            it.next();
            it.remove();
            count++;
        }
        
        // TODO cleanup foi times and sampling location indexes
        
        return count;
    }


    double[] getDataTimeRange(String producerID)
    {
        ProducerTimeKey beforeAll = new ProducerTimeKey(producerID, Double.NEGATIVE_INFINITY);
        ProducerTimeKey afterAll = new ProducerTimeKey(producerID, Double.POSITIVE_INFINITY);
        ProducerTimeKey first = recordIndex.ceilingKey(beforeAll);
        ProducerTimeKey last = recordIndex.floorKey(afterAll);
        
        if (first == null || last == null)
            return new double[] { Double.NaN, Double.NaN };
        else
            return new double[] {first.timeStamp, last.timeStamp};
    }
    
    
    Iterator<double[]> getRecordsTimeClusters(String producerID)
    {
        ProducerTimeKey beforeAll = new ProducerTimeKey(producerID, Double.NEGATIVE_INFINITY);
        final Cursor<ProducerTimeKey, DataBlock> cursor = recordIndex.cursor(beforeAll);
        
        return new Iterator<double[]>()
        {
            double lastTime = Double.NaN;
            
            @Override
            public boolean hasNext()
            {
                return cursor.hasNext();
            }

            @Override
            public double[] next()
            {
                double[] clusterTimeRange = new double[2];
                clusterTimeRange[0] = lastTime;
                
                while (cursor.hasNext())
                {
                    double recTime = cursor.next().timeStamp;
                    
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

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }    
        };
    }
    
    
    int[] getEstimatedRecordCounts(String producerID, double[] timeStamps)
    {
        int[] bins = new int[timeStamps.length];
        
        boolean first = true;
        long lastKeyIndex = 0;        
        double lastKeyTime = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < timeStamps.length; i++)
        {
            double time = timeStamps[i];
            
            // skip bin if last key already too high for this slot
            if (lastKeyTime > time)
                continue;
            
            ProducerTimeKey timeKey = new ProducerTimeKey(producerID, time);
            ProducerTimeKey key = (i == 0) ? recordIndex.ceilingKey(timeKey) : recordIndex.floorKey(timeKey);
            
            // we're done if no more keys can be found for this producer
            if (key == null || !key.producerID.equals(producerID))
                break;
            
            long keyIndex = recordIndex.getKeyIndex(key);
            if (i > 0)
            {
                int count = (int)(keyIndex - lastKeyIndex);
                bins[i-1] = count + (first ? 1 : 0);
                first = false;
            }
            
            lastKeyIndex = keyIndex;
            lastKeyTime = key.timeStamp;
        }
                
        return bins;
    }
    
    
    private double[] getTimeRange(String producerID, IDataFilter filter)
    {
        double[] timeRange = filter.getTimeStampRange();
        if (timeRange != null)
        {
            // special case when requesting latest record
            if (timeRange[0] == Double.POSITIVE_INFINITY && timeRange[1] == Double.POSITIVE_INFINITY)
            {
                ProducerTimeKey afterAll = new ProducerTimeKey(producerID, Double.POSITIVE_INFINITY);
                ProducerTimeKey lastProducerKey = recordIndex.floorKey(afterAll);
                return new double[] {lastProducerKey.timeStamp, lastProducerKey.timeStamp};
            }
            else
                return timeRange;
        }
        else
            return ALL_TIMES;
    }
    
    
    private Iterator<IDataRecord> getEntryIterator(final String producerID, double begin, double end)
    {
        final ProducerTimeKey beginKey = new ProducerTimeKey(producerID, begin);
        final ProducerTimeKey endKey = new ProducerTimeKey(producerID, end);
        final RangeCursor<ProducerTimeKey, DataBlock> cursor = new RangeCursor<>(recordIndex, beginKey, endKey);
        final String recordType = this.name;
        
        return new Iterator<IDataRecord>()
        {
            @Override
            public final boolean hasNext()
            {
                return cursor.hasNext();
            }

            @Override
            public final IDataRecord next()
            {
                cursor.next();
                final ObsKey key = new ObsKey(recordType, producerID, null, cursor.getKey().timeStamp);
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

            @Override
            public final void remove()
            {
                //cursor.remove(); // not supported
                recordIndex.remove(cursor.getKey());
            }
        };
    }
    
    
    private Iterator<IDataRecord> getEntryIterator(IDataFilter filter, boolean preloadValue)
    {
        String producerID = getProducerID(filter);
        
        // get time periods matching filter
        Set<ObsTimePeriod> obsTimePeriods = getObsTimePeriods(producerID, filter);
            
        // return special iterator to scan through each time range sequentially
        // optionally post-filtering records on their spatial location      
        if (samplingLocationIndex != null && filter instanceof IObsFilter && ((IObsFilter)filter).getRoi() != null)
            return new IteratorWithSpatialFilter(new IteratorWithFoi(obsTimePeriods), (IObsFilter)filter);
        else
            return new IteratorWithFoi(obsTimePeriods);
    }
    
    
    private Set<ObsTimePeriod> getObsTimePeriods(String producerID, final IDataFilter filter)
    {
        double[] timeRange = getTimeRange(producerID, filter);
        
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
        
        // if using spatial filter and NOT indexing obs locations, first get
        // matching FOI IDs and then follow normal process
        if (roi != null && samplingLocationIndex == null)
        {
            IFeatureFilter foiFilter = new FeatureFilter()
            {
                @Override
                public Set<String> getFeatureIDs()
                {
                    return ((IObsFilter)filter).getFoiIDs();
                }

                @Override
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
        
        // get time periods for list of FOIs
        Set<ObsTimePeriod> obsTimeRanges = null;
        if (foiIDs != null)
        {
            obsTimeRanges = foiTimesIndex.getSortedFoiTimes(producerID, foiIDs);
            if (obsTimeRanges.isEmpty())
                return Collections.emptySet();
        }
        
        // get time periods for spatially indexed observations
        if (roi != null && samplingLocationIndex != null)
        {
            Set<ObsTimePeriod> samplingTimeRanges = samplingLocationIndex.getObsTimePeriods(producerID, timeRange, roi);
            
            // intersect with foi time periods if any
            if (obsTimeRanges != null)
            {
                Iterator<ObsTimePeriod> it = samplingTimeRanges.iterator();
                Iterator<ObsTimePeriod> foiIt = obsTimeRanges.iterator();
                ObsTimePeriod foiTimeRange = foiIt.next();
                
                while (it.hasNext())
                {
                    ObsTimePeriod obsTimePeriod = it.next();
                    
                    boolean intersect = false;                    
                    while (foiTimeRange != null)
                    {
                        if (obsTimePeriod.stop < foiTimeRange.start)
                            break;
                            
                        if (trimTimeRange(obsTimePeriod, foiTimeRange.start, foiTimeRange.stop))
                        {
                            intersect = true;
                            obsTimePeriod.foiID = foiTimeRange.foiID;
                            break;
                        }
                        
                        foiTimeRange = foiIt.hasNext() ? foiIt.next() : null;
                    }
                                            
                    if (!intersect)
                        it.remove();
                }
            }
            
            obsTimeRanges = samplingTimeRanges;
        }
        
        // if no time periods selected, just process whole time range
        if (obsTimeRanges == null)
            obsTimeRanges = Sets.newHashSet(new ObsTimePeriod(producerID, null, timeRange[0], timeRange[1]));
        
        // trim periods to time range specified in filter
        Iterator<ObsTimePeriod> it = obsTimeRanges.iterator();
        while (it.hasNext())
        {
            ObsTimePeriod obsTimePeriod = it.next();
            if (!trimTimeRange(obsTimePeriod, timeRange[0], timeRange[1]))
                it.remove();
        }
        
        return obsTimeRanges;
    }
    
    
    boolean trimTimeRange(ObsTimePeriod obsTimePeriod, double start, double stop)
    {
        // trim foi period to filter time range
        if (obsTimePeriod.start < start)
            obsTimePeriod.start = start;
        
        if (obsTimePeriod.stop > stop)
            obsTimePeriod.stop = stop;
                        
        // return false if period completely outside time range
        return obsTimePeriod.start <= obsTimePeriod.stop;
    }
    
    
    Iterator<String> getFoiIDs(String producerID)
    {
        return foiTimesIndex.getFoiIDs(producerID);
    }
    
    
    void delete()
    {
        foiTimesIndex.delete();
        if (samplingLocationIndex != null)
            samplingLocationIndex.delete();
        recordIndex.getStore().removeMap(recordIndex);
    }
    
    
    void close()
    {
        if (samplingLocationIndex != null)
            samplingLocationIndex.close();
    }

}
