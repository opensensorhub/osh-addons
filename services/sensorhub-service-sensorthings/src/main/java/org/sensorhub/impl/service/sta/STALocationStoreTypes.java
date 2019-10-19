/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import java.nio.ByteBuffer;
import java.time.Instant;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;
import org.sensorhub.impl.datastore.h2.H2Utils;
import org.sensorhub.impl.service.sta.ILocationStore.IHistoricalLocation;


/**
 * <p>
 * Key and filter types used by default location store implementation.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 16, 2019
 */
class STALocationStoreTypes
{
    
    static class MVThingLocationKey implements IHistoricalLocation
    {
        long thingID;
        long locationID;
        Instant time;        
        
        MVThingLocationKey(long thingID, long locationID)
        {
            this.thingID = thingID;
            this.locationID = locationID;
        }
        
        MVThingLocationKey(long thingID, long locationID, Instant time)
        {
            this.thingID = thingID;
            this.locationID = locationID;
            this.time = time;
        }

        @Override
        public long getThingID()
        {
            return thingID;
        }

        @Override
        public Instant getTime()
        {
            return time;
        }
    }
    
    
    static class MVThingLocationKeyDataType implements DataType
    {
        private static final int MEM_SIZE = 8+8+8+4;        
        
        @Override
        public int compare(Object objA, Object objB)
        {
            MVThingLocationKey a = (MVThingLocationKey)objA;
            MVThingLocationKey b = (MVThingLocationKey)objB;
            
            // first compare thing internal ID
            int comp = Long.compare(a.thingID, b.thingID);
            if (comp != 0)
                return comp;
            
            // then compare time stamp
            // sort in reverse order so that latest version is always first
            comp = -a.time.compareTo(b.time);
            if (comp != 0)
                return comp;
            
            // then compare location ID
            return Long.compare(a.locationID, b.locationID);
        }        

        @Override
        public int getMemory(Object obj)
        {
            return MEM_SIZE;
        }        

        @Override
        public void write(WriteBuffer wbuf, Object obj)
        {
            MVThingLocationKey key = (MVThingLocationKey)obj;
            wbuf.putVarLong(key.thingID);
            wbuf.putVarLong(key.locationID);
            H2Utils.writeInstant(wbuf, key.time);
        }        

        @Override
        public void write(WriteBuffer wbuf, Object[] obj, int len, boolean key)
        {
            for (int i=0; i<len; i++)
                write(wbuf, obj[i]);
        }        

        @Override
        public Object read(ByteBuffer buff)
        {
            long thingID = DataUtils.readVarLong(buff);
            long locationID = DataUtils.readVarLong(buff);
            Instant time = H2Utils.readInstant(buff);
            return new MVThingLocationKey(thingID, locationID, time);
        }        

        @Override
        public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
        {
            for (int i=0; i<len; i++)
                obj[i] = read(buff);        
        }
    }
    
    
    static class MVLocationThingKeyDataType extends MVThingLocationKeyDataType
    {
        
        @Override
        public int compare(Object objA, Object objB)
        {
            MVThingLocationKey a = (MVThingLocationKey)objA;
            MVThingLocationKey b = (MVThingLocationKey)objB;
            
            // first compare location internal ID
            int comp = Long.compare(a.locationID, b.locationID);
            if (comp != 0)
                return comp;
            
            // then compare time stamp
            // sort in reverse order so that latest version is always first
            comp = -a.time.compareTo(b.time);
            if (comp != 0)
                return comp;
            
            // then compare thing ID
            return Long.compare(a.thingID, b.thingID);
        }
    }
}