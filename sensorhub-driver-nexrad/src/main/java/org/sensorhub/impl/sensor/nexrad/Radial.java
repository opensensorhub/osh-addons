package org.sensorhub.impl.sensor.nexrad;

public class Radial {
	protected int numBins;
	protected float radialStartTime;
	protected float elevation, azimuth;
	protected float distanceToFirstBin;  //incorporate this in ProcChain for Doppler geoloc
    protected short [] dataShort;   // data in file is unsigned byte, so we have to store as shorts in java
    protected float [] dataFloat;   // Level III is floats, at least for BR
}
