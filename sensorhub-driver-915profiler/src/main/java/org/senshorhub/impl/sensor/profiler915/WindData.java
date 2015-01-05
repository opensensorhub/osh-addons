package org.senshorhub.impl.sensor.profiler915;

/**
 * <p>Title: WindData.java</p>
 * <p>Description:  </p>
 * @author Tony Cook
 * @since Jun 29, 2010
 */

public class WindData
{
	double height;
	double speed;
	double direction;
	double [] rad;
	double [] cnt;  //
	double [] snr; //

	public WindData(int numBeams){
		rad = new double[numBeams];
		cnt = new double[numBeams];
		snr = new double[numBeams];
	}

	public double getHeight(){
		return height;
	}

	public double getSpeed() {
		return speed;
	}

	public double getDirection() {
		return direction;
	}
}
