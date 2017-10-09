package org.sensorhub.impl.sensor.fltaware;

public class TurbulenceRecord
{
	long time;  // constant for now but will be variable if we incorporate forecast turb
	String waypointName;
	double lat;
	double lon;
	float [] turbulence;
}
