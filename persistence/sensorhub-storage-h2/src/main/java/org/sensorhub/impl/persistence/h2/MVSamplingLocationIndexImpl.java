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

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.rtree.MVRTreeMap;
import org.h2.mvstore.rtree.SpatialKey;
import org.h2.mvstore.rtree.SpatialDataType;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.utils.SWEDataUtils.VectorIndexer;
import org.vast.util.Bbox;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;
import net.opengis.swe.v20.DataBlock;


/**
 * <p>
 * Spatial index of observation time ranges used to implement fast query of
 * observations with changing spatial location
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jun 10, 2019
 */
class MVSamplingLocationIndexImpl
{
    private static final String SAMPLING_AREAS_MAP_NAME = "@obsAreas";
    private static final int MAX_RECORDS_PER_CLUSTER = 100;
    private static final int MAX_COMMIT_PERIOD = 10000; // millis
    
    VectorIndexer locationIndexer;
    MVRTreeMap<ObsTimePeriod> clusterIndex;
    Map<String, ProducerCacheInfo> clusterCache = new ConcurrentHashMap<>();
    //private DateTimeFormat df = new DateTimeFormat();
    
    static class ProducerCacheInfo
    {
        ObsTimePeriod producerTimeRange;
        Bbox bbox = new Bbox();
        int numRecords;
        long lastCommitTime;
    }
    
    
    MVSamplingLocationIndexImpl(MVStore mvStore, String seriesName, VectorIndexer locationIndexer)
    {
        this.locationIndexer = locationIndexer;
        
        String mapName = SAMPLING_AREAS_MAP_NAME + ":" + seriesName;
        this.clusterIndex = mvStore.openMap(mapName, new MVRTreeMap.Builder<ObsTimePeriod>()
                .valueType(new ObsTimePeriodDataType()));
    }
    
    
    void update(DataKey key, DataBlock data)
    {
        double x = locationIndexer.getCoordinateAsDouble(0, data);
        double y = locationIndexer.getCoordinateAsDouble(1, data);
        long now = System.currentTimeMillis();
        
        // retrieve previously cached info or create new one
        ProducerCacheInfo cachedInfo = clusterCache.get(key.producerID);
        if (cachedInfo == null)
        {
            cachedInfo = new ProducerCacheInfo();
            cachedInfo.lastCommitTime = now;
            clusterCache.put(key.producerID, cachedInfo);
        }
        
        // update cached producer cluster info
        if (cachedInfo.producerTimeRange == null)
            cachedInfo.producerTimeRange = new ObsTimePeriod(key.producerID, key.timeStamp, key.timeStamp);
        else if (key.timeStamp < cachedInfo.producerTimeRange.start)
            cachedInfo.producerTimeRange.start = key.timeStamp;
        else if (key.timeStamp > cachedInfo.producerTimeRange.stop)
            cachedInfo.producerTimeRange.stop = key.timeStamp;
        cachedInfo.bbox.resizeToContain(x, y, 0.0);
        cachedInfo.numRecords++;
        
        // only update index in storage if records count in cluster reached threshold        
        if (cachedInfo.numRecords >= MAX_RECORDS_PER_CLUSTER || now - cachedInfo.lastCommitTime >= MAX_COMMIT_PERIOD)
        {            
            /*System.out.println("Commit cluster: " + 
                    "time=[" + cachedInfo.producerTimeRange.start + "-" + cachedInfo.producerTimeRange.stop + "]" +
                    ", bbox=" + cachedInfo.bbox);*/
            clusterIndex.add(getSpatialKey(cachedInfo), cachedInfo.producerTimeRange);
            
            // reset cached info
            cachedInfo.numRecords = 0;
            cachedInfo.producerTimeRange = null;
            cachedInfo.bbox.nullify();
            cachedInfo.lastCommitTime = now;
        }
    }
    
    
    SpatialKey getSpatialKey(ProducerCacheInfo info)
    {        
        float xmin = (float)info.bbox.getMinX();
        float xmax = Math.nextUp((float)info.bbox.getMaxX());
        float ymin = (float)info.bbox.getMinY();
        float ymax = Math.nextUp((float)info.bbox.getMaxY());
        int hashID = Objects.hash(info.producerTimeRange.producerID, info.producerTimeRange.start);
        return new SpatialKey(hashID, xmin, xmax, ymin, ymax);
    }
    
    
    SpatialKey getSpatialKey(Polygon roi)
    {        
        Envelope env = roi.getEnvelopeInternal();
        return new SpatialKey(0, (float)env.getMinX(), Math.nextUp((float)env.getMaxX()),
                                 (float)env.getMinY(), Math.nextUp((float)env.getMaxY()),
                                 Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    }
    
    
    RTreeCursor<ObsTimePeriod> getCursor(SpatialKey bbox)
    {
        //return clusterIndex.findIntersectingKeys(bbox);
        return new RTreeCursor<ObsTimePeriod>(clusterIndex.getRoot(), bbox) {
            @Override
            protected boolean check(boolean leaf, SpatialKey key,
                    SpatialKey test) {
                return ((SpatialDataType)clusterIndex.getKeyType()).isOverlap(key, test);
            }
        };
    }
    
    
    Set<ObsTimePeriod> getObsTimePeriods(final String producerID, final double[] timeRange, final Polygon roi)
    {
        TreeSet<ObsTimePeriod> obsTimes = new TreeSet<>(new ObsTimePeriod.Comparator());
        
        // first check cached obs cluster
        boolean checkStorage = true;
        ProducerCacheInfo cachedInfo = clusterCache.get(producerID);
        if (cachedInfo != null && cachedInfo.producerTimeRange != null)
        {
            if (cachedInfo.bbox.toJtsPolygon().intersects(roi) &&
                cachedInfo.producerTimeRange.start <= timeRange[1] &&
                cachedInfo.producerTimeRange.stop >= timeRange[0])
                obsTimes.add(cachedInfo.producerTimeRange);
            
            // check storage only if requesting data before current cluster time extent
            // this especially prevents lookup in storage for 'latest records' request
            checkStorage = (timeRange[0] < cachedInfo.producerTimeRange.start);
        }
        
        // check storage only if time range goes further than current cluster time range
        if (checkStorage)
        {
            // get spatial key for roi bounding rectangle
            SpatialKey bbox = getSpatialKey(roi);
            
            // iterate with cursor and post-filter on producerID and time range
            RTreeCursor<ObsTimePeriod> geoCursor = getCursor(bbox);
            while (geoCursor.hasNext())
            {
                geoCursor.next();
                ObsTimePeriod obsCluster = geoCursor.getValue();
                
                if (!obsCluster.producerID.equals(producerID) ||
                    obsCluster.start > timeRange[1] ||
                    obsCluster.stop < timeRange[0])
                    continue;
                
                obsTimes.add(obsCluster);
            }
        }
        
        return obsTimes;
    }
    
    
    void remove(IDataFilter filter)
    {
        RTreeCursor<ObsTimePeriod> geoCursor;
        
        if (filter instanceof IObsFilter && ((IObsFilter)filter).getRoi() != null)
        {
            SpatialKey bbox = getSpatialKey(((IObsFilter)filter).getRoi());
            geoCursor = getCursor(bbox);
        }
        else
        {
            // otherwise use cursor to scan the whole index
            geoCursor = new RTreeCursor<ObsTimePeriod>(clusterIndex.getRoot(), null);
        }
        
        // iterate with cursor and remove items matching filter
        while (geoCursor.hasNext())
        {
            SpatialKey key = geoCursor.next();
            ObsTimePeriod obsCluster = geoCursor.getValue();
            maybeRemoveEntry(filter, key, obsCluster);
        }
    }
    
    
    private void maybeRemoveEntry(IDataFilter filter, SpatialKey key, ObsTimePeriod obsCluster)
    {
        // don't remove if producer is not in the filter list 
        if (filter.getProducerIDs() != null && !filter.getProducerIDs().contains(obsCluster.producerID))
            return;
        
        // don't remove if FOI is not in the filter list
        if (filter instanceof IObsFilter &&
            ((IObsFilter)filter).getFoiIDs() != null && !((IObsFilter)filter).getFoiIDs().contains(obsCluster.foiID))
            return;
        
        // don't remove if filter time range does not fully include the cluster
        if (filter.getTimeStampRange() != null)
        {
            if (filter.getTimeStampRange()[0] > obsCluster.start ||
                filter.getTimeStampRange()[1] < obsCluster.stop)
            {
                /*System.out.println(
                    "Skipping spatial index entry: " +
                    df.formatIso(obsCluster.start, 0) + "/" +
                    df.formatIso(obsCluster.stop, 0));*/
                return;
            }
        }
        
        // remove cluster
        /*System.out.println(
            "Removing spatial index entry: " +
            df.formatIso(obsCluster.start, 0) + "/" +
            df.formatIso(obsCluster.stop, 0));*/
        clusterIndex.remove(key);
    }
    
    
    void cleanupProducer(String producerID)
    {
        clusterCache.remove(producerID);
    }
    
    
    void cleanupOrphanEntries(Set<String> producerIDs)
    {
        // iterate with cursor and remove all items w/ unknown producer ID
        RTreeCursor<ObsTimePeriod> geoCursor = new RTreeCursor<>(clusterIndex.getRoot(), null);
        while (geoCursor.hasNext())
        {
            SpatialKey key = geoCursor.next();
            ObsTimePeriod obsCluster = geoCursor.getValue();
            if (!producerIDs.contains(obsCluster.producerID))
                clusterIndex.remove(key);
        }
        
    }
    
    
    void delete()
    {
        clusterIndex.getStore().removeMap(clusterIndex);
    }
    
    
    void close()
    {
        for (ProducerCacheInfo cachedInfo: clusterCache.values())
        {
            if (cachedInfo.numRecords > 0)
            {
                /*System.out.println("Close - Commit cluster: " + 
                        "time=[" + cachedInfo.producerTimeRange.start + "-" + cachedInfo.producerTimeRange.stop + "]" +
                        ", bbox=" + cachedInfo.bbox);*/
                clusterIndex.add(getSpatialKey(cachedInfo), cachedInfo.producerTimeRange);
            }
        }
    }
}
