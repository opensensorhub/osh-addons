/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.garret.perst.IPersistentMap;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;


/**
 * <p>
 * PERST implementation of FoI observation periods
 * </p>
 *
 * @author Alex Robin
 * @since May 8, 2015
 */
class FoiTimesStoreImpl extends Persistent
{
    class FeatureEntry
    {
        String uid;
        List<double[]> timePeriods = new ArrayList<double[]>();
        
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
    
    
    class FoiTimePeriodComparator implements Comparator<FoiTimePeriod>
    {
        public int compare(FoiTimePeriod p0, FoiTimePeriod p1)
        {
            return (int)Math.signum(p0.start - p1.start);
        }        
    }
        
    
    IPersistentMap<String, FeatureEntry> idIndex;
    transient String lastFoi;
    
    
    // default constructor needed by PERST on Android JVM
    FoiTimesStoreImpl() {}

    
    FoiTimesStoreImpl(Storage db)
    {
        super(db);
        idIndex = db.createMap(String.class);
    }
    
    
    Set<FoiTimePeriod> getSortedFoiTimes(Collection<String> uids)
    {
        try
        {
            idIndex.sharedLock();
            
            // create set with custom comparator for sorting FoiTimePeriod objects
            TreeSet<FoiTimePeriod> foiTimes = new TreeSet<FoiTimePeriod>(new FoiTimePeriodComparator());
            
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
        finally
        {
            idIndex.unlock();
        }
    }
    
    
    void updateFoiPeriod(String uid, double timeStamp)
    {
        try
        {
            idIndex.exclusiveLock();
            
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
            {
                entry = new FeatureEntry(uid);
                idIndex.put(uid, entry);
            }
            
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
            
            // mark entry as modified so changes can be commited
            getStorage().modify(entry);
            
            // remember current FOI
            lastFoi = uid;
        }
        finally
        {
            idIndex.unlock();
        }
    }
    
    
    void remove(String uid)
    {
        try
        {
            idIndex.exclusiveLock();
            idIndex.remove(uid);
        }
        finally
        {
            idIndex.unlock();
        }
    }    
}
