package org.senshorhub.impl.sensor.profiler915;

public class Profile915 extends Profile
{
	static final double DTR = Math.PI / 180.0;

	//  Header stuff
	protected Profiler915_Header header;

	int headerStartByte;
	protected double sysTime;
	short radarIndex;
	short beamArrayIndex;
	short numCoherentIntegrations;
	short numSpectraInAverage;
	
	//  Doppler data
	private int numGates;
	
	float [] dopplerRaw;
	float [] swRaw;
	float [] snr;
	float [] noise;
	float [] spectralWidth;
	float [] dopplerVelocity;

	//  Computed params
	float [] gateAgl;  //  km
	float [] corrVel;
	float  nyquist;
	
	//  Spectral Data
	private int numSpectralBins;
	float spectralData [][];
	int spectralDataByteStart;
	
	public void initializeArrays(int numGates, int numBins){
		//  Get from Header...
		this.setNumberOfGates(numGates);
		this.setNumSpectralBins(numBins);
	}
	
	private void setNumberOfGates(int numGates){
		this.numGates = numGates;
		this.dopplerVelocity = new float[numGates];
		this.dopplerRaw = new float[numGates];
		this.swRaw = new float[numGates];
		this.spectralWidth = new float[numGates];
		this.snr = new float[numGates];
		this.noise  = new float[numGates];
		this.gateAgl = new float[numGates];
		this.corrVel = new float[numGates];
	}
	
	private void setNumSpectralBins(int num){
		this.numSpectralBins = num;
		spectralData = new float[numGates][numSpectralBins];
	}
	
	/*
	 * compute params for every gate
	 *   NOTE: proftools caluclates these for the entire file after reading data.  
	 *   I think the params we are interested in for the ASCII output fils only
	 *   need to be calulcated on a per profile basis.  Can expand later if we 
	 *   need to.
	 */
	public void computeParams(){
		
		//  Compute GateHeight, proftools.pro:1160
		//   * == profile Index,    index(*) == header 
		//   c_bm(*) == beamArrayIndex   par_ndx == header.paramSetIndex[]
		int dirIdx = header.directionIndex[beamArrayIndex];
		int parSetIdx = header.paramSetIndex[beamArrayIndex];
		
		//	beam_elev(*)=(b_elev(index(*), dir_ndx(index(*),c_bm(*)) ) )/100.
		double beamElev = header.beamElevationAngle[dirIdx]/100.0;

		//		mult_factor(*)=cos((90.-beam_elev(*))*!dtor)
		double multFac = Math.cos((90.0 - beamElev) * DTR);

		//			horiz_factor(*)=sin((90.-beam_elev(*))*!dtor)
		double horizFac = Math.sin((90.0 - beamElev)*DTR);
		
//	   	    first_gate(*)=mult_factor* (  ( delay( index(*),par_ndx(index(*),c_bm(*)) )  
//					                        -rx_delay( index(*),par_ndx(index(*),c_bm(*)) )  )* 
//				      		                1.e-9)*
//											3.e8/2000.;kilometers
		double delay = header.delayFirstPulse[parSetIdx];
		double rxDelay = header.rxDelay[parSetIdx];
		double firstGate = 1.0e-9 * (delay - rxDelay);
		firstGate = firstGate * multFac * 3.0e8 / 2000.0;
		
//			 gate_height(*)=mult_factor* $
//					(spacing(index(*),par_ndx(index(*),c_bm(*)))*1.e-9)*3.e8/2000.;kilometers
		double gateHeight = multFac * header.gateSpacingNanosecs[parSetIdx] * 1.0e-9 * 3.0e8 / 2000.0;
		
		double wavelength, prf;   
		//		prf(i)= ( 1. / ( ipp(index(i),par_ndx(index(i),c_bm(i)) ) *1.e-9)) $
		//                   / num_integ(index(i),par_ndx(index(i),c_bm(i)) )
		prf = (1.0 / (header.interPulsePeriodNanosecs[parSetIdx] * 1.0e-9)) / header.numCoherentIntegrations[parSetIdx];
		//	wavelength(i)=3.e8/( rad_tx( index(i) ) *1.e4 )
		wavelength = 3.0e8 / (header.txFreq*1.0e4);
		//	 nyquist(i)=(prf(i)*wavelength(i))/4.
		nyquist = (float)((prf * wavelength) / 4.0);
		//  NOTE:  i in the proftools equations is profile index, and the second dim is gate index
		for(int i=0; i< numGates; i++) {
			//    gate_agl(i,*)=first_gate(i)+gate_height(i)*levels(*)
			gateAgl[i] = (float)(firstGate + (gateHeight * (float)i));
			//
			//	 sw_velocity(i,*)=sw_val(i,*)*nyquist(i)
			spectralWidth[i] = (float)swRaw[i] * nyquist;
			//	 dop_velocity(i,*)=m_dop_val(i,*)*nyquist(i)
			//  NOTE added (-) to correct Vel direction; 1/11/2010 - TC
			dopplerVelocity[i] = -1.0f * (float)dopplerRaw[i] * nyquist;
			//  spec_corvel = -dop_velocity(plot_points(prof_ndx),gate_num)*
			//                 exp( 0.45 * (-gate_agl (plot_points(prof_ndx),gate_num) /9.58)  )
			//  NOTE removed (-) here because of above sign correction to dopplerVel; 1/11/2010 TC
			corrVel[i] = dopplerVelocity[i] * (float)(Math.exp( 0.45 * (-gateAgl[i]/9.58) ));
			//Math.exp(arg0);

			
		}
	}
	
	public int getNumGates() {
		return numGates;
	}
	
	public double getMaxHeight() {
		return gateAgl[numGates - 1];
	}
	
	public int getNumSpectralBins() {
		return numSpectralBins;
	}

	public void setHeader(Profiler915_Header header) {
		this.header = header;
	}

	public float [] getData(int momentNum) {
		switch(momentNum) {
		case 0:
			return dopplerVelocity;
		case 1:
			return spectralWidth;
		case 2:
			return snr;
		default:
			System.err.println("Profile915.getData():  Moment num not recognized: " + momentNum);
			return null;
		}
	}
	
	public float [] getHeights(){
		return gateAgl;
	}
	
	public float getHeight(int gateNum){
		return gateAgl[gateNum];
	}
	
	public double getTime() {
		return sysTime;
	}
	
	public int getGateNum(double height) {
		for(int i=1; i< gateAgl.length; i++) {
			if(height<= gateAgl[i])
				return i-1;
		}
		return gateAgl.length-1;
	}
	
	public float getDopplerVelocity(int gateNum) {
		return dopplerVelocity[gateNum];
	}

	public float getSpectralWidth(int gateNum) {
		return spectralWidth[gateNum];
	}

	public float getSnr(int gateNum) {
		return snr[gateNum];
	}

	public float [] getDopplerVelocity() {
		return dopplerVelocity;
	}

	public float [] getSpectralWidth() {
		return spectralWidth;
	}

	public float [] getSnr() {
		return snr;
	}
	
	public float [] getSpectralData(int gateNum) {
		return spectralData[gateNum];
	}

	public float getNyquist(){
		return nyquist;
	}
}

