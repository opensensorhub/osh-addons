package org.sensorhub.impl.sensor.nexrad;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * <p>Title: Sweep.java</p>
 * <p>Description:  One elevation sweep- may abstract out for Data typeStr if needed  </p>
 * @author Tony Cook
 * @since Jul 11, 2011
 * 
 *   TODO- separate into Sweep (for az,el) and rasterSweep 
 */

public class Sweep
{
	protected List<Radial> radials = new ArrayList<>();
	protected double binSpacing; // 1000 vs. 999
	protected int numBins;
	protected long startTimeMs;
	String utcTimeStr;  
	
//	protected LatLon location;
	@Deprecated //  use BboxXY
	protected Rectangle2D.Double boundsr2d;  // 
//	protected BboxXY bbox;
	
	public Sweep() {		// TODO Auto-generated constructor stub
	}
	
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
		return radials.get(0).numBins;
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
	
	protected Radial[] getAziumthSortedRadials() {
		TreeSet<Radial> sortedRadials = new TreeSet<Radial>(new Comparator<Radial>() {
			@Override
			public int compare(Radial r1, Radial r2) {
				return (r1.azimuth > r2.azimuth) ? 1 : -1;  // should never be equal
			}
		}); 
		
		sortedRadials.addAll(radials);
		
		return sortedRadials.toArray(new Radial [] {});
	}

}
