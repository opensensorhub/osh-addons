package org.senshorhub.impl.sensor.profiler915;

public class Profiler915_Header {
	//  mostly fixed parameters
	short revNumber;
	short numBytesInHeader;
	short addtionalReadings;
	short maxNumRadars;
	short maxNumBeamParams;
	short maxNumBeams;
	short maxNumBeamDirs;
	short maxNumBandwidths;
	//  radar id/location info
	String stationName;
	String radarName;
	short lat;
	short lon;
	short minutes;
	short alt;
	short numRadars;
	//  radar operational info
	short idNum;
	int txFreq;
	float maxDutyCycle;
	short maxTxPulseMicrosecs;
	short txPulseOn;
	short numAllowedDirections;
	short numBeamPositions;
	short numBeamParamSets;
	
	//  sampling params
	//  NOTE: For all arrays, MAX is allocated and read, but only num is used, 
	int [] interPulsePeriodNanosecs;
	int [] pulseWidthNanosec;
	int [] delayFirstPulse;  //  from firstPulse to firstFate in Nanos
	int [] gateSpacingNanosecs;
	short [] numGateHeights;
	short [] numCoherentIntegrations;
	short [] numSpectraAveraged;
	short [] numPointsInFFT;
	short [] rxDelay;
	short [] rxBeamwidthCode;
	short [] numAttenuatedRangeGates;
	short [] numPulseCodeBits;
	
	//  Beam Config Params
	String [] dirLabel;
	short [] beamAzimuthAngle;  //  degrees from N
	short [] beamElevationAngle; //  degrees from Horizontal
	short [] steeringCode;
	
	//  Beam Control Params
	short [] directionIndex;
	short [] paramSetIndex;
	short [] numReps;
	
	//  Bandwitdth arrays
	short [] pulseWidthNanosecs_bw;
	short [] rxDelayNanosecs;
	
	short numWindSpectralBins;
	//  Start byte of data in data file corresponding to this header
	int dataStartByteNum;
	//  Start byte of this header in header file- Check that this is accurate, since the header data records 
	//  change for every data record.  It does not seem right.  Should probably use dataStartByteNum somehow
	int headerStartByte;
	
	public void initSampleParamArrays(int size) {
		interPulsePeriodNanosecs = new int[size];
		pulseWidthNanosec = new int[size];
		delayFirstPulse = new int[size];
		gateSpacingNanosecs = new int[size];
		numGateHeights = new short[size];
		numCoherentIntegrations = new short[size];
		numSpectraAveraged = new short[size];
		numPointsInFFT= new short[size];
		rxDelay= new short[size];
		rxBeamwidthCode= new short[size];
		numAttenuatedRangeGates= new short[size];
		numPulseCodeBits= new short[size];
	}
	
	public void initBeamControlParamArrays(int size) {
		directionIndex = new short[size];
		paramSetIndex = new short[size];
		numReps = new short[size];
	}
	
	public void initBeamConfigArrays(int size) {
		dirLabel = new String[size];
		beamAzimuthAngle = new short[size];
		beamElevationAngle = new short[size];
		steeringCode= new short[size];
	}
	
	public void initBandwithArrays(int size) {
		pulseWidthNanosecs_bw = new short[size];
		rxDelayNanosecs= new short[size];
	}
	
}
