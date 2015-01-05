package org.senshorhub.impl.sensor.profiler915;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Title: WindRecord.java</p>
 * <p>Description:  </p>
 * @author Tony Cook
 * @since Jun 29, 2010
 */

public class WindRecord
{
	protected String stationName;
	protected String revision;
	protected double lat, lon, alt;
	double time;
	int averagingTime;
	private int numBeams;
	int rangeGate;
	int [] consensus;
	int [] totalRecords;
	int [] windowSize;
	int [] numCodedCells = new int[2];
	int [] numSpectra = new int[2];
	int [] pulseWidth =  new int[2];
	int [] interPulsePd =  new int[2];
	double [] velocity = new double[2];
	int verticalCorrection;;
	int [] delay = new int[2];
	int [] gateDelay = new int[2];
	int [] numGates =  new int[2];
	int [] gateSpacing = new int[2];
	double [] azimuth;
	double [] elevation;
	List<WindData> windData;

	
	public WindRecord() {
		windData =new ArrayList<WindData>(100); 
	}
	
	public void setNumBeams(int num) {
		numBeams = num;
		consensus = new int[num];
		totalRecords = new int[num];
		windowSize = new int[num];
		azimuth = new double[num];
		elevation = new double[num];

	}
	
	public int getNumBeams(){
		return numBeams;	
	}
	
	public double getTime(){
		return time;
	}
	
	public List<WindData> getWindData(){
		return windData;
	}
	
	public double getMaxHeight(){
		double maxHt = 0.0;
		for(WindData wdTmp: windData) {
			if(wdTmp.height > maxHt)
				maxHt = wdTmp.height;
		}
		
		return maxHt;
	}
}
