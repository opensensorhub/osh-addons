/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.persistence.h2;

import java.io.Serializable;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.FileStore;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreTool;


public class TestMVStore
{

    static class Record implements Serializable
    {
        public int id;
        public double[] pos;
    }
    
    
    public static void main(String[] args)
    {
        final String fileName = "mvtest.dat";
        
        MVStore.Builder builder = new MVStore.Builder()
                .fileName(fileName)
                .cacheSize(1)
                .autoCommitBufferSize(256);
        MVStore mvStore = builder.open();
        mvStore.setRetentionTime(1000);
        mvStore.setVersionsToKeep(0);
        
        MVMap<Integer, Record> map1 = mvStore.openMap("map1");
        
        final int numRecords = 1000000;
        final int printInterval = numRecords/10;
        
        
        long t0 = System.currentTimeMillis();
        for (int i=0; i<numRecords; i++)
        {
            Record rec = new Record();
            rec.id = i;
            rec.pos = new double[] {i, 2*i, 3*i};
            
            map1.put(rec.id, rec);
            
            if (i%printInterval == 0)
                System.out.println(i);
        }
        
        mvStore.close();
        System.out.format("Inserts done. %d rec/s\n\n", 1000*numRecords/(System.currentTimeMillis()-t0));
        
        mvStore = builder.open();
        map1 = mvStore.openMap("map1");
        
        // cursor read
        t0 = System.currentTimeMillis();
        Cursor<Integer, Record> c = map1.cursor(0);
        while (c.hasNext())
        {
            int key = c.next();
            if (key%printInterval == 0)
                System.out.println(key);
        }
        System.out.format("Cursor read done. %d rec/s\n\n", 1000*numRecords/(System.currentTimeMillis()-t0));
                
        // sequential read
        t0 = System.currentTimeMillis();
        for (int i=0; i<numRecords; i++)
        {
            int key = i;
            Record rec = map1.get(key);
            if (i%printInterval == 0)
                System.out.println(i);
        }
        System.out.format("Sequential read done. %d rec/s\n\n", 1000*numRecords/(System.currentTimeMillis()-t0));
        
        // random read
        t0 = System.currentTimeMillis();
        for (int i=0; i<numRecords; i++)
        {
            int key = (int)(Math.random()*numRecords);
            Record rec = map1.get(key);
            if (i%printInterval == 0)
                System.out.println(i);
        }
        System.out.format("Random read done. %d rec/s\n\n", 1000*numRecords/(System.currentTimeMillis()-t0));
        
        /*System.out.println((map1.getKeyIndex(1000)-map1.getKeyIndex(100)));
        
        for (int i=101; i<200; i++)
        {
            map1.remove(i);
            mvStore.commit();
        }
        
        System.out.println((map1.getKeyIndex(1000)-map1.getKeyIndex(100)));*/
        
        /*map1.clear();
        //mvStore.compactRewriteFully();
        mvStore.close();
        
        MVStoreTool.compact(fileName, false);
        MVStoreTool.info(fileName);*/
        
    }
}
