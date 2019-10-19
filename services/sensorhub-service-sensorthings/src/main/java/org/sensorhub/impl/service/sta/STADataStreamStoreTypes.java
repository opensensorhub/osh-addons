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
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

/**
 * <p>
 * Internal key data types used by default data stream store implementation.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 16, 2019
 */
class STADataStreamStoreTypes
{
            
    static class MVDataStreamThingKey
    {
        long thingID;
        long dataStreamID;
        
        MVDataStreamThingKey(long thingID, long dsID)
        {
            this.thingID = thingID;
            this.dataStreamID = dsID;
        }
    }
    
    
    static class MVThingDataStreamKeyDataType implements DataType
    {
        private static final int MEM_SIZE = 8+8;
        
        
        @Override
        public int compare(Object objA, Object objB)
        {
            MVDataStreamThingKey a = (MVDataStreamThingKey)objA;
            MVDataStreamThingKey b = (MVDataStreamThingKey)objB;
            
            // first compare thing internal ID
            int comp = Long.compare(a.thingID, b.thingID);
            if (comp != 0)
                return comp;
            
            // then compare datastream ID
            return Long.compare(a.dataStreamID, b.dataStreamID);
        }
        

        @Override
        public int getMemory(Object obj)
        {
            return MEM_SIZE;
        }
        

        @Override
        public void write(WriteBuffer wbuf, Object obj)
        {
            MVDataStreamThingKey key = (MVDataStreamThingKey)obj;
            wbuf.putVarLong(key.thingID);
            wbuf.putVarLong(key.dataStreamID);
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
            long dsID = DataUtils.readVarLong(buff);
            return new MVDataStreamThingKey(thingID, dsID);
        }
        

        @Override
        public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
        {
            for (int i=0; i<len; i++)
                obj[i] = read(buff);        
        }
    }
}
