/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.h2;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;
import org.objenesis.strategy.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import net.opengis.OgcPropertyList;


public class KryoDataType implements DataType
{
    ThreadLocal<KryoInstance> kryoLocal;
    Map<Integer, Class<?>> registeredClasses = new HashMap<>();
    int averageRecordSize, maxRecordSize;
    
    
    static class KryoInstance
    {
        Kryo kryo;
        Output output;
        Input input;
        
        KryoInstance(Map<Integer, Class<?>> registeredClasses, int bufferSize, int maxBufferSize)
        {
            kryo = new Kryo();
            
            // instantiate classes using default (private) constructor when available
            // or using direct JVM technique when needed
            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
            
            // avoid using collection serializer on OgcPropertyList because
            // the add method doesn't behave as expected
            kryo.addDefaultSerializer(OgcPropertyList.class, FieldSerializer.class);
            
            // pre-register data block classes to reduce storage size
            // don't change the order to stay compatible with old storage files!!
            for (Entry<Integer, Class<?>> entry: registeredClasses.entrySet())                
                kryo.register(entry.getValue(), entry.getKey());
            
            input = new Input();
            output = new Output(bufferSize, maxBufferSize);
        }
    }
    
    
    public KryoDataType()
    {
        this(10*1024*1024);
    }
    
    
    public KryoDataType(final int maxRecordSize)
    {
        this.maxRecordSize = maxRecordSize;
        this.registeredClasses = new HashMap<>();
        
        this.kryoLocal = new ThreadLocal<KryoInstance>()
        {
            public KryoInstance initialValue()
            {
                return new KryoInstance(registeredClasses, 2*averageRecordSize, maxRecordSize);
            }
        };
    }
    
    
    @Override
    public int compare(Object a, Object b)
    {
        // don't care since we won't use this for keys
        return 0;
    }


    @Override
    public int getMemory(Object obj)
    {
        initRecordSize(obj);
        return averageRecordSize;
    }


    @Override
    public void write(WriteBuffer buff, Object obj)
    {
        initRecordSize(obj);
        
        KryoInstance kryoI = kryoLocal.get();
        Kryo kryo = kryoI.kryo;
        Output output = kryoI.output;        
        output.setPosition(0);
        
        //kryo.writeObjectOrNull(output, obj, objectType);
        kryo.writeClassAndObject(output, obj);
        buff.put(output.getBuffer(), 0, output.position());
        
        // adjust the average size using an exponential moving average
        int size = output.position();
        averageRecordSize = (size + averageRecordSize*4) / 5;
    }
    
    
    protected void initRecordSize(Object obj)
    {
        if (averageRecordSize <= 0)
        {
            KryoInstance kryoI = kryoLocal.get();
            Kryo kryo = kryoI.kryo;
            Output output = kryoI.output;
            output.setPosition(0);
            kryo.writeClassAndObject(output, obj);
            averageRecordSize = output.position();
            output.setBuffer(new byte[averageRecordSize*2], maxRecordSize);
        }
    }


    @Override
    public void write(WriteBuffer buff, Object[] obj, int len, boolean key)
    {
        for (int i = 0; i < len; i++)
            write(buff, obj[i]);
    }


    @Override
    public Object read(ByteBuffer buff)
    {
        KryoInstance kryoI = kryoLocal.get();
        Kryo kryo = kryoI.kryo;
        Input input = kryoI.input;
        
        input.setBuffer(buff.array(), buff.position(), buff.remaining());
        //Object obj = kryo.readObjectOrNull(input, objectType);
        Object obj = kryo.readClassAndObject(input);
        buff.position(input.position());
        
        return obj;
    }


    @Override
    public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
    {
        for (int i = 0; i < len; i++)
            obj[i] = read(buff);
    }

}
