package org.sensorhub.impl.sensor.nexrad;

import java.util.ArrayList;
import java.util.List;

import org.sensorhub.impl.sensor.nexrad.Radial;



/**
 * <p>Title: Sweep.java</p>
 * <p>Description:  One elevation sweep- may abstract out for Data typeStr if needed  </p>
 * @author Tony Cook
 * @since Jul 11, 2011
 * 
 *   TODO- separate into Sweep (for az,el) and rasterSweep 
 */

@Deprecated // Use LdmRadial
public class Sweep
{
	protected List<Radial> radials = new ArrayList<>();
	protected double gateSpacing; // 1000 vs. 999
	protected double rangeToFirstGate;
	protected int numGates;
	protected long startTimeMs;
	String utcTimeStr;  
	
	public void addRadial(Radial radial) {
		radials.add(radial);
	}
	
	public long getStartTimeMs() {
		return startTimeMs;
	}

	public void setStartTimeMs(long startTimeMs) {
		this.startTimeMs = startTimeMs;
	}

	public int getNumRadials() {
		return radials.size();
	}
	
	// ASSumes all radials have same number of bins- ok  4 now
	public int getNumBins() {
		return radials.get(0).numGates;
	}

//	public BboxXY getBbox() {
//		return bbox;
//	}
//	
//	public void setBbox(BboxXY bb) {
//		this.bbox = bb;
//	}
//
//	public LatLon getLocation() {
//		return location;
//	}
//
//	public void setLocation(LatLon location) {
//		this.location = location;
//	}

	//  mutable
	public List<Radial> getRadials() {
		return radials;
	}
	
	public Radial [] getRadialArray() {
		return radials.toArray(new Radial[] {});
	}

	public double getRangeToFirstBin() {
		return rangeToFirstGate;
	}

	public void setRangeToFirstBin(double rangeToFirstBin) {
		this.rangeToFirstGate = rangeToFirstBin;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Sweep info:\n");
		sb.append("\tsweepStartTime: " + startTimeMs + "\n");
		sb.append("\tnumRadials: " + radials.size() + "\n");
		sb.append("\tnumBins: " + numGates + "\n");
		sb.append("\tbinSpacing: " + gateSpacing + "\n");
		sb.append("\trangeToFirstBin: " + rangeToFirstGate + "\n");
		
		return sb.toString();
	}

}
