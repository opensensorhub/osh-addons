package org.senshorhub.impl.sensor.profiler915;

import java.io.DataInputStream;
import java.io.IOException;

public class ByteSwappedReader 
{
	public static short readSwappedShort (DataInputStream is) throws IOException {
		short value = is.readShort();
		int b1 = value & 0xff;
		int b2 = (value >> 8) & 0xff;

		return (short) (b1 << 8 | b2 << 0);
	}
	public static short readSwappedShort (DataInputStream is, boolean dump) throws IOException {
		if(!dump)
			return readSwappedShort(is);

		short value = is.readShort();
		int b1 = value & 0xff;
		int b2 = (value >> 8) & 0xff;

		System.out.println("value:" + value + ": " + 
				Integer.toHexString(b1) + " " + 
				Integer.toHexString(b2));

		return (short) (b1 << 8 | b2 << 0);
	}

	public static int readSwappedInt (DataInputStream is) throws IOException {
		int value = is.readInt();

		return swap(value);
	}

	public static int readSwappedInt (DataInputStream is, boolean dump) throws IOException {
		if(!dump)
			return readSwappedInt(is);

		int value = is.readInt();
		int b1 = (value >>  0) & 0xff;
		int b2 = (value >>  8) & 0xff;
		int b3 = (value >> 16) & 0xff;
		int b4 = (value >> 24) & 0xff;
		System.out.println("value:" + value + ": " + 
				Integer.toHexString(b1) + " " + 
				Integer.toHexString(b2) + " " + 
				Integer.toHexString(b3) + " " + 
				Integer.toHexString(b4)); 

		return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;
	}


	private static int swap(int value) {
		//  swap from b1b2b3b4 -> b4b3b2b1
		int b1 = (value >>  0) & 0xff;
		int b2 = (value >>  8) & 0xff;
		int b3 = (value >> 16) & 0xff;
		int b4 = (value >> 24) & 0xff;

		return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;
	}

	public static float readSwappedFloat (DataInputStream is) throws IOException {
		float value = is.readFloat();

		int intValue = Float.floatToIntBits (value);
		intValue = swap(intValue);
		return Float.intBitsToFloat (intValue);
	}

	/**
	 * Byte swap a single double value.
	 * 
	 * @param value  Value to byte swap.
	 * @return       Byte swapped representation.
	 */
	public static double readSwappedDouble (DataInputStream is)throws IOException {
		double value = is.readDouble();
		long longValue = Double.doubleToLongBits (value);
		longValue = swap (longValue);
		return Double.longBitsToDouble (longValue);
	}

	/**
	 * Byte swap a single long value.
	 * 
	 * @param value  Value to byte swap.
	 * @return       Byte swapped representation.
	 */
	public static long swap (long value)
	{
		long b1 = (value >>  0) & 0xff;
		long b2 = (value >>  8) & 0xff;
		long b3 = (value >> 16) & 0xff;
		long b4 = (value >> 24) & 0xff;
		long b5 = (value >> 32) & 0xff;
		long b6 = (value >> 40) & 0xff;
		long b7 = (value >> 48) & 0xff;
		long b8 = (value >> 56) & 0xff;

		return b1 << 56 | b2 << 48 | b3 << 40 | b4 << 32 |
				b5 << 24 | b6 << 16 | b7 <<  8 | b8 <<  0;
	}


}

