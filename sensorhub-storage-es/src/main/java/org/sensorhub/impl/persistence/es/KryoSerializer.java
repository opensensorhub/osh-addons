package org.sensorhub.impl.persistence.es;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.objenesis.strategy.StdInstantiatorStrategy;
import org.sensorhub.impl.persistence.es.KryoDataType.KryoInstance;
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

public class KryoSerializer {
	private static final ThreadLocal<Kryo> kryoLocal = new ThreadLocal<Kryo>() {
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

	public static byte[] serialize(Object object) {
		//kryoLocal.get().writeClassAndObject(output, object);
		
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
		
		// return serialized data
		return result;
	}

	public static <T> T deserialize(byte[] serializedData) {
		ByteArrayInputStream bis = new ByteArrayInputStream(serializedData);
	    Input ki = new Input(bis);
	    T result = (T) kryoLocal.get().readClassAndObject(ki);
	    ki.close();
	    return result;
	}
}
