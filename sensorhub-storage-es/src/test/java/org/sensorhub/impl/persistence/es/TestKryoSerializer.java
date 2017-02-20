package org.sensorhub.impl.persistence.es;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.esotericsoftware.kryo.io.Output;

public class TestKryoSerializer {

	@Test
	public void testSerialize_DataStreamInfo() {
		DataStreamInfo dsi = new DataStreamInfo("test", null, null);
		
		Output output = new Output( new ByteArrayOutputStream());
		KryoSerializer.serialize(dsi,output);
		
		String serializedObject = output.getOutputStream().toString();
		
		System.out.println(serializedObject);
		
		output.close();
	}
}
