package org.senshorhub.impl.sensor.profiler915;


/**
 * <p>Title: Profile.java</p>
 * <p>Description:  </p>
 * @author Tony Cook
 * @since Nov 8, 2010
 */

abstract public class Profile
{
	abstract public int getNumGates();
	
	abstract public double getMaxHeight();
	
	abstract public float [] getData(int momentNum);

	abstract public float [] getHeights();
	
	abstract public float getHeight(int gateNum);
	
	abstract public double getTime();
	
	abstract public int getGateNum(double height);

}
