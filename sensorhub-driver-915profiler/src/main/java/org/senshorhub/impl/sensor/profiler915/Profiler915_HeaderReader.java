package org.senshorhub.impl.sensor.profiler915;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Profiler915_HeaderReader 
{
	String headerFile;
	Map <Integer, Profiler915_Header> headerMap;
	int byteIndex;

	public Profiler915_HeaderReader(){
		headerMap = new HashMap<Integer, Profiler915_Header>();
	}
	
	public void setHeaderFile(String file){
		this.headerFile = file;
	}
	
	public void readHeader() throws IOException {
		DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(headerFile)));
		boolean eof = false;
		byteIndex = 0;
		
		Profiler915_Header headerTmp;
		while(!eof) {
			headerTmp = readHeaderRecord(is);
			if(headerTmp == null)
				break;
			headerMap.put(headerTmp.headerStartByte, headerTmp);
		}
		is.close();
	}
	
	protected Profiler915_Header readHeaderRecord(DataInputStream is) throws IOException { 
		Profiler915_Header header = new Profiler915_Header();
		
		//  Test for EOF on first field
		try {
			header.revNumber = ByteSwappedReader.readSwappedShort(is);
		} catch (EOFException e) {
			// TODO Auto-generated catch block
			System.out.println("Header EOF reached");
			return null;
		}
		header.numBytesInHeader = ByteSwappedReader.readSwappedShort(is);
		header.headerStartByte = byteIndex;
		byteIndex += header.numBytesInHeader;
		header.addtionalReadings = ByteSwappedReader.readSwappedShort(is);
		header.maxNumRadars = ByteSwappedReader.readSwappedShort(is);
		header.maxNumBeamParams = ByteSwappedReader.readSwappedShort(is);
		header.maxNumBeams = ByteSwappedReader.readSwappedShort(is);
		header.maxNumBeamDirs = ByteSwappedReader.readSwappedShort(is);
		header.maxNumBandwidths = ByteSwappedReader.readSwappedShort(is);
		header.stationName = readString(is,32);
		
		//  Radar Operational Params
		header.lat = ByteSwappedReader.readSwappedShort(is);
		header.lon = ByteSwappedReader.readSwappedShort(is);
		header.minutes = ByteSwappedReader.readSwappedShort(is);
		header.alt = ByteSwappedReader.readSwappedShort(is);
		header.numRadars = ByteSwappedReader.readSwappedShort(is);

		header.radarName = readString(is,32);
		
		header.idNum = ByteSwappedReader.readSwappedShort(is);
		header.txFreq = ByteSwappedReader.readSwappedInt(is);  // MHz x 100
		header.maxDutyCycle = ByteSwappedReader.readSwappedFloat(is);
		header.maxTxPulseMicrosecs = ByteSwappedReader.readSwappedShort(is);
		header.txPulseOn = ByteSwappedReader.readSwappedShort(is);  //  on = 1
		header.numAllowedDirections = ByteSwappedReader.readSwappedShort(is);
		header.numBeamPositions = ByteSwappedReader.readSwappedShort(is);
		header.numBeamParamSets = ByteSwappedReader.readSwappedShort(is);
		
		//  Sampling Params
		header.initSampleParamArrays(header.maxNumBeamParams);
		for(int i = 0; i<header.maxNumBeamParams; i++){
			header.interPulsePeriodNanosecs[i] = ByteSwappedReader.readSwappedInt(is);
			header.pulseWidthNanosec[i] = ByteSwappedReader.readSwappedInt(is);
			header.delayFirstPulse[i] = ByteSwappedReader.readSwappedInt(is);
			header.gateSpacingNanosecs[i] = ByteSwappedReader.readSwappedInt(is);
			header.numGateHeights[i] = ByteSwappedReader.readSwappedShort(is);
			header.numCoherentIntegrations[i] = ByteSwappedReader.readSwappedShort(is);
			header.numSpectraAveraged[i] = ByteSwappedReader.readSwappedShort(is);
			header.numPointsInFFT[i] = ByteSwappedReader.readSwappedShort(is);
			header.rxDelay[i] = ByteSwappedReader.readSwappedShort(is);
			header.rxBeamwidthCode[i] = ByteSwappedReader.readSwappedShort(is);
			header.numAttenuatedRangeGates[i] = ByteSwappedReader.readSwappedShort(is);
			header.numPulseCodeBits[i] = ByteSwappedReader.readSwappedShort(is);
		}

		//  Beam Control Params	- -save these later if needed
		header.initBeamControlParamArrays(header.maxNumBeams);
		for(int i = 0; i<header.maxNumBeams; i++){
			header.directionIndex[i] = ByteSwappedReader.readSwappedShort(is);
			header.paramSetIndex[i] = ByteSwappedReader.readSwappedShort(is);
			header.numReps[i] = ByteSwappedReader.readSwappedShort(is);
		}
		
		int i1 = ByteSwappedReader.readSwappedInt(is);
		int i2 = ByteSwappedReader.readSwappedInt(is);
		int i3 = ByteSwappedReader.readSwappedInt(is);
		int i4= ByteSwappedReader.readSwappedInt(is);
		int i5 =ByteSwappedReader.readSwappedInt(is);

		//  Beam config params
		header.initBeamConfigArrays(header.maxNumBeamDirs);
		for(int i = 0; i<header.maxNumBeamDirs; i++) {
			header.dirLabel[i] = readString(is, 12);
			header.beamAzimuthAngle[i] = ByteSwappedReader.readSwappedShort(is);
			header.beamElevationAngle[i] = ByteSwappedReader.readSwappedShort(is);
			header.steeringCode[i] = ByteSwappedReader.readSwappedShort(is);
		}
		
		//  BeamWidth params
		header.initBandwithArrays(header.maxNumBandwidths);
		for(int i = 0; i<header.maxNumBandwidths; i++) {
			header.pulseWidthNanosecs_bw[i] = ByteSwappedReader.readSwappedShort(is);
			header.rxDelayNanosecs[i] = ByteSwappedReader.readSwappedShort(is);
		}
		
		// 18 shorts - save these later if needed
		short s1 = ByteSwappedReader.readSwappedShort(is);
		short s2 = ByteSwappedReader.readSwappedShort(is);
		short s3 = ByteSwappedReader.readSwappedShort(is);
		short s4 = ByteSwappedReader.readSwappedShort(is);
		short s5 = ByteSwappedReader.readSwappedShort(is);
		header.numWindSpectralBins = ByteSwappedReader.readSwappedShort(is);
		short s6 = ByteSwappedReader.readSwappedShort(is);
		short s7 = ByteSwappedReader.readSwappedShort(is);
		short s8 = ByteSwappedReader.readSwappedShort(is);
		short s9 = ByteSwappedReader.readSwappedShort(is);
		short s10 = ByteSwappedReader.readSwappedShort(is);
		short s11 = ByteSwappedReader.readSwappedShort(is);
		short s12 = ByteSwappedReader.readSwappedShort(is);
		short s13 = ByteSwappedReader.readSwappedShort(is);
		short s14 = ByteSwappedReader.readSwappedShort(is);
		short s15 = ByteSwappedReader.readSwappedShort(is);
		short s16 = ByteSwappedReader.readSwappedShort(is);
		short s17 = ByteSwappedReader.readSwappedShort(is);
		
		//  reserved for future use
		readString(is,44);
		
		//  startByteNumber in Data file for the record corresponding to this header
		//  always 0? so ignore
		header.dataStartByteNum = ByteSwappedReader.readSwappedInt(is);
		//System.err.println("StartByte: " + Long.toHexString(header.dataStartByteNum));
		return header;
	}
	
	protected String readString(DataInputStream is, int numBytes) throws IOException {
		StringBuffer sbuff = new StringBuffer(numBytes);
		byte [] b = new byte[numBytes];
		is.readFully(b);
		for(int i=0;i < numBytes;  i++) {
			if(b[i] == 0x0)
				break;
			sbuff.append((char)b[i]);
		}
		
		return sbuff.toString();
	}
	

	public static void main(String [] args) throws IOException 
	{
		Profiler915_HeaderReader reader = new Profiler915_HeaderReader();
		
		reader.setHeaderFile("C:/tcook/kk/915/data/H09042a.SPC");
		
		reader.readHeader();
	}
}

