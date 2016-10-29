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

import java.util.Iterator;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


public class MVTimeSeriesImpl implements IRecordStoreInfo
{
    static final double[] ALL_TIMES = new double[] {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
    MVMap<Double, DataBlock> recordIndex;
    DataComponent recordDescription;
    DataEncoding recommendedEncoding;
    
    
    public MVTimeSeriesImpl(MVStore mvStore, String name, DataComponent recordDescription, DataEncoding recommendedEncoding)
    {
        this.recordDescription = recordDescription;
        this.recommendedEncoding = recommendedEncoding;
        this.recordIndex = mvStore.openMap(name, new MVMap.Builder<Double, DataBlock>().valueType(new KryoDataType()));
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


    Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        double[] timeRange = getTimeRange(filter);
        final RangeCursor<Double, DataBlock> cursor = new RangeCursor<Double, DataBlock>(recordIndex, timeRange[0], timeRange[1]);
        
        return new Iterator<DataBlock>()
        {
            public final boolean hasNext()
            {
                return cursor.hasNext();
            }

            public final DataBlock next()
            {
                cursor.next();
                return cursor.getValue();
            }

            public final void remove()
            {
                cursor.remove();
            }
        };
    }


    int getNumMatchingRecords(IDataFilter filter, long maxCount)
    {
        double[] timeRange = getTimeRange(filter);
        final RangeCursor<Double, DataBlock> cursor = new RangeCursor<Double, DataBlock>(recordIndex, timeRange[0], timeRange[1]);
                
        int count = 0;
        while (cursor.hasNext() && count <= maxCount)
        {
            cursor.next();
            count++;
        }
        
        return count;
    }


    Iterator<IDataRecord> getRecordIterator(IDataFilter filter)
    {
        double[] timeRange = getTimeRange(filter);
        final RangeCursor<Double, DataBlock> cursor = new RangeCursor<Double, DataBlock>(recordIndex, timeRange[0], timeRange[1]);
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
                final DataKey key = new DataKey(recordType, cursor.getKey());
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
    
    
    protected double[] getTimeRange(IDataFilter filter)
    {
        double[] timeRange = filter.getTimeStampRange();
        if (timeRange != null)
            return timeRange;
        else
            return ALL_TIMES;
    }


    void store(DataKey key, DataBlock data)
    {
        recordIndex.putIfAbsent(key.timeStamp, data);
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
        double[] timeRange = filter.getTimeStampRange();
        final RangeCursor<Double, DataBlock> cursor = new RangeCursor<Double, DataBlock>(recordIndex, timeRange[0], timeRange[1]);
        
        int count = 0;
        while (cursor.hasNext())
        {
           // TODO would be nice if cursor remove() was implemented to avoid a get by key everytime
            recordIndex.remove(cursor.next());
            count++;
        }
        
        return count;
    }


    double[] getDataTimeRange()
    {
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

}
