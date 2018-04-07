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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;


/**
 * <p>
 * Implementation of FoI observation periods
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 10, 2017
 */
class MVFoiTimesStoreImpl
{
    private static final String FOI_TIMES_MAP_NAME = "@foiTimes";
    
    MVMap<String, FeatureEntry> idIndex;
    Map<String, String> lastFois = new HashMap<>(); // last FOI for each producer
    
    
    static class FeatureTimesDataType extends KryoDataType
    {
        FeatureTimesDataType()
        {
            // pre-register used types with Kryo
            registeredClasses.put(10, FeatureEntry.class);
        }
    }
    
    
    static class FeatureEntry
    {
        String uid;
        List<double[]> timePeriods = new ArrayList<>();
        
        FeatureEntry(String uid)
        {
            this.uid = uid;
        }
    }
    
    
    static class FoiTimePeriod
    {
        String producerID;
        String foiID;
        double start;
        double stop;
        
        FoiTimePeriod(String producerID, String foiID, double start, double stop)
        {
            this.producerID = producerID;
            this.foiID = foiID;
            this.start = start;
            this.stop = stop;
        }
    }
    
    
    static class FoiTimePeriodComparator implements Comparator<FoiTimePeriod>
    {
        public int compare(FoiTimePeriod p0, FoiTimePeriod p1)
        {
            return Double.compare(p0.start, p1.start);
        }        
    }    
    
    
    MVFoiTimesStoreImpl(MVStore mvStore, String seriesName)
    {
        String mapName = FOI_TIMES_MAP_NAME + ":" + seriesName;
        this.idIndex = mvStore.openMap(mapName, new MVMap.Builder<String, FeatureEntry>().valueType(new FeatureTimesDataType()));
    }
    
    
    String getKey(String producerID, String foiID)
    {
        if (producerID == null)
            return foiID;
        else
            return producerID + foiID;
    }
    
    
    String[] getProducerKeyRange(String producerID)
    {
        String from = getKey(producerID, "");
        String to = getKey(producerID, "\uFFFF");
        return new String[] {from, to};
    }
    
    
    int getNumFois(String producerID)
    {
        String[] keyRange = getProducerKeyRange(producerID);
        
        String firstKey = idIndex.ceilingKey(keyRange[0]);
        String lastKey = idIndex.floorKey(keyRange[1]);
        if (firstKey == null || lastKey == null)
            return 0;
        
        long i0 = idIndex.getKeyIndex(firstKey);
        long i1 = idIndex.getKeyIndex(lastKey);
        return (int)(i1 - i0);
    }
    
    
    Iterator<String> getFoiIDs(String producerID)
    {
        String[] keyRange = getProducerKeyRange(producerID);
        return new RangeCursor<>(idIndex, keyRange[0], keyRange[1]);
    }
    
    
    Set<FoiTimePeriod> getSortedFoiTimes(final String producerID, final Collection<String> foiIDs)
    {
        // create set with custom comparator for sorting FoiTimePeriod objects
        TreeSet<FoiTimePeriod> foiTimes = new TreeSet<>(new FoiTimePeriodComparator());
        
        // TODO handle case of overlaping FOI periods?
        
        if (foiIDs != null)
        {
            for (String foiID: foiIDs)
            {
                String key = getKey(producerID, foiID);
                FeatureEntry fEntry = idIndex.get(key);
                if (fEntry == null)
                    continue;
                
                // add each period to sorted set
                for (double[] timePeriod: fEntry.timePeriods)
                    foiTimes.add(new FoiTimePeriod(producerID, foiID, timePeriod[0], timePeriod[1]));
            }
        }
        else // no filtering on FOI ID -> select them all
        {
            for (FeatureEntry fEntry: idIndex.values())
            {
                String foiID = fEntry.uid;
                
                // add each period to sorted set
                for (double[] timePeriod: fEntry.timePeriods)
                    foiTimes.add(new FoiTimePeriod(producerID, foiID, timePeriod[0], timePeriod[1]));
            }
        }
        
        return foiTimes;
    }
    
    
    void updateFoiPeriod(final String producerID, final String foiID, double timeStamp)
    {
        // if lastFois doesn't have it (after restart)
        // add producer FOI for which we last received data
        String lastFoi = lastFois.get(producerID);
        if (lastFoi == null)
        {
            String firstKey = getKey(producerID, "");
            Cursor<String, FeatureEntry> cursor = idIndex.cursor(firstKey);
            
            double latestTime = Double.NEGATIVE_INFINITY;
            while (cursor.hasNext() && cursor.next().startsWith(firstKey))
            {
                FeatureEntry entry = cursor.getValue();
                int nPeriods = entry.timePeriods.size();
                if (entry.timePeriods.get(nPeriods-1)[1] > latestTime)
                    lastFoi = entry.uid;
            }
        }
        
        // create new entry if needed
        String key = getKey(producerID, foiID);
        FeatureEntry entry = idIndex.get(key);
        if (entry == null)
            entry = new FeatureEntry(foiID);
        
        // if same foi, keep growing period
        if (foiID.equals(lastFoi))
        {
            int numPeriods = entry.timePeriods.size();
            double[] lastPeriod = entry.timePeriods.get(numPeriods-1);
            double currentEndTime = lastPeriod[1];
            if (timeStamp > currentEndTime)
                lastPeriod[1] = timeStamp;
        }
        
        // otherwise start new period
        else
            entry.timePeriods.add(new double[] {timeStamp, timeStamp});
        
        // replace old entry
        idIndex.put(key, entry);
        
        // remember current FOI
        lastFois.put(producerID, foiID);
    }
    
    
    void remove(String producerID, String foiID)
    {
        String key = getKey(producerID, foiID);
        idIndex.remove(key);
    }
}
