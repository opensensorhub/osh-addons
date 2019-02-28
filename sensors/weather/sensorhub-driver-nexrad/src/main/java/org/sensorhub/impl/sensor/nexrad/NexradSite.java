package org.sensorhub.impl.sensor.nexrad;

/**
 * <p>Title: NexradSite.java</p>
 * <p>Description:  </p>
 * @author Tony Cook
 */

public class NexradSite
{
	public String id;
	public String name;
	public double lat; // meters
	public double lon; 
	public double elevation; 
	public int elevationFeet;
	//  TODO: round up these conversion factors and put in Util class
	public static final double FEET_TO_METERS =  0.3048;
	
	public NexradSite(String id) {
		this.id = id;
	}

	public String toString() {
		return id + " " + name + "  " + lat + "," + lon + "," + elevation + " meters";
	}
}
