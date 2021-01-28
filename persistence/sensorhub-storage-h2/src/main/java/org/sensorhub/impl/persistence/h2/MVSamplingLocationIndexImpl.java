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
import org.h2.mvstore.rtree.MVRTreeMap.RTreeCursor;
import org.sensorhub.api.persistence.DataKey;
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
            cachedInfo.producerTimeRange = new ObsTimePeriod(key.producerID, key.timeStamp);
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
    
    
    Set<ObsTimePeriod> getObsTimePeriods(final String producerID, final double[] timeRange, final Polygon roi)
    {
        TreeSet<ObsTimePeriod> obsTimes = new TreeSet<>(new ObsTimePeriod.Comparator());
        
        // first check cached obs cluster
        boolean checkStorage = true;
        ProducerCacheInfo cachedInfo = clusterCache.get(producerID);
        if (cachedInfo != null)
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
            // iterate through spatial index using bounding rectangle
            Envelope env = roi.getEnvelopeInternal();
            SpatialKey bbox = new SpatialKey(0, (float)env.getMinX(), Math.nextUp((float)env.getMaxX()),
                                                (float)env.getMinY(), Math.nextUp((float)env.getMaxY()),
                                                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
            //System.out.println(bbox);
            final RTreeCursor geoCursor = clusterIndex.findIntersectingKeys(bbox);
            
            // wrap with iterator to filter on producerID
            while (geoCursor.hasNext())
            {
                SpatialKey key = geoCursor.next();
                ObsTimePeriod obsCluster = clusterIndex.get(key);
                if (!obsCluster.producerID.equals(producerID) ||
                    obsCluster.start > timeRange[1] ||
                    obsCluster.stop < timeRange[0])
                    continue;
                
                /*System.out.println("Select cluster " + producerID + ": " +
                        obsCluster.start + " -> " + obsCluster.stop);*/
                obsTimes.add(obsCluster);
            }
        }
        
        return obsTimes;
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
