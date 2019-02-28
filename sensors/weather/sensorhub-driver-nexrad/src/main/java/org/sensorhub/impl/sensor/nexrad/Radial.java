package org.sensorhub.impl.sensor.nexrad;

@Deprecated // Use LdmRadial
public class Radial {
	protected int numGates;
	protected long radialStartTime;  // Utc in MS
	protected float elevation, azimuth;
    protected short [] dataShort;   // if data is short/unsigned byte
    protected float [] dataFloat;   // if data is float
    protected short rangeToCenterOfFirstGate; // meters
	protected short rangeGateSize; // meters

    /**
	 * @return the rangeGateSize
	 */
	public final short getRangeGateSize() {
		return rangeGateSize;
	}
	/**
	 * @param rangeGateSize the rangeGateSize to set
	 */
	public final void setRangeGateSize(short rangeGateSize) {
		this.rangeGateSize = rangeGateSize;
	}
	public final short getRangeToCenterOfFirstGate() {
		return rangeToCenterOfFirstGate;
	}
	public final void setRangeToCenterOfFirstGate(short rangeToCenterOfFirstGate) {
		this.rangeToCenterOfFirstGate = rangeToCenterOfFirstGate;
	}
    
	public int getNumGates() {
		return numGates;
	}
	public void setNumGates(int numGates) {
		this.numGates = numGates;
	}
	public long getRadialStartTime() {
		return radialStartTime;
	}
	public void setRadialStartTime(long radialStartTime) {
		this.radialStartTime = radialStartTime;
	}
	public float getElevation() {
		return elevation;
	}
	public void setElevation(float elevation) {
		this.elevation = elevation;
	}
	public float getAzimuth() {
		return azimuth;
	}
	public void setAzimuth(float azimuth) {
		this.azimuth = azimuth;
	}
	public float[] getDataFloat() {
		return dataFloat;
	}
	public void setDataFloat(float[] dataFloat) {
		this.dataFloat = dataFloat;
	}
	public short[] getDataShort() {
		return dataShort;
	}
	public void setDataShort(short[] dataShort) {
		this.dataShort = dataShort;
	}
}
