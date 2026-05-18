/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.es;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.objenesis.strategy.StdInstantiatorStrategy;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockByte;
import org.vast.data.DataBlockDouble;
import org.vast.data.DataBlockFloat;
import org.vast.data.DataBlockInt;
import org.vast.data.DataBlockLong;
import org.vast.data.DataBlockParallel;
import org.vast.data.DataBlockShort;
import org.vast.data.DataBlockString;
import org.vast.data.DataBlockTuple;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

import net.opengis.OgcPropertyList;

/**
 * <p>
 * Kryo Serializer/Deserializer.
 * </p>
 *
 * @author Mathieu Dhainaut <mathieu.dhainaut@gmail.com>
 * @since 2017
 */
public class KryoSerializer {
    
	private static final ThreadLocal<Kryo> kryoLocal = new ThreadLocal<Kryo>() {
		@Override
		protected Kryo initialValue() {
			Kryo kryo = new Kryo();
			kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
			kryo.addDefaultSerializer(OgcPropertyList.class, FieldSerializer.class);

			kryo.register(AbstractDataBlock[].class);
			kryo.register(DataBlockTuple.class);
			kryo.register(DataBlockParallel.class);
			kryo.register(DataBlockByte.class);
			kryo.register(DataBlockShort.class);
			kryo.register(DataBlockInt.class);
			kryo.register(DataBlockLong.class);
			kryo.register(DataBlockFloat.class);
			kryo.register(DataBlockDouble.class);
			kryo.register(DataBlockString.class);
			kryo.register(DataStreamInfo.class);
			return kryo;
		};
	};
	
	private KryoSerializer() {	    
	}

	public static Kryo getInstance() {
		return kryoLocal.get();
	}
	
	public static byte[] serialize(Object object) {
		// create buffer
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Output output = new Output(bos);
		
		// write into buffer
		kryoLocal.get().writeClassAndObject(output, object);
		output.flush();
		
		// get serialized data
		byte[] result = bos.toByteArray();
		
		// close buffer
		output.close();
		
		// 
		// return serialized data
		return result;
	}

	public static <T> T deserialize(byte[] serializedData) {
		// create buffer
		ByteArrayInputStream bis = new ByteArrayInputStream(serializedData);
	    Input ki = new Input(bis);
	    
	    // read from buffer
	    T result = (T) kryoLocal.get().readClassAndObject(ki);
	    
	    // close buffer
	    ki.close();
	    
	    // return deserialized data
	    return result;
	}
}
