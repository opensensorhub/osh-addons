package org.sensorhub.impl.sensor.turbulence;

public class TurbulenceRecord
{
	long time;  // constant for now but will be variable if we incorporate forecast turb
	double lat;
	double lon;
	float [] turbulence;
}
