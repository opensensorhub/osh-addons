package org.sensorhub.impl.sensor.flightAware;

public class TurbulenceRecord
{
	public long time;  // constant for now but will be variable if we incorporate forecast turb
	public String waypointName = "";  
	public double lat;
	public double lon;
	public float [] turbulence;
}
