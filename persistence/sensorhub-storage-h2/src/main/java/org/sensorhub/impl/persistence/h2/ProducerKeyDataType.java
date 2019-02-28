/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2018 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;

import java.nio.ByteBuffer;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;


/**
 * H2 datatype to create a composite producerID + time stamp index
 */
public class ProducerKeyDataType implements DataType
{

    @Override
    public int compare(Object objA, Object objB)
    {
        ProducerTimeKey a = (ProducerTimeKey)objA;
        ProducerTimeKey b = (ProducerTimeKey)objB;
        
        // first compare ID part of the key, with special null cases
        if (a.producerID == null && b.producerID != null)
            return -1;
        if (a.producerID != null && b.producerID == null)
            return 1;
        
        if (a.producerID != null && b.producerID != null)
        {
            int stringComp = a.producerID.compareTo(b.producerID);
            if (stringComp != 0)
                return stringComp;
        }
        
        // only if IDs are the same, compare timeStamp part
        return Double.compare(a.timeStamp, b.timeStamp);
    }
    

    @Override
    public int getMemory(Object obj)
    {
        ProducerTimeKey key = (ProducerTimeKey)obj;
        if (key.producerID == null)
            return 10;
        else
            return key.producerID.length()*3 + 8;
    }
    

    @Override
    public void write(WriteBuffer buff, Object obj)
    {
        ProducerTimeKey key = (ProducerTimeKey)obj;
        
        if (key.producerID != null)
        {
            short idLength = (short)key.producerID.length();
            buff.putShort(idLength);
            buff.putStringData(key.producerID, idLength);
        }
        else
            buff.putShort((short)0);
        
        buff.putDouble(key.timeStamp);
    }
    

    @Override
    public void write(WriteBuffer buff, Object[] obj, int len, boolean key)
    {
        for (int i=0; i<len; i++)
            write(buff, obj[i]);
    }
    

    @Override
    public Object read(ByteBuffer buff)
    {
        int idLength = buff.getShort();
        
        String producerID = null;
        if (idLength > 0)
            producerID = DataUtils.readString(buff, idLength);
        
        double timeStamp = buff.getDouble();
        return new ProducerTimeKey(producerID, timeStamp);
    }
    

    @Override
    public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
    {
        for (int i=0; i<len; i++)
            obj[i] = read(buff);        
    }

}
