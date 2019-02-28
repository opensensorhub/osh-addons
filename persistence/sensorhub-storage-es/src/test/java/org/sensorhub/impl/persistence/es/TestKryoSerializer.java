package org.sensorhub.impl.persistence.es;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;
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

public class TestKryoSerializer {

	@Test
	public void testSerialize_DataStreamInfo() throws IOException {
		DataStreamInfo dsi = new DataStreamInfo("test", null, null);
		
		/*Kryo kryo = new Kryo();
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
		
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Output output = new Output(bos);
		kryo.writeClassAndObject(output, dsi);
		output.flush();
		
		byte[] ser = bos.toByteArray();
		
		System.out.println(new String(ser));
		
		// Deserialize.
	    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
	    Input ki = new Input(bis);
	    DataStreamInfo des = (DataStreamInfo)kryo.readClassAndObject(ki);		
		System.out.println(des.getName());*/
		
		byte[] serializedData = KryoSerializer.serialize(dsi);
		System.out.println(new String(serializedData));
		
		DataStreamInfo dsi2 = KryoSerializer.<DataStreamInfo>deserialize(serializedData);
		System.out.println(dsi2.getName());
	}
}
