package org.sensorhub.impl.sensor.nexrad.aws;

/**
 * <p>Title: MomwntDataBlock.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 16, 2016
 */
public class MomentDataBlock {
	public char blockType ;  // always 'D'
	public String blockName;  // REF,VEL, SW< etc
	public short numGates;
	public short rangeToCenterOfFirstGate;
	public short rangeSampleInterval;
	public short rangeFoldingThreshold;
	public short snrThreshold;
	public int controlFlags;
	public int gateSizeBits;
	public float scale;  //  scale to convert from integer to floating point
	public float offset;  //  offset to convert from integer to fp
	byte [] bdata;  // NOTE that for diffPhase, this will be short [] 
	short [] sdata;  
	private float [] data;  //  Convert raw data scaled data:  F = (N - Offset)/scale

	public MomentDataBlock() {
	}
	
	public MomentDataBlock(String name) {
		blockName = name;
	}
	
	public float [] getData() {
		if(data == null) {
			data = new float[numGates];

			for(int i=0; i<numGates; i++) {
				data[i] = ((bdata[i] & 0xFF) - offset) / scale;
			}
		}

		return data;
	}

	// So UcarReader can set data as float []
	public void setData(float [] data) {
		this.data = data;
	}


	public static short byte2short(byte[] data)
	{
		return (short)((data[0]<<8) | (data[1]));
	}
}
