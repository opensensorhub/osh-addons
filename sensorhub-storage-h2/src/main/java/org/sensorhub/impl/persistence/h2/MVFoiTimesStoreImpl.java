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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
    String lastFoi;
    
    
    static class FeatureTimesDataType extends KryoDataType
    {
        FeatureTimesDataType()
        {
            // pre-register used types with Kryo
            registeredClasses.put(10, FeatureEntry.class);
            registeredClasses.put(11, FoiTimePeriod.class);
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
        String uid;
        double start;
        double stop;
        
        FoiTimePeriod(String uid, double start, double stop)
        {
            this.uid = uid;
            this.start = start;
            this.stop = stop;
        }
    }
    
    
    static class FoiTimePeriodComparator implements Comparator<FoiTimePeriod>
    {
        public int compare(FoiTimePeriod p0, FoiTimePeriod p1)
        {
            return (int)Math.signum(p0.start - p1.start);
        }        
    }    
    
    
    MVFoiTimesStoreImpl(MVStore mvStore, String name)
    {
        String mapName = FOI_TIMES_MAP_NAME + name;
        this.idIndex = mvStore.openMap(mapName, new MVMap.Builder<String, FeatureEntry>().valueType(new FeatureTimesDataType()));
    }
    
    
    Set<FoiTimePeriod> getSortedFoiTimes(Collection<String> uids)
    {
        // create set with custom comparator for sorting FoiTimePeriod objects
        TreeSet<FoiTimePeriod> foiTimes = new TreeSet<>(new FoiTimePeriodComparator());
        
        // TODO handle case of overlaping FOI periods?
        
        if (uids != null)
        {
            for (String uid: uids)
            {
                FeatureEntry fEntry = idIndex.get(uid);
                if (fEntry == null)
                    continue;
                
                // add each period to sorted set
                for (double[] timePeriod: fEntry.timePeriods)
                    foiTimes.add(new FoiTimePeriod(uid, timePeriod[0], timePeriod[1]));
            }
        }
        else // no filtering on FOI ID -> select them all
        {
            for (FeatureEntry fEntry: idIndex.values())
            {
                String uid = fEntry.uid;
                
                // add each period to sorted set
                for (double[] timePeriod: fEntry.timePeriods)
                    foiTimes.add(new FoiTimePeriod(uid, timePeriod[0], timePeriod[1]));
            }
        }
        
        return foiTimes;
    }
    
    
    void updateFoiPeriod(String uid, double timeStamp)
    {
        // if lastFoi is null (after restart), set to the one for which we last received data
        if (lastFoi == null)
        {
            double latestTime = Double.NEGATIVE_INFINITY;
            for (FeatureEntry entry: idIndex.values())
            {
                int nPeriods = entry.timePeriods.size();
                if (entry.timePeriods.get(nPeriods-1)[1] > latestTime)
                    lastFoi = entry.uid;
            }
        }
        
        FeatureEntry entry = idIndex.get(uid);
        if (entry == null)
            entry = new FeatureEntry(uid);
        
        // if same foi, keep growing period
        if (uid.equals(lastFoi))
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
        idIndex.put(uid, entry);
        
        // remember current FOI
        lastFoi = uid;
    }
    
    
    void remove(String uid)
    {
        idIndex.remove(uid);
    }    
}
