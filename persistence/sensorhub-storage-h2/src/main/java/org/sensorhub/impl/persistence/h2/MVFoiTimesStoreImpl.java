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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.impl.persistence.IteratorWrapper;


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
    Map<String, FoiInfo> lastFois = new ConcurrentHashMap<>(); // last FOI for each producer
    
    
    static class FoiInfo
    {
        String uid;
        double lastTimeStamp;
    }
    
    
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
            return producerID + "|" + foiID;
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
        String keyPrefix = keyRange[0];
        
        String firstKey = idIndex.ceilingKey(keyRange[0]);
        String lastKey = idIndex.floorKey(keyRange[1]);
        if (firstKey == null || !firstKey.startsWith(keyPrefix) ||
            lastKey == null || !lastKey.startsWith(keyPrefix))
            return 0;
        
        long i0 = idIndex.getKeyIndex(firstKey);
        long i1 = idIndex.getKeyIndex(lastKey);
        return (int)(i1 - i0 + 1);
    }
    
    
    Iterator<String> getFoiIDs(String producerID)
    {
        String[] keyRange = getProducerKeyRange(producerID);
        Iterator<String> cursor = new RangeCursor<>(idIndex, keyRange[0], keyRange[1]);
        
        // wrap to remove producer prefix and return clean FOI IDs
        return new IteratorWrapper<String, String>(cursor) {
            @Override
            protected String process(String key)
            {
                return key.substring(key.indexOf('|')+1);
            }
        };
    }
    
    
    Set<ObsTimePeriod> getSortedFoiTimes(final String producerID, final Collection<String> foiIDs)
    {
        // create set with custom comparator for sorting FoiTimePeriod objects
        TreeSet<ObsTimePeriod> foiTimes = new TreeSet<>(new ObsTimePeriod.Comparator());
        
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
                    foiTimes.add(new ObsTimePeriod(producerID, foiID, timePeriod[0], timePeriod[1]));
            }
        }
        else // no filtering on FOI ID -> select them all
        {
            for (FeatureEntry fEntry: idIndex.values())
            {
                String foiID = fEntry.uid;
                
                // add each period to sorted set
                for (double[] timePeriod: fEntry.timePeriods)
                    foiTimes.add(new ObsTimePeriod(producerID, foiID, timePeriod[0], timePeriod[1]));
            }
        }
        
        return foiTimes;
    }
    
    
    void updateFoiPeriod(final String producerID, final String foiID, double timeStamp)
    {
        // if lastFois has no value for producer (first update or after restart)
        // look for latest FOI observed by this producer
        String nonNullProducerID = producerID != null ? producerID : "";
        FoiInfo lastFoi = lastFois.get(nonNullProducerID);
        if (lastFoi == null)
        {
            lastFoi = new FoiInfo();
            lastFois.put(nonNullProducerID, lastFoi);
            
            String firstKey = getKey(producerID, "");
            Cursor<String, FeatureEntry> cursor = idIndex.cursor(firstKey);
                        
            double latestTime = Double.NEGATIVE_INFINITY;
            while (cursor.hasNext() && cursor.next().startsWith(firstKey))
            {
                FeatureEntry entry = cursor.getValue();
                int nPeriods = entry.timePeriods.size();
                double foiStopTime = entry.timePeriods.get(nPeriods-1)[1];
                if (foiStopTime > latestTime)
                {
                    lastFoi.uid = entry.uid;
                    latestTime = foiStopTime;
                }
            }
            
            if (latestTime == Double.POSITIVE_INFINITY)
                lastFoi.lastTimeStamp = timeStamp - 1e-3;
            else
                lastFoi.lastTimeStamp = latestTime;
        }
        
        // create or update entry only if FOI has changed
        if (!foiID.equals(lastFoi.uid))
        {
            // close period of last FOI
            closeLastFoiPeriod(producerID, lastFoi);
            
            // retrieve or create entry for current FOI
            String key = getKey(producerID, foiID);
            FeatureEntry entry = idIndex.get(key);
            if (entry == null)
                entry = new FeatureEntry(foiID);
            
            // add new observed period valid until 'now'
            entry.timePeriods.add(new double[] {timeStamp, Double.POSITIVE_INFINITY});
            idIndex.put(key, entry);
        }
        
        lastFoi.uid = foiID;
        lastFoi.lastTimeStamp = timeStamp;
    }
    
    
    private void closeLastFoiPeriod(String producerID, FoiInfo lastFoi)
    {
        String key = getKey(producerID, lastFoi.uid);
        FeatureEntry entry = idIndex.get(key);
        if (entry != null)
        {
            int numPeriods = entry.timePeriods.size();
            if (numPeriods > 0)
            {
                double[] lastPeriod = entry.timePeriods.get(numPeriods-1);
                lastPeriod[1] = lastFoi.lastTimeStamp;
            }
            idIndex.put(key, entry);
        }
    }
    
    
    void remove(IDataFilter filter)
    {
        // if filtering on producer IDs, scan only these sections of the index
        if (filter.getProducerIDs() != null)
        {
            for (String producerID: filter.getProducerIDs())
            {
                String[] keyRange = getProducerKeyRange(producerID);
                RangeCursor<String, FeatureEntry> cursor = new RangeCursor<>(idIndex, keyRange[0], keyRange[1]);
                
                while (cursor.hasNext())
                {
                    String key = cursor.next();
                    FeatureEntry entry = cursor.getValue();
                    maybeRemoveEntry(filter, key, entry);
                }
            }
        }
        
        // otherwise do a full index scan
        else
        {
            for (Entry<String, FeatureEntry> entry: idIndex.entrySet())
                maybeRemoveEntry(filter, entry.getKey(), entry.getValue());
        }
    }
    
    
    private void maybeRemoveEntry(IDataFilter filter, String key, FeatureEntry entry)
    {
        // don't do anything if FOI is not in the filter list
        if (filter instanceof IObsFilter &&
            ((IObsFilter)filter).getFoiIDs() != null && !((IObsFilter)filter).getFoiIDs().contains(entry.uid))
            return;
        
        // check time filter to see if we should remove only certain time periods
        if (filter.getTimeStampRange() != null)
        {
            Iterator<double[]> it = entry.timePeriods.iterator();
            boolean modified = false;
            while (it.hasNext())
            {
                double[] period = it.next();
                
                // don't remove if filter time range does not fully include the period
                if (filter.getTimeStampRange()[0] > period[0] ||
                    filter.getTimeStampRange()[1] < period[1])
                    continue;
                
                it.remove();
                modified = true;
            }
            
            if (!modified)
                return;
            
            // reinsert entry if there are some periods left
            // otherwise it will get fully removed
            if (!entry.timePeriods.isEmpty())
            {
                idIndex.put(key, entry);
                return;
            }
        }
        
        // else fully remove it
        idIndex.remove(key);
    }
    
    
    void remove(String producerID, String foiID)
    {
        String key = getKey(producerID, foiID);
        idIndex.remove(key);
    }
    
    
    void cleanupProducer(String producerID)
    {
        lastFois.remove(producerID);
        
        // remove all remaining producer entries
        String[] keyRange = getProducerKeyRange(producerID);
        RangeCursor<String, FeatureEntry> cursor = new RangeCursor<>(idIndex, keyRange[0], keyRange[1]);
        while (cursor.hasNext())
        {
            String key = cursor.next();
            idIndex.remove(key);
        }
    }
    
    
    void delete()
    {
        idIndex.getStore().removeMap(idIndex);
    }
}
