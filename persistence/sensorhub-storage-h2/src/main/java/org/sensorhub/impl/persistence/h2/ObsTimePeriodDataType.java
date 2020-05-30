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


public class ObsTimePeriodDataType implements DataType
{

    @Override
    public int compare(Object objA, Object objB)
    {
        // not used as key
        return 0;
    }
    

    @Override
    public int getMemory(Object obj)
    {
        ObsTimePeriod val = (ObsTimePeriod)obj;
        if (val.producerID == null)
            return 18;
        else
            return val.producerID.length()*3 + 18;
    }
    

    @Override
    public void write(WriteBuffer buff, Object obj)
    {
        ObsTimePeriod val = (ObsTimePeriod)obj;
        
        if (val.producerID != null)
        {
            short idLength = (short)val.producerID.length();
            buff.putShort(idLength);
            buff.putStringData(val.producerID, idLength);
        }
        else
            buff.putShort((short)0);
        
        buff.putDouble(val.start);
        buff.putDouble(val.stop);
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
        
        double start = buff.getDouble();
        double stop = buff.getDouble();
        return new ObsTimePeriod(producerID, null, start, stop);
    }
    

    @Override
    public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
    {
        for (int i=0; i<len; i++)
            obj[i] = read(buff);        
    }

}
